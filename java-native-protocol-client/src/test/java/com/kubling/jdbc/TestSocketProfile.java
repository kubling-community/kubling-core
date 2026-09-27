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

import com.kubling.jdbc.JDBCURL.ConnectionType;
import com.kubling.net.KublingURL;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class TestSocketProfile {

    /**
     * Valid format of urls
     */
    @Test
    public void testAcceptsURL1() {
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@mm://localhost:1234"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@mm://localhost:1234"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234;version=x"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234,localhost2:12342,localhost3:12343"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234,localhost2:12342,localhost3:12343;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mms://localhost:1234;logLevel=1;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@mm://localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log;autoCommitTxn=OFF;paritalResultsMode=true"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@mms://localhost:1234"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234;version=x"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://localhost:1234,localhost2:12342,localhost3:12343"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mms://localhost:1234,localhost2:12342,localhost3:12343;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mms://localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log;autoCommitTxn=OFF;paritalResultsMode=true"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://127.0.0.1:1234;logLevel=2"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mms://127.0.0.1:1234"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://127.0.0.1:1234,localhost.mydomain.com:63636;logLevel=2"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://my-host.mydomain.com:53535,127.0.0.1:1234"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://123.123.123.123:53535,127.0.0.1:1234"));

        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@localhost:1234"));

        //DQP type
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@c:/dqp.properties;version=1"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@/foo/dqp.properties;version=1"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@../foo/dqp.properties;version=1"));

        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:jvdb@mm://localhost:port"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@localhost:port;version=x"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234,localhost2:12342,localhost3:12343"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234,localhost2:12342,localhost3:12343;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234;logLevel=1;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log;autoCommitTxn=OFF;paritalResultsMode=true"));
        assertNull(JDBCURL.acceptsUrl("jdbc:kubling:@localhost:1234;stickyConnections=false;socketsPerVM=4"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://my_host.mydomain.com:53535,127.0.0.1:1234"));
    }

    /**
     * Invalid format of urls
     */
    @Test
    public void testAcceptsURL2() {
        assertFalse(KublingDriver.getInstance().acceptsURL("jdbc:matamatrix:test"));
        assertFalse(KublingDriver.getInstance().acceptsURL("metamatrix:test"));
        assertFalse(KublingDriver.getInstance().acceptsURL("jdbc&matamatrix:test"));
        assertFalse(KublingDriver.getInstance().acceptsURL("jdbc;metamatrix:test"));
    }

    @Test
    public void testParseURL() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT@mm://slwxp157:1234", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("mm://slwxp157:1234", p.getProperty(KublingURL.CONNECTION.SERVER_URL));
        assertEquals(3, p.size());
    }

    @Test
    public void testParseURL2() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT@mms://slwxp157:1234;version=3", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("3", p.getProperty(BaseDataSource.VDB_VERSION));
        assertEquals("mms://slwxp157:1234", p.getProperty(KublingURL.CONNECTION.SERVER_URL));
        assertEquals("3", p.getProperty(BaseDataSource.VERSION));
        assertEquals(BaseDataSource.DEFAULT_APP_NAME, p.getProperty(BaseDataSource.APP_NAME));
        assertEquals(5, p.size());
    }

    @Test
    public void testParseURL3() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT@mm://slwxp157:1234,slntmm01:43401,sluxmm09:43302;version=4;autoCommitTxn=ON;partialResultsMode=YES;ApplicationName=Client", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("4", p.getProperty(BaseDataSource.VDB_VERSION));
        assertEquals("ON", p.getProperty(ExecutionProperties.PROP_TXN_AUTO_WRAP));
        assertEquals("YES", p.getProperty(ExecutionProperties.PROP_PARTIAL_RESULTS_MODE));
        assertEquals("mm://slwxp157:1234,slntmm01:43401,sluxmm09:43302", p.getProperty(KublingURL.CONNECTION.SERVER_URL));
        assertEquals("4", p.getProperty(BaseDataSource.VERSION));
        assertEquals("Client", p.getProperty(BaseDataSource.APP_NAME));
        assertEquals(7, p.size());
    }

    @Test
    public void testIPV6() throws SQLException {
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://[::1]:53535,127.0.0.1:1234"));
        assertEquals(ConnectionType.Socket, JDBCURL.acceptsUrl("jdbc:kubling:vdb@mm://[3ffe:ffff:0100:f101::1]:53535,127.0.0.1:1234"));

        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT@mms://[3ffe:ffff:0100:f101::1]:1234;version=3", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("3", p.getProperty(BaseDataSource.VDB_VERSION));
        assertEquals("mms://[3ffe:ffff:0100:f101::1]:1234", p.getProperty(KublingURL.CONNECTION.SERVER_URL));
        assertEquals("3", p.getProperty(BaseDataSource.VERSION));
    }

    @Test
    public void testIPV6MultipleHosts() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT@mms://[3ffe:ffff:0100:f101::1]:1234,[::1]:31000,127.0.0.1:2134;version=3", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("3", p.getProperty(BaseDataSource.VDB_VERSION));
        assertEquals("mms://[3ffe:ffff:0100:f101::1]:1234,[::1]:31000,127.0.0.1:2134", p.getProperty(KublingURL.CONNECTION.SERVER_URL));
        assertEquals("3", p.getProperty(BaseDataSource.VERSION));
    }
}
