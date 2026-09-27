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
import org.junit.jupiter.api.Test;

import java.sql.DriverPropertyInfo;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class TestKublingDriver {
    KublingDriver drv = new KublingDriver();

    @Test
    public void testAccepts() {
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:12345"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:12345;user=foo;password=bar"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@/foo/blah/deploy.properties"));

        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:12345"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:12345;user=foo;password=bar"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@/foo/blah/deploy.properties"));
        assertTrue(drv.acceptsURL("jdbc:kubling:8294601c-9fe9-4244-9499-4a012c5e1476_vdb"));
        assertTrue(drv.acceptsURL("jdbc:kubling:8294601c-9fe9-4244-9499-4a012c5e1476_vdb@mm://localhost:12345"));
        assertTrue(drv.acceptsURL("jdbc:kubling:test_vdb@mm://local-host:12345"));
        assertTrue(drv.acceptsURL("jdbc:kubling:test_vdb@mm://local_host:12345"));
        assertTrue(drv.acceptsURL("jdbc:kubling:test_vdb.1@mm://local_host:12345"));
        assertTrue(drv.acceptsURL("jdbc:kubling:test_vdb.10@mm://local_host:12345"));
    }

    /**
     * Valid format of urls
     */
    @Test
    public void testAcceptsURL1() {
        assertTrue(drv.acceptsURL("jdbc:kubling:jvdb@mm://localhost:1234"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234;version=x"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234,localhost2:12342,localhost3:12343"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234,localhost2:12342,localhost3:12343;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mms://localhost:1234;logLevel=1;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@mm://localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log;autoCommitTxn=OFF;paritalResultsMode=true"));
        assertTrue(drv.acceptsURL("jdbc:kubling:jvdb@mms://localhost:1234"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234;version=x"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234,localhost2:12342,localhost3:12343"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mms://localhost:1234,localhost2:12342,localhost3:12343;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mms://localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log;autoCommitTxn=OFF;paritalResultsMode=true"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://127.0.0.1:1234;logLevel=2"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mms://127.0.0.1:1234"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://127.0.0.1:1234,localhost.mydomain.com:63636;logLevel=2"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://my-host.mydomain.com:53535,127.0.0.1:1234"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://123.123.123.123:53535,127.0.0.1:1234"));

        //DQP type
        assertTrue(drv.acceptsURL("jdbc:kubling:jvdb@c:/dqp.properties;version=1"));
        assertTrue(drv.acceptsURL("jdbc:kubling:jvdb@/foo/dqp.properties;version=1"));
        assertTrue(drv.acceptsURL("jdbc:kubling:jvdb@../foo/dqp.properties;version=1"));

        assertTrue(drv.acceptsURL("jdbc:kubling:jvdb@mm://localhost:port"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@localhost:port;version=x"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234,localhost2:12342,localhost3:12343"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234,localhost2:12342,localhost3:12343;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234;logLevel=1;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234;logLevel=2;logFile=D:\\metamatrix\\work\\DQP\\log\\jdbcLogFile.log;autoCommitTxn=OFF;paritalResultsMode=true"));
        assertFalse(drv.acceptsURL("jdbc:kubling:@localhost:1234;stickyConnections=false;socketsPerVM=4"));
        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://my_host.mydomain.com:53535,127.0.0.1:1234"));

        assertTrue(drv.acceptsURL("jdbc:kubling:vdb@mm://localhost:1234;version=x;useJDBC4ColumnNameAndLabelSemantics=false"));

    }

    /**
     * Invalid format of urls
     */
    @Test
    public void testAcceptsURL2() {
        assertFalse(drv.acceptsURL("jdbc:matamatrix:test"));
        assertFalse(drv.acceptsURL("metamatrix:test"));
        assertFalse(drv.acceptsURL("jdbc&matamatrix:test"));
        assertFalse(drv.acceptsURL("jdbc;metamatrix:test"));
    }

    @Test
    public void testParseURL() throws Exception {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT@mm://slwxp157:1234", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("mm://slwxp157:1234", p.getProperty(KublingURL.CONNECTION.SERVER_URL));
        assertEquals(3, p.size());
    }

    @Test
    public void testParseURL2() throws Exception {
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
    public void testParseURL3() throws Exception {
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
    public void testGetPropertyInfo1() throws Exception {
        DriverPropertyInfo[] info = drv.getPropertyInfo("jdbc:kubling:vdb@mm://localhost:12345;applicationName=x", null);

        assertEquals(27, info.length);
        assertFalse(info[1].required);
        assertEquals("ApplicationName", info[1].name);
        assertEquals("x", info[1].value);

        for (DriverPropertyInfo dpi : info) {
            assertFalse(dpi.description.startsWith("<Missing message"), dpi.name);
        }
    }

}
