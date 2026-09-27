/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.jdbc;

import com.kubling.net.KublingURL;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 4.3
 */
public class JDBCURL {
    private static final String UTF_8 = "UTF-8";
    public static final String JDBC_PROTOCOL = "jdbc:kubling:";

    static final String URL_PATTERN = JDBC_PROTOCOL + "([^@^;]+)(?:@([^;]*))?(;.*)?";
    static Pattern urlPattern = Pattern.compile(URL_PATTERN);

    public static final Map<String, String> EXECUTION_PROPERTIES = Collections.unmodifiableMap(buildProps());

    private static Map<String, String> buildProps() {
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> keys = extractFieldNames(ExecutionProperties.class);
        for (String key : keys) {
            result.put(key, key);
        }
        result.put(LocalProfile.USE_CALLING_THREAD, LocalProfile.USE_CALLING_THREAD);
        return result;
    }

    private static Set<String> extractFieldNames(Class<?> clazz) throws AssertionError {
        HashSet<String> result = new HashSet<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == String.class) {
                try {
                    if (!result.add((String) field.get(null))) {
                        throw new AssertionError("Duplicate value for " + field.getName());
                    }
                } catch (Exception e) {
                    // Ignored
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static final Map<String, String> KNOWN_PROPERTIES = getKnownProperties();

    private static Map<String, String> getKnownProperties() {
        Set<String> props = new HashSet<>(Arrays.asList(
                BaseDataSource.APP_NAME,
                BaseDataSource.VDB_NAME,
                BaseDataSource.VERSION,
                BaseDataSource.VDB_VERSION,
                BaseDataSource.USER_NAME,
                BaseDataSource.PASSWORD,
                LocalProfile.WAIT_FOR_LOAD,
                KublingURL.CONNECTION.AUTO_FAILOVER,
                KublingURL.CONNECTION.PASSTHROUGH_AUTHENTICATION,
                KublingURL.CONNECTION.ENCRYPT_REQUESTS,
                KublingURL.CONNECTION.LOGIN_TIMEOUT,
                DatabaseMetaDataImpl.REPORT_AS_VIEWS,
                DatabaseMetaDataImpl.NULL_SORT,
                ResultSetImpl.DISABLE_FETCH_SIZE));
        props.addAll(EXECUTION_PROPERTIES.keySet());
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String string : props) {
            result.put(string, string);
        }
        return Collections.unmodifiableMap(result);
    }

    private String vdbName;
    private String connectionURL;
    private final Properties properties = new Properties();

    public enum ConnectionType {
        Embedded,
        Socket
    }

    public static ConnectionType acceptsUrl(String url) {
        Matcher m = urlPattern.matcher(url);
        if (m.matches()) {
            return m.group(2) != null ? ConnectionType.Socket : ConnectionType.Embedded;
        }
        return null;
    }

    private String urlString;

    public JDBCURL(String jdbcURL) {
        parseURL(jdbcURL);
    }

    public JDBCURL(String vdbName, String connectionURL, Properties props) {
        if (vdbName == null || vdbName.trim().isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.vdbName = vdbName;
        this.connectionURL = connectionURL;
        if (props != null) {
            normalizeProperties(props, this.properties);
        }
    }

    public String getVDBName() {
        return vdbName;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public Properties getProperties() {
        // Make a copy of the properties object, including any non-string values that may be contained in the map.
        Properties newProps = new Properties();
        newProps.putAll(this.properties);
        return newProps;
    }

    private void parseURL(String jdbcURL) {
        if (jdbcURL == null) {
            throw new IllegalArgumentException();
        }
        // Trim extra spaces
        jdbcURL = jdbcURL.trim();
        if (jdbcURL.isEmpty()) {
            throw new IllegalArgumentException();
        }

        Matcher m = urlPattern.matcher(jdbcURL);
        if (!m.matches()) {
            throw new IllegalArgumentException();
        }
        vdbName = getValidValue(m.group(1));
        connectionURL = m.group(2);
        if (connectionURL != null) {
            connectionURL = getValidValue(connectionURL.trim());
        }
        String props = m.group(3);
        if (props != null) {
            parseConnectionProperties(props, this.properties);
        }
    }

    public static void parseConnectionProperties(String connectionInfo, Properties p) {
        String[] connectionParts = connectionInfo.split(";");
        // The rest should be connection params
        for (String connectionPart : connectionParts) {
            parseConnectionProperty(connectionPart, p);
        }
    }

    static void parseConnectionProperty(String connectionProperty, Properties p) {
        if (connectionProperty.isEmpty()) {
            // Be tolerant of double-semicolons and dangling semicolons
            return;
        } else if (connectionProperty.length() < 3) {
            // key=value must have at least 3 characters
            throw new IllegalArgumentException();
        }
        int firstEquals = connectionProperty.indexOf('=');
        if (firstEquals < 1) {
            throw new IllegalArgumentException();
        }
        String key = connectionProperty.substring(0, firstEquals).trim();
        String value = connectionProperty.substring(firstEquals + 1).trim();
        if (value.indexOf('=') >= 0) {
            throw new IllegalArgumentException();
        }
        addNormalizedProperty(getValidValue(key), getValidValue(value), p);
    }

    public String getJDBCURL() {
        if (urlString == null) {
            StringBuilder buf = new StringBuilder(JDBC_PROTOCOL)
                    .append(safeEncode(vdbName));
            if (this.connectionURL != null) {
                buf.append('@').append(connectionURL);
            }
            TreeMap sorted = new TreeMap(properties);

            for (Object o : sorted.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                if (entry.getValue() instanceof String) {
                    // get only the string properties, because a non-string property could not have been set on the url.
                    buf.append(';')
                            .append(entry.getKey())
                            .append('=')
                            .append(safeEncode((String) entry.getValue()));
                }
            }
            urlString = buf.toString();
        }
        return urlString;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getUserName() {
        return properties.getProperty(BaseDataSource.USER_NAME);
    }

    public String getPassword() {
        return properties.getProperty(BaseDataSource.PASSWORD);
    }

    public String getVDBVersion() {
        if (properties.contains(BaseDataSource.VDB_VERSION)) {
            return properties.getProperty(BaseDataSource.VDB_VERSION);
        }
        return properties.getProperty(BaseDataSource.VERSION);
    }

    public String getTransactionAutowrapMode() {
        return properties.getProperty(ExecutionProperties.PROP_TXN_AUTO_WRAP);
    }

    public String getPartialResultsMode() {
        return properties.getProperty(ExecutionProperties.PROP_PARTIAL_RESULTS_MODE);
    }

    public String getResultSetCacheMode() {
        return properties.getProperty(ExecutionProperties.RESULT_SET_CACHE_MODE);
    }

    public String getAnsiQuotedIdentifiers() {
        return properties.getProperty(ExecutionProperties.ANSI_QUOTED_IDENTIFIERS);
    }

    public String getFetchSize() {
        return properties.getProperty(ExecutionProperties.PROP_FETCH_SIZE);
    }

    public String getTransparentFailover() {
        return properties.getProperty(KublingURL.CONNECTION.AUTO_FAILOVER);
    }

    public String getDisableLocalTransactions() {
        return properties.getProperty(ExecutionProperties.DISABLE_LOCAL_TRANSACTIONS);
    }

    public String toString() {
        return getJDBCURL();
    }

    private static void normalizeProperties(Properties source, Properties target) {
        for (Enumeration e = source.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            addNormalizedProperty(key, source.get(key), target);
        }
    }

    public static void addNormalizedProperty(String key, Object value, Properties target) {
        String validKey = getValidKey(key);

        // now add the normalized key and value into the properties object.
        target.put(validKey, value);
    }

    public static String getValidKey(String key) {
        String result = KNOWN_PROPERTIES.get(key);
        if (result != null) {
            return result;
        }
        return key;
    }

    private static String getValidValue(String value) {
        // Decode the value of the property if incase they were encoded.
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String safeEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static Properties normalizeProperties(Properties props) {
        normalizeProperties(props, props);
        return props;
    }

}
