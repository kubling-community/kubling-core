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

package com.kubling.net;

import com.kubling.core.util.StringUtil;
import com.kubling.jdbc.JDBCPlugin;

import java.net.MalformedURLException;
import java.util.*;


/**
 * Class defines the URL in the Kubling.
 *
 * @since 4.2
 */
public class KublingURL {

    public interface JDBC {
        // constant indicating Virtual database name
        String VDB_NAME = "VirtualDatabaseName";
        // constant indicating Virtual database version
        String VDB_VERSION = "VirtualDatabaseVersion";
        // constant for vdb version part of serverURL
        String VERSION = "version";
    }

    public interface CONNECTION {
        String CLIENT_IP_ADDRESS = "clientIpAddress";
        String CLIENT_HOSTNAME = "clientHostName";
        String CLIENT_MAC = "clientMAC";
        /**
         * If true, will automatically select a new server instance after a communication exception.
         *
         * @since 5.6
         */
        String AUTO_FAILOVER = "autoFailover";

        String SERVER_URL = "serverURL";
        /**
         * Non-secure Protocol.
         */
        String NON_SECURE_PROTOCOL = "mm";
        /**
         * Secure Protocol.
         */
        String SECURE_PROTOCOL = "mms";
        // name of the application which is obtaining connection
        String APP_NAME = "ApplicationName";
        // constant for username part of url
        String USER_NAME = "user";
        // constant for password part of url
        String PASSWORD = "password";

        String PASSTHROUGH_AUTHENTICATION = "PassthroughAuthentication";

        String ENCRYPT_REQUESTS = "encryptRequests";
        String LOGIN_TIMEOUT = "loginTimeout";

    }

    public static final String DOT_DELIMITER = ".";
    public static final String DOUBLE_SLASH_DELIMITER = "//";
    public static final String COMMA_DELIMITER = ",";

    public static final String COLON_DELIMITER = ":";
    public static final String BACKSLASH_DELIMITER = "\\";
    public static final String DEFAULT_PROTOCOL = CONNECTION.NON_SECURE_PROTOCOL + "://";
    public static final String SECURE_PROTOCOL = CONNECTION.SECURE_PROTOCOL + "://";

    public static final String INVALID_FORMAT_SERVER = JDBCPlugin.Util.getString("MMURL.INVALID_FORMAT");
    /*
     * appserver URL
     */
    private String appServerURL;
    /*
     * List of <code> HostData </code> in a cluster
     */
    private final List<HostInfo> hosts = new ArrayList<>();

    private boolean usingSSL = false;

    /**
     * Create an MMURL from the server URL.  For use by the server-side.
     *
     * @param serverURL Expected format: mm[s]://server1:port1[,server2:port2]
     * @since 4.2
     */
    public KublingURL(String serverURL) throws MalformedURLException {
        if (serverURL == null) {
            throw new MalformedURLException(INVALID_FORMAT_SERVER);
        }
        if (StringUtil.startsWithIgnoreCase(serverURL, SECURE_PROTOCOL)) {
            usingSSL = true;
        } else if (!StringUtil.startsWithIgnoreCase(serverURL, DEFAULT_PROTOCOL)) {
            throw new MalformedURLException(INVALID_FORMAT_SERVER);
        }

        appServerURL = serverURL;
        parseServerURL(serverURL.substring(usingSSL ? SECURE_PROTOCOL.length() :
                DEFAULT_PROTOCOL.length()), INVALID_FORMAT_SERVER);
    }

    public KublingURL(String host, int port, boolean secure) {
        usingSSL = secure;
        if (host.startsWith("[")) {
            host = host.substring(1, host.indexOf(']'));
        }
        hosts.add(new HostInfo(host, port));
    }

    /**
     * Validates that a server URL is in the correct format.
     *
     * @param serverURL Expected format: mm[s]://server1:port1[,server2:port2]
     * @since 4.2
     */
    public static boolean isValidServerURL(String serverURL) {
        boolean valid = true;
        try {
            new KublingURL(serverURL);
        } catch (Exception e) {
            valid = false;
        }

        return valid;
    }

    public List<HostInfo> getHostInfo() {
        return hosts;
    }

    /**
     * Get a list of hosts
     *
     * @return string of host separated by commas
     * @since 4.2
     */
    public String getHosts() {
        StringBuilder hostList = new StringBuilder();
        if (hosts != null) {
            Iterator<HostInfo> iterator = hosts.iterator();
            while (iterator.hasNext()) {
                HostInfo element = iterator.next();
                hostList.append(element.getHostName());
                if (iterator.hasNext()) {
                    hostList.append(COMMA_DELIMITER);
                }
            }
        }
        return hostList.toString();
    }

    /**
     * Get a list of ports
     *
     * @return string of ports separated by commas
     * @since 4.2
     */
    public String getPorts() {
        StringBuilder portList = new StringBuilder();
        if (Objects.nonNull(hosts)) {
            Iterator<HostInfo> iterator = hosts.iterator();
            while (iterator.hasNext()) {
                HostInfo element = iterator.next();
                portList.append(element.getPortNumber());
                if (iterator.hasNext()) {
                    portList.append(COMMA_DELIMITER);
                }
            }
        }
        return portList.toString();
    }

    /**
     * @since 4.2
     */
    private void parseServerURL(String serverURL, String exceptionMessage) throws MalformedURLException {
        StringTokenizer st = new StringTokenizer(serverURL, COMMA_DELIMITER);
        if (!st.hasMoreTokens()) {
            throw new MalformedURLException(exceptionMessage);
        }
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            nextToken = nextToken.trim();
            String host;
            String port;
            if (nextToken.startsWith("[")) {
                int hostEnd = nextToken.indexOf("]:");
                if (hostEnd == -1) {
                    throw new MalformedURLException
                            (JDBCPlugin.Util.getString("KublingURL.invalid_ipv6_hostport", nextToken, exceptionMessage));
                }
                host = nextToken.substring(1, hostEnd);
                port = nextToken.substring(hostEnd + 2);
            } else {
                int hostEnd = nextToken.indexOf(":");
                if (hostEnd == -1) {
                    throw new MalformedURLException(JDBCPlugin.Util.getString("KublingURL.invalid_hostport", nextToken, exceptionMessage));
                }
                host = nextToken.substring(0, hostEnd);
                port = nextToken.substring(hostEnd + 1);
            }
            host = host.trim();
            port = port.trim();
            if (host.isEmpty() || port.isEmpty()) {
                throw new MalformedURLException(JDBCPlugin.Util.getString("KublingURL.invalid_hostport", nextToken, exceptionMessage));
            }
            int portNumber = validatePort(port);
            HostInfo hostInfo = new HostInfo(host, portNumber);
            hosts.add(hostInfo);
        }
    }

    public static int validatePort(String port) throws MalformedURLException {
        int portNumber;
        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException nfe) {
            throw new MalformedURLException(JDBCPlugin.Util.getString("KublingURL.non_numeric_port", port));
        }
        String msg = validatePort(portNumber);
        if (msg != null) {
            throw new MalformedURLException(msg);
        }
        return portNumber;
    }

    public static String validatePort(int portNumber) {
        if (portNumber < 0 || portNumber > 0xFFFF) {
            return JDBCPlugin.Util.getString("KublingURL.port_out_of_range", portNumber);
        }
        return null;
    }

    /**
     * Get the Application Server URL
     *
     * @return String for connection to the Server
     * @since 4.2
     */
    public String getAppServerURL() {
        if (appServerURL == null) {
            StringBuilder sb = new StringBuilder();
            if (usingSSL) {
                sb.append(SECURE_PROTOCOL);
            } else {
                sb.append(DEFAULT_PROTOCOL);
            }
            Iterator<HostInfo> iter = hosts.iterator();
            while (iter.hasNext()) {
                HostInfo host = iter.next();

                boolean ipv6HostName = host.getHostName().indexOf(':') != -1;
                if (ipv6HostName) {
                    sb.append('[');
                }
                sb.append(host.getHostName());
                if (ipv6HostName) {
                    sb.append(']');
                }
                sb.append(COLON_DELIMITER);
                sb.append(host.getPortNumber());
                if (iter.hasNext()) {
                    sb.append(COMMA_DELIMITER);
                }
            }
            appServerURL = sb.toString();
        }
        return appServerURL;
    }

    /**
     * @see Object#toString()
     * @since 4.2
     */
    public String toString() {
        return getAppServerURL();
    }

    /**
     * @see Object#equals(Object)
     * @since 4.2
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof KublingURL url)) {
            return false;
        }
        return (appServerURL.equals(url.getAppServerURL()));
    }

    /**
     * @see Object#hashCode()
     * @since 4.2
     */
    public int hashCode() {
        return appServerURL.hashCode();
    }

    public boolean isUsingSSL() {
        return usingSSL;
    }

}
