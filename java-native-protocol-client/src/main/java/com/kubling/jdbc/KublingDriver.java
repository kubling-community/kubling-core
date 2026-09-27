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

import com.kubling.core.KublingException;
import com.kubling.core.util.ApplicationInfo;
import com.kubling.core.util.PropertiesUtils;
import com.kubling.core.util.ReflectionHelper;
import com.kubling.net.KublingURL;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * JDBC Driver class for Kubling Embedded and Kubling Server. This class automatically registers with the
 * {@link DriverManager}
 * <p>
 * The accepted URL format for the connection
 * <ul>
 *     <li> Server/socket connection:<b> jdbc:kubling:&lt;vdb-name&gt;@mm[s]://&lt;server-name&gt;:&lt;port&gt;;[user=&lt;user-name&gt;][password=&lt;user-password&gt;][other-properties]*</b>
 *     <li> Embedded  connection:<b> jdbc:kubling:&lt;vdb-name&gt;@&lt;file-path-to-deploy.properties&gt;;[user=&lt;user-name&gt;][password=&lt;user-password&gt;][other-properties]*</b>
 * </ul>
 * The user, password properties are needed if the user authentication is turned on. All the "other-properties" are simple name value pairs.
 * Look at {@link JDBCURL} KNOWN_PROPERTIES for list of known properties allowed.
 */

public class KublingDriver implements Driver {

    static Logger logger = Logger.getLogger("com.kubling.jdbc");
    static final String DRIVER_NAME = "Kubling DBVirt JDBC Driver";

    private static final KublingDriver INSTANCE = new KublingDriver();

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            // Logging
            String logMsg = JDBCPlugin.Util.getString("MMDriver.Err_registering", e.getMessage());
            logger.log(Level.SEVERE, logMsg);
        }
    }

    private ConnectionProfile socketProfile = new SocketProfile();
    private ConnectionProfile localProfile;

    public static KublingDriver getInstance() {
        return INSTANCE;
    }

    public ConnectionImpl connect(String url, Properties info) throws SQLException {
        JDBCURL.ConnectionType conn = JDBCURL.acceptsUrl(url);
        if (conn == null) {
            return null;
        }
        if (info == null) {
            // create a properties obj if it is null
            info = new Properties();
        } else {
            //don't modify the original
            info = PropertiesUtils.clone(info);
        }
        parseURL(url, info);

        ConnectionImpl myConnection;

        ConnectionProfile cp = socketProfile;
        if (conn == JDBCURL.ConnectionType.Embedded) {
            if (localProfile == null) {
                try {
                    localProfile = (ConnectionProfile) ReflectionHelper.create(
                            "com.kubling.jdbc.jboss.ModuleLocalProfile", null,
                            getClass().getClassLoader());
                } catch (KublingException e) {
                    throw KublingSQLException.create(e, JDBCPlugin.Util.gs("module_load_failed"));
                }
            }
            cp = localProfile;
        }
        try {
            myConnection = cp.connect(url, info);
        } catch (KublingSQLException e) {
            logger.log(Level.FINE, "Could not create connection", e);
            throw KublingSQLException.create(e, e.getMessage());
        }

        // logging
        String logMsg = JDBCPlugin.Util.getString("JDBCDriver.Connection_sucess");
        logger.fine(logMsg);

        return myConnection;
    }

    public void setLocalProfile(ConnectionProfile embeddedProfile) {
        this.localProfile = embeddedProfile;
    }

    public void setSocketProfile(ConnectionProfile socketProfile) {
        this.socketProfile = socketProfile;
    }

    /**
     * Returns true if the driver thinks that it can open a connection to the given URL.
     * Expected URL format for server mode is
     * jdbc:kubling::VDB{@literal @}mm://server:port;version=1;user=username;password=password
     *
     * @param url used to establish a connection.
     * @return A boolean value indicating whether the driver understands the subprotocol.
     */
    public boolean acceptsURL(String url) {
        return JDBCURL.acceptsUrl(url) != null;
    }

    public int getMajorVersion() {
        return ApplicationInfo.getInstance().getMajorReleaseVersion();
    }

    public int getMinorVersion() {
        return ApplicationInfo.getInstance().getMinorReleaseVersion();
    }

    public String getDriverName() {
        return DRIVER_NAME;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (info == null) {
            info = new Properties();
        } else {
            info = PropertiesUtils.clone(info);
        }

        // construct list of driverPropertyInfo objects
        List<DriverPropertyInfo> driverProps = new LinkedList<>();

        parseURL(url, info);

        for (String property : JDBCURL.KNOWN_PROPERTIES.keySet()) {
            DriverPropertyInfo dpi = new DriverPropertyInfo(property, info.getProperty(property));
            if (property.equals(BaseDataSource.VDB_NAME)) {
                dpi.required = true;
            }
            dpi.description = JDBCPlugin.Util.getString(property + "_desc");
            if (JDBCPlugin.Util.keyExists(property + "_choices")) {
                dpi.choices = JDBCPlugin.Util.getString(property + "_choices").split(",");
            }
            driverProps.add(dpi);
        }

        // create an array of DriverPropertyInfo objects
        DriverPropertyInfo[] propInfo = new DriverPropertyInfo[driverProps.size()];

        // copy the elements from the list to the array
        return driverProps.toArray(propInfo);
    }

    /**
     * This method parses the URL and adds properties to the properties object.
     * These include required and any optional properties specified in the URL.
     *
     * @param url  The URL needed to be parsed.
     * @param info The properties object which is to be updated with properties in the URL.
     * @throws SQLException if the URL is not in the expected format.
     */
    protected static void parseURL(String url, Properties info) throws SQLException {
        if (url == null) {
            String msg = JDBCPlugin.Util.getString("MMDriver.urlFormat");
            throw new KublingSQLException(msg);
        }
        try {
            JDBCURL jdbcURL = new JDBCURL(url);
            info.setProperty(BaseDataSource.VDB_NAME, jdbcURL.getVDBName());
            if (jdbcURL.getConnectionURL() != null) {
                info.setProperty(KublingURL.CONNECTION.SERVER_URL, jdbcURL.getConnectionURL());
            }
            Properties optionalParams = jdbcURL.getProperties();
            JDBCURL.normalizeProperties(info);
            Enumeration<?> keys = optionalParams.keys();
            while (keys.hasMoreElements()) {
                String propName = (String) keys.nextElement();
                // Don't let the URL properties override the passed-in Properties object.
                if (!info.containsKey(propName)) {
                    info.setProperty(propName, optionalParams.getProperty(propName));
                }
            }
            // add the property only if it is new because they could have
            // already been specified either through url or otherwise.
            if (!info.containsKey(BaseDataSource.VDB_VERSION) && jdbcURL.getVDBVersion() != null) {
                info.setProperty(BaseDataSource.VDB_VERSION, jdbcURL.getVDBVersion());
            }
            if (!info.containsKey(BaseDataSource.APP_NAME)) {
                info.setProperty(BaseDataSource.APP_NAME, BaseDataSource.DEFAULT_APP_NAME);
            }

        } catch (IllegalArgumentException iae) {
            throw new KublingSQLException(JDBCPlugin.Util.getString("MMDriver.urlFormat"));
        }
    }

    /**
     * This method returns true if the driver passes jdbc compliance tests.
     *
     * @return true if the driver is jdbc complaint, else false.
     */
    public boolean jdbcCompliant() {
        return false;
    }

    public Logger getParentLogger() {
        return logger;
    }
}


