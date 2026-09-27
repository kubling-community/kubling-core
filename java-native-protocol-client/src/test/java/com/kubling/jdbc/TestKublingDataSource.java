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

import com.kubling.client.RequestMessage;
import com.kubling.core.util.UnitTestUtil;
import com.kubling.net.KublingURL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestKublingDataSource {

    protected static final boolean VALID = true;
    protected static final boolean INVALID = false;

    private KublingDataSource dataSource;

    protected static final String STD_SERVER_NAME = "unitTestServerName";
    protected static final String STD_DATABASE_NAME = "unitTestVdbName";
    protected static final String STD_DATABASE_VERSION = "unitTestVdbVersion";
    protected static final String STD_DATA_SOURCE_NAME = "unitTestDataSourceName";
    protected static final int STD_PORT_NUMBER = 7001;
    protected static final String STD_LOG_FILE = UnitTestUtil.getTestScratchPath() + "/unitTestLogFile";
    protected static final int STD_LOG_LEVEL = 2;
    protected static final String STD_TXN_WRAP = KublingDataSource.TXN_WRAP_AUTO;
    protected static final String STD_PARTIAL_MODE = "false";
    protected static final String STD_CONFIG_FILE = UnitTestUtil.getTestDataPath() + "/bqt/bqt.properties";
    protected static final String STD_ALTERNATE_SERVERS = "unitTestServerName2:7001,unitTestServerName2:7002,unitTestServerName3:7001";


    @BeforeEach
    protected void setUp() {
        dataSource = new KublingDataSource();
        dataSource.setServerName(STD_SERVER_NAME);
        dataSource.setDatabaseVersion(STD_DATABASE_VERSION);
        dataSource.setDatabaseName(STD_DATABASE_NAME);
        dataSource.setPortNumber(STD_PORT_NUMBER);
        dataSource.setDataSourceName(STD_DATA_SOURCE_NAME);
        dataSource.setAutoCommitTxn(STD_TXN_WRAP);
        dataSource.setPartialResultsMode(STD_PARTIAL_MODE);
        dataSource.setSecure(true);
        dataSource.setAlternateServers(STD_ALTERNATE_SERVERS);
        dataSource.setUseJDBC4ColumnNameAndLabelSemantics(true);
    }

    // =========================================================================
    //                      H E L P E R   M E T H O D S
    // =========================================================================

    protected String getReasonWhyInvalid(final String propertyName, final String value) {
        switch (propertyName) {
            case "DatabaseName" -> {
                return KublingDataSource.reasonWhyInvalidDatabaseName(value);
            }
            case "DatabaseVersion" -> {
                return KublingDataSource.reasonWhyInvalidDatabaseVersion(value);
            }
            case "DataSourceName" -> {
                return KublingDataSource.reasonWhyInvalidDataSourceName(value);
            }
            case "Description" -> {
                return KublingDataSource.reasonWhyInvalidDescription(value);
            }
            case "ServerName" -> {
                return KublingDataSource.reasonWhyInvalidServerName(value);
            }
            case "TransactionAutoWrap" -> {
                return KublingDataSource.reasonWhyInvalidTransactionAutoWrap(value);
            }
            case "partialResultsMode" -> {
                return KublingDataSource.reasonWhyInvalidPartialResultsMode(value);
            }
            case "socketsPerVM" -> {
                return KublingDataSource.reasonWhyInvalidSocketsPerVM(value);
            }
            case "stickyConnections" -> {
                return KublingDataSource.reasonWhyInvalidStickyConnections(value);
            }
        }

        fail("Unknown property name \"" + propertyName + "\"");
        return null;
    }

    protected String getReasonWhyInvalid(final String propertyName, final int value) {
        if (propertyName.equals("PortNumber")) {
            return KublingDataSource.reasonWhyInvalidPortNumber(value);
        }
        fail("Unknown property name \"" + propertyName + "\"");
        return null;
    }

    public void helpTestReasonWhyInvalid(
            final String propertyName,
            final String value,
            final boolean shouldBeValid) {

        final String reason = getReasonWhyInvalid(propertyName, value);
        if (shouldBeValid) {
            assertNull(reason, "Unexpectedly considered invalid value \"" + value + "\"; reason = " + reason);
        }
    }

    public void helpTestReasonWhyInvalid(
            final String propertyName,
            final int value,
            final boolean shouldBeValid) {
        final String reason = getReasonWhyInvalid(propertyName, value);
        if (shouldBeValid) {
            assertNull(reason, "Unexpectedly considered invalid value " + value + "; reason = " + reason);
        }
    }

    public void helpTestBuildingURL(
            final String vdbName,
            final String vdbVersion,
            final String serverName,
            final int portNumber,
            final String alternateServers,
            final String txnAutoWrap,
            final String partialMode,
            final int fetchSize,
            final boolean showPlan,
            final boolean secure,
            final String expectedURL) {


        helpTestBuildingURL2(vdbName, vdbVersion, serverName, portNumber, alternateServers,
                txnAutoWrap, partialMode, fetchSize, showPlan, secure, true, expectedURL);
    }

    public void helpTestBuildingURL2(final String vdbName, final String vdbVersion,
                                     final String serverName, final int portNumber,
                                     final String alternateServers,
                                     final String txnAutoWrap, final String partialMode,
                                     final int fetchSize, final boolean showPlan,
                                     final boolean secure, final boolean useJDBC4Semantics,
                                     final String expectedURL) {

        final KublingDataSource ds = new KublingDataSource();
        ds.setServerName(serverName);
        ds.setDatabaseVersion(vdbVersion);
        ds.setDatabaseName(vdbName);
        ds.setPortNumber(portNumber);
        ds.setFetchSize(fetchSize);
        ds.setAutoCommitTxn(txnAutoWrap);
        ds.setPartialResultsMode(partialMode);
        if (showPlan) {
            ds.setShowPlan(RequestMessage.ShowPlan.ON.toString());
        }
        ds.setSecure(secure);
        ds.setAlternateServers(alternateServers);
        ds.setUseJDBC4ColumnNameAndLabelSemantics(useJDBC4Semantics);

        String url;
        try {
            url = ds.buildURL().getJDBCURL();
        } catch (KublingSQLException e) {
            throw new RuntimeException(e);
        }
        compareUrls(expectedURL, url);
    }

    /**
     * Compare urls without regard to property ordering
     */
    private void compareUrls(final String expectedURL, String url) {
        String[] parts = url.split(";", 2);
        String[] expectedParts = expectedURL.split(";", 2);

        assertEquals(parts[0], expectedParts[0]);
        assertEquals(new HashSet<>(Arrays.asList(parts[1].split(";"))),
                new HashSet<>(Arrays.asList(expectedParts[1].split(";"))));
    }

    public void helpTestConnection(
            final String vdbName,
            final String vdbVersion,
            final String serverName,
            final int portNumber, final String alternateServers,
            final String user,
            final String password,
            final String dataSourceName,
            final String txnAutoWrap,
            final String partialMode)
            throws SQLException {

        KublingDataSource ds = new KublingDataSource();

        ds.setServerName(serverName);
        ds.setDatabaseVersion(vdbVersion);
        ds.setDatabaseName(vdbName);
        ds.setPortNumber(portNumber);
        ds.setUser(user);
        ds.setPassword(password);
        ds.setDataSourceName(dataSourceName);
        ds.setAutoCommitTxn(txnAutoWrap);
        ds.setPartialResultsMode(partialMode);
        ds.setAlternateServers(alternateServers);
        ds.setUseJDBC4ColumnNameAndLabelSemantics(true);

        ds.getConnection();

    }

    // =========================================================================
    //                         T E S T     C A S E S
    // =========================================================================

    // ----------------------------------------------------------------
    //                       Test Getters
    // ----------------------------------------------------------------

    @Test
    public void testGetServerName() {
        final String result = dataSource.getServerName();
        assertEquals(STD_SERVER_NAME, result);
    }

    @Test
    public void testGetDatabaseVersion() {
        final String result = dataSource.getDatabaseVersion();
        assertEquals(STD_DATABASE_VERSION, result);
    }

    @Test
    public void testGetDatabaseName() {
        final String result = dataSource.getDatabaseName();
        assertEquals(STD_DATABASE_NAME, result);
    }

    @Test
    public void testGetDefaultApplicationName() {
        final String result = dataSource.getApplicationName();
        assertEquals(BaseDataSource.DEFAULT_APP_NAME, result);
    }

    @Test
    public void testGetApplicationName() {
        dataSource.setApplicationName("ClientApp");
        final String result = dataSource.getApplicationName();
        assertEquals("ClientApp", result);
    }

    @Test
    public void testGetPortNumber() {
        final int result = dataSource.getPortNumber();
        assertEquals(STD_PORT_NUMBER, result);
    }

    @Test
    public void testGetDataSourceName() {
        final String result = dataSource.getDataSourceName();
        assertEquals(STD_DATA_SOURCE_NAME, result);
    }

    @Test
    public void testGetLoginTimeout() {
        try {
            final int actual = 1000;
            dataSource.setLoginTimeout(actual);
            final int result = dataSource.getLoginTimeout();
            assertEquals(actual, result);
        } catch (SQLException e) {
            fail("Error obtaining login timeout");
        }
    }

    @Test
    public void testGetLogWriter() {
        try {
            final PrintWriter actual = new PrintWriter(new ByteArrayOutputStream());
            dataSource.setLogWriter(actual);
            final PrintWriter result = dataSource.getLogWriter();
            assertEquals(result, actual);
        } catch (SQLException e) {
            fail("Error obtaining login timeout");
        }
    }

    @Test
    public void testGetTransactionAutoWrap() {
        final String result = dataSource.getAutoCommitTxn();
        Properties p = dataSource.buildProperties("foo", "bar");
        assertEquals(STD_TXN_WRAP, p.getProperty(ExecutionProperties.PROP_TXN_AUTO_WRAP));
        assertEquals(STD_TXN_WRAP, result);
    }

    @Test
    public void testGetSecure() {
        assertTrue(dataSource.isSecure());
        dataSource.setSecure(false);
        assertFalse(dataSource.isSecure());
    }

    @Test
    public void testGetAlternateServers() {
        String result = dataSource.getAlternateServers();
        assertEquals(STD_ALTERNATE_SERVERS, result);
        dataSource.setAlternateServers(null);
        result = dataSource.getAlternateServers();
        assertNull(result);
        dataSource.setAlternateServers(STD_ALTERNATE_SERVERS);
        result = dataSource.getAlternateServers();
        assertEquals(STD_ALTERNATE_SERVERS, result);
    }

    // ----------------------------------------------------------------
    //                       Test invalid reasons
    // ----------------------------------------------------------------

    @Test
    public void testReasonWhyInvalidDatabaseName1() {
        helpTestReasonWhyInvalid("DatabaseName", "Valid VDB Name", VALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseName2() {
        helpTestReasonWhyInvalid("DatabaseName", "", INVALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseName3() {
        helpTestReasonWhyInvalid("DatabaseName", null, INVALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseVersion1() {
        helpTestReasonWhyInvalid("DatabaseVersion", "Valid VDB Version", VALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseVersion2() {
        helpTestReasonWhyInvalid("DatabaseVersion", "1", VALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseVersion3() {
        helpTestReasonWhyInvalid("DatabaseVersion", "1.2.3", VALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseVersion4() {
        helpTestReasonWhyInvalid("DatabaseVersion", "1 2 3", VALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseVersion5() {
        helpTestReasonWhyInvalid("DatabaseVersion", "", VALID);
    }

    @Test
    public void testReasonWhyInvalidDatabaseVersion6() {
        helpTestReasonWhyInvalid("DatabaseVersion", null, VALID);
    }


    @Test
    public void testReasonWhyInvalidDataSourceName1() {
        helpTestReasonWhyInvalid("DataSourceName", "Valid Data Source Name", VALID);
    }

    @Test
    public void testReasonWhyInvalidDataSourceName2() {
        helpTestReasonWhyInvalid("DataSourceName", "", VALID);
    }

    @Test
    public void testReasonWhyInvalidDataSourceName3() {
        helpTestReasonWhyInvalid("DataSourceName", "", VALID);
    }

    @Test
    public void testReasonWhyInvalidDescription1() {
        helpTestReasonWhyInvalid("Description", "Valid App Name", VALID);
    }

    @Test
    public void testReasonWhyInvalidDescription2() {
        helpTestReasonWhyInvalid("Description", "", VALID);
    }

    @Test
    public void testReasonWhyInvalidDescription3() {
        helpTestReasonWhyInvalid("Description", null, VALID);
    }

    @Test
    public void testReasonWhyInvalidPortNumber1() {
        helpTestReasonWhyInvalid("PortNumber", 1, VALID);
    }

    @Test
    public void testReasonWhyInvalidPortNumber2() {
        helpTestReasonWhyInvalid("PortNumber", 9999999, INVALID);
    }

    @Test
    public void testReasonWhyInvalidPortNumber3() {
        helpTestReasonWhyInvalid("PortNumber", 0, VALID);
    }

    @Test
    public void testReasonWhyInvalidPortNumber4() {
        helpTestReasonWhyInvalid("PortNumber", -1, INVALID);
    }

    @Test
    public void testReasonWhyInvalidServerName1() {
        helpTestReasonWhyInvalid("ServerName", "Valid Server Name", VALID);
    }

    @Test
    public void testReasonWhyInvalidServerName2() {
        helpTestReasonWhyInvalid("ServerName", "Valid Server Name", VALID);
    }

    @Test
    public void testReasonWhyInvalidServerName3() {
        helpTestReasonWhyInvalid("ServerName", "", INVALID);
    }

    @Test
    public void testReasonWhyInvalidServerName4() {
        helpTestReasonWhyInvalid("ServerName", null, INVALID);
    }

    @Test
    public void testReasonWhyInvalidTransactionAutoWrap1() {
        helpTestReasonWhyInvalid("TransactionAutoWrap", KublingDataSource.TXN_WRAP_OFF, VALID);
    }

    @Test
    public void testReasonWhyInvalidTransactionAutoWrap2() {
        helpTestReasonWhyInvalid("TransactionAutoWrap", KublingDataSource.TXN_WRAP_ON, VALID);
    }

    @Test
    public void testReasonWhyInvalidTransactionAutoWrap3() {
        helpTestReasonWhyInvalid("TransactionAutoWrap", KublingDataSource.TXN_WRAP_AUTO, VALID);
    }

    @Test
    public void testReasonWhyInvalidTransactionAutoWrap5() {
        helpTestReasonWhyInvalid("TransactionAutoWrap", "off", INVALID);    // lowercase value
    }

    @Test
    public void testReasonWhyInvalidTransactionAutoWrap6() {
        helpTestReasonWhyInvalid("TransactionAutoWrap", "Invalid AutoWrap", INVALID);
    }

    @Test
    public void testreasonWhyInvalidPartialResultsMode1() {
        helpTestReasonWhyInvalid("partialResultsMode", "Invalid partial mode", INVALID);
    }

    @Test
    public void testreasonWhyInvalidPartialResultsMode2() {
        helpTestReasonWhyInvalid("partialResultsMode", "true", VALID);
    }

    @Test
    public void testReasonWhyInvalidSocketsPerVM1() {
        helpTestReasonWhyInvalid("socketsPerVM", null, VALID);
    }

    @Test
    public void testReasonWhyInvalidSocketsPerVM2() {
        helpTestReasonWhyInvalid("socketsPerVM", "4", VALID);
    }

    @Test
    public void testReasonWhyInvalidSocketsPerVM3() {
        helpTestReasonWhyInvalid("socketsPerVM", "-3", INVALID);
    }

    @Test
    public void testReasonWhyInvalidSocketsPerVM4() {
        helpTestReasonWhyInvalid("socketsPerVM", "5.6", INVALID);
    }

    @Test
    public void testReasonWhyInvalidStickyConnections1() {
        helpTestReasonWhyInvalid("stickyConnections", null, VALID);
    }

    @Test
    public void testReasonWhyInvalidStickyConnections2() {
        helpTestReasonWhyInvalid("stickyConnections", "false", VALID);
    }

    @Test
    public void testReasonWhyInvalidStickyConnections3() {
        helpTestReasonWhyInvalid("stickyConnections", "YES", INVALID);
    }

    public void helpTestAlternateServer(String altServers, boolean valid) {
        this.dataSource.setAlternateServers(altServers);
        try {
            this.dataSource.buildURL();
            if (!valid) {
                fail("expected exception");
            }
        } catch (KublingSQLException e) {
            if (valid) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testReasonWhyInvalidAlternateServers1() {
        helpTestAlternateServer(null, VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers2() {
        helpTestAlternateServer("", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers3() {
        helpTestAlternateServer("server", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers4() {
        helpTestAlternateServer("server:100", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers5() {
        helpTestAlternateServer("server:port", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers6() {
        helpTestAlternateServer("server:100:1", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers7() {
        helpTestAlternateServer("server:100:abc", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers8() {
        helpTestAlternateServer("server:abc:100", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers9() {
        helpTestAlternateServer(":100", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers10() {
        helpTestAlternateServer(":abc", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers11() {
        helpTestAlternateServer("server1:100,server2", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers12() {
        helpTestAlternateServer("server1:100,server2:101", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers13() {
        helpTestAlternateServer("server1:100,", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers14() {
        helpTestAlternateServer("server1:100,server2:abc", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers15() {
        helpTestAlternateServer("server1:100,server2:101:abc", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers16() {
        helpTestAlternateServer("server1,server2:100", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers17() {
        helpTestAlternateServer("server1,server2", VALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers18() {
        helpTestAlternateServer(",server2:100", INVALID);
    }

    @Test
    public void testReasonWhyInvalidAlternateServers19() {
        helpTestAlternateServer("server1,server2,server3,server4:500", VALID);
    }

    // ----------------------------------------------------------------
    //                       Test building URLs
    // ----------------------------------------------------------------

    @Test
    public void testBuildingURL1() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "1.2.3";
        final int portNumber = 7001;
        final String transactionAutoWrap = null;
        final String partialMode = "true";
        final boolean secure = false;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, null, transactionAutoWrap, partialMode, 500, false, secure,
                "jdbc:kubling:vdbName@mm://hostname:7001;fetchSize=500;ApplicationName=JDBC;VirtualDatabaseVersion=1.2.3;partialResultsMode=true;VirtualDatabaseName=vdbName");
    }

    @Test
    public void testBuildingIPv6() {
        final String serverName = "3ffe:ffff:0100:f101::1";
        final String vdbName = "vdbName";
        final String vdbVersion = "1";
        final int portNumber = 7001;
        final String transactionAutoWrap = null;
        final String partialMode = "true";
        final boolean secure = false;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, null, transactionAutoWrap, partialMode, 500, false, secure,
                "jdbc:kubling:vdbName@mm://[3ffe:ffff:0100:f101::1]:7001;fetchSize=500;ApplicationName=JDBC;VirtualDatabaseVersion=1;partialResultsMode=true;VirtualDatabaseName=vdbName");
    }

    @Test
    public void testBuildingIPv6WithBrackets() {
        final String serverName = "[3ffe:ffff:0100:f101::1]";
        final String vdbName = "vdbName";
        final String vdbVersion = "1";
        final int portNumber = 7001;
        final String transactionAutoWrap = null;
        final String partialMode = "true";
        final boolean secure = false;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, null, transactionAutoWrap, partialMode, 500, false, secure,
                "jdbc:kubling:vdbName@mm://[3ffe:ffff:0100:f101::1]:7001;fetchSize=500;ApplicationName=JDBC;VirtualDatabaseVersion=1;partialResultsMode=true;VirtualDatabaseName=vdbName");
    }

    @Test
    public void testBuildingIPv6Alternate() {
        final String serverName = "3ffe:ffff:0100:f101::1";
        final String vdbName = "vdbName";
        final String vdbVersion = "1";
        final int portNumber = 7001;
        final String transactionAutoWrap = null;
        final String partialMode = "true";
        final boolean secure = false;
        final String alternates = "[::1],127.0.0.1:1234";
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, alternates, transactionAutoWrap, partialMode, 500, false, secure,
                "jdbc:kubling:vdbName@mm://[3ffe:ffff:0100:f101::1]:7001,[::1]:7001,127.0.0.1:1234;fetchSize=500;ApplicationName=JDBC;VirtualDatabaseVersion=1;partialResultsMode=true;VirtualDatabaseName=vdbName");
    }

    @Test
    public void testBuildingURL2() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = 7001;
        final String transactionAutoWrap = KublingDataSource.TXN_WRAP_AUTO;
        final String partialMode = "false";
        final boolean secure = false;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, null, transactionAutoWrap, partialMode, -1, false, secure,
                "jdbc:kubling:vdbName@mm://hostname:7001;ApplicationName=JDBC;partialResultsMode=false;autoCommitTxn=DETECT;VirtualDatabaseName=vdbName");
    }

    @Test
    public void testBuildURL3() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = 7001;
        final String transactionAutoWrap = KublingDataSource.TXN_WRAP_AUTO;
        final String partialMode = "false";
        final boolean secure = false;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, null, transactionAutoWrap, partialMode, -1, true, secure,
                "jdbc:kubling:vdbName@mm://hostname:7001;ApplicationName=JDBC;SHOWPLAN=ON;partialResultsMode=false;autoCommitTxn=DETECT;VirtualDatabaseName=vdbName");
    }

    // Test secure protocol
    @Test
    public void testBuildURL4() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = 7001;
        final String transactionAutoWrap = KublingDataSource.TXN_WRAP_AUTO;
        final String partialMode = "false";
        final boolean secure = true;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, null, transactionAutoWrap, partialMode, -1, true, secure,
                "jdbc:kubling:vdbName@mms://hostname:7001;ApplicationName=JDBC;SHOWPLAN=ON;partialResultsMode=false;autoCommitTxn=DETECT;VirtualDatabaseName=vdbName");
    }

    /*
     * Test alternate servers list
     *
     * Server list uses server:port pairs
     */
    @Test
    public void testBuildURL5() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = 7001;
        final String alternateServers = "hostName:7002,hostName2:7001,hostName2:7002";
        final String transactionAutoWrap = KublingDataSource.TXN_WRAP_AUTO;
        final String partialMode = "false";
        final boolean secure = false;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, alternateServers, transactionAutoWrap, partialMode, -1, true, secure,
                "jdbc:kubling:vdbName@mm://hostName:7001,hostName:7002,hostName2:7001,hostName2:7002;ApplicationName=JDBC;SHOWPLAN=ON;partialResultsMode=false;autoCommitTxn=DETECT;VirtualDatabaseName=vdbName");
    }

    /*
     * Test alternate servers list
     *
     * Server list uses server:port pairs and we set secure to true
     */
    @Test
    public void testBuildURL6() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = 7001;
        final String alternateServers = "hostName:7002,hostName2:7001,hostName2:7002";
        final String transactionAutoWrap = KublingDataSource.TXN_WRAP_AUTO;
        final String partialMode = "false";
        final boolean secure = true;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, alternateServers, transactionAutoWrap, partialMode, -1, true, secure,
                "jdbc:kubling:vdbName@mms://hostName:7001,hostName:7002,hostName2:7001,hostName2:7002;ApplicationName=JDBC;SHOWPLAN=ON;partialResultsMode=false;autoCommitTxn=DETECT;VirtualDatabaseName=vdbName");
    }

    /*
     * Test alternate servers list
     *
     * Server list uses server:port pairs and server with no port
     * In this case, the server with no port should default to ds.portNumber.
     */
    @Test
    public void testBuildURL7() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = 7001;
        final String alternateServers = "hostName:7002,hostName2,hostName2:7002";
        final String transactionAutoWrap = KublingDataSource.TXN_WRAP_AUTO;
        final String partialMode = "false";
        final boolean secure = false;
        helpTestBuildingURL(vdbName, vdbVersion, serverName, portNumber, alternateServers, transactionAutoWrap, partialMode, -1, true, secure,
                "jdbc:kubling:vdbName@mm://hostName:7001,hostName:7002,hostName2:7001,hostName2:7002;ApplicationName=JDBC;SHOWPLAN=ON;partialResultsMode=false;autoCommitTxn=DETECT;VirtualDatabaseName=vdbName");
    }

    /**
     * Test turning off using JDBC4 semantics
     */
    @Test
    public void testBuildURL8() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "1.2.3";
        final int portNumber = 7001;
        final String transactionAutoWrap = null;
        final String partialMode = "true";
        final boolean secure = false;
        helpTestBuildingURL2(vdbName, vdbVersion, serverName, portNumber, null, transactionAutoWrap, partialMode, 500, false, secure, false,
                "jdbc:kubling:vdbName@mm://hostname:7001;fetchSize=500;ApplicationName=JDBC;VirtualDatabaseVersion=1.2.3;partialResultsMode=true;useJDBC4ColumnNameAndLabelSemantics=false;VirtualDatabaseName=vdbName");
    }

    @Test
    public void testBuildURL_AdditionalProperties() throws KublingSQLException {
        final KublingDataSource ds = new KublingDataSource();
        ds.setAdditionalProperties("foo=bar;a=b");
        ds.setServerName("hostName");
        ds.setDatabaseName("vdbName");
        ds.setPortNumber(1);
        assertEquals("jdbc:kubling:vdbName@mm://hostname:1;ApplicationName=JDBC;VirtualDatabaseName=vdbName;a=b;fetchSize=2048;foo=bar", ds.buildURL().getJDBCURL());
    }

    @Test
    public void testBuildURLEncryptRequests() throws KublingSQLException {
        final KublingDataSource ds = new KublingDataSource();
        ds.setServerName("hostName");
        ds.setDatabaseName("vdbName");
        ds.setEncryptRequests(true);
        compareUrls("jdbc:kubling:vdbName@mm://hostname:0;fetchSize=2048;ApplicationName=JDBC;encryptRequests=true;VirtualDatabaseName=vdbName", ds.buildURL().getJDBCURL());
    }

    @Test
    public void testInvalidDataSource() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = -1;              // this is what is invalid
        final String dataSourceName = null;
        final String transactionAutoWrap = null;
        try {
            helpTestConnection(vdbName, vdbVersion, serverName, portNumber, null, null, null, dataSourceName, transactionAutoWrap,
                    "false");       // TRUE TO OVERRIDE USERNAME & PASSWORD
            fail("Unexpectedly able to connect");
        } catch (SQLException e) {
            // this is expected!
        }
    }

    /*
     * Test invalid alternateServer list
     *
     * Server list uses a non-numeric value for port.
     */
    @Test
    public void testInvalidDataSource2() {
        final String serverName = "hostName";
        final String vdbName = "vdbName";
        final String vdbVersion = "";
        final int portNumber = 31000;
        final String alternateServers = "hostName:-1"; // this is what is invalid
        final String dataSourceName = null;
        final String transactionAutoWrap = null;
        try {
            helpTestConnection(vdbName, vdbVersion, serverName, portNumber,
                    alternateServers, null, null, dataSourceName, transactionAutoWrap, "false");      // TRUE TO OVERRIDE USERNAME & PASSWORD
            fail("Unexpectedly able to connect");
        } catch (SQLException e) {
            // this is expected!
        }
    }

    @Test
    public void testUrlEncodedProperties() throws SQLException {
        KublingDriver td = Mockito.mock(KublingDriver.class);
        KublingDataSource tds = new KublingDataSource(td);
        tds.setDatabaseName("y");
        tds.setUser("%25user");
        tds.setServerName("x");
        tds.getConnection();

        ArgumentCaptor<Properties> argument = ArgumentCaptor.forClass(Properties.class);
        Mockito.verify(td).connect(Mockito.eq("jdbc:kubling:y@mm://x:0"), argument.capture());
        Properties p = argument.getValue();
        assertEquals("%25user", p.getProperty(BaseDataSource.USER_NAME));
    }

    @Test
    public void testLoginTimeout() throws SQLException {
        KublingDriver td = Mockito.mock(KublingDriver.class);
        KublingDataSource tds = new KublingDataSource(td);
        tds.setDatabaseName("y");
        tds.setServerName("x");
        tds.setLoginTimeout(2);
        tds.getConnection();

        ArgumentCaptor<Properties> argument = ArgumentCaptor.forClass(Properties.class);
        Mockito.verify(td).connect(Mockito.eq("jdbc:kubling:y@mm://x:0"), argument.capture());
        Properties p = argument.getValue();
        assertEquals("2", p.getProperty(KublingURL.CONNECTION.LOGIN_TIMEOUT));
    }

    @Test
    public void testGetConnectionWithUser() throws SQLException {
        KublingDriver td = Mockito.mock(KublingDriver.class);
        KublingDataSource tds = new KublingDataSource(td);
        tds.setDatabaseName("y");
        tds.setUser("%25user");
        tds.setServerName("x");
        tds.getConnection("user", "password");

        ArgumentCaptor<Properties> argument = ArgumentCaptor.forClass(Properties.class);
        Mockito.verify(td).connect(Mockito.eq("jdbc:kubling:y@mm://x:0"), argument.capture());
        Properties p = argument.getValue();
        assertEquals("user", p.getProperty(BaseDataSource.USER_NAME));
    }

}
