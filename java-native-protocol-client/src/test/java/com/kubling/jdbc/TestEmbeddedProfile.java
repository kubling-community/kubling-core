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
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class TestEmbeddedProfile {

    /*
     * Test method for 'com.metamatrix.jdbc.EmbeddedEmbeddedProfile.acceptsURL(String)'
     * // (\\w:[\\\\,\\/]|file:\\/\\/|\\/|\\\\|(\\.){1,2}){1}
     */
    @Test
    public void testAcceptsURL() {

        // ClassPath based URL
        assertNotSame(ConnectionType.Embedded, JDBCURL.acceptsUrl("jdbc:kubling:BQT@classpath:/dqp.properties;partialResultsMode=true"));

        assertEquals(ConnectionType.Embedded, JDBCURL.acceptsUrl("jdbc:kubling:BQT"));
        assertEquals(ConnectionType.Embedded, JDBCURL.acceptsUrl("jdbc:kubling:BQT!/path/foo.properties"));
        assertEquals(ConnectionType.Embedded, JDBCURL.acceptsUrl("jdbc:kubling:BQT;"));
        assertEquals(ConnectionType.Embedded, JDBCURL.acceptsUrl("jdbc:kubling:BQT;version=1;logFile=foo.txt"));
        assertEquals(ConnectionType.Embedded, JDBCURL.acceptsUrl("jdbc:kubling:BQT.1;version=1;logFile=foo.txt"));
    }

    @Test
    public void testParseURL() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals(2, p.size());
    }

    @Test
    public void testParseURL2() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT;version=3", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("3", p.getProperty(BaseDataSource.VDB_VERSION));
        assertEquals("3", p.getProperty(BaseDataSource.VERSION));
        assertEquals(4, p.size());
    }

    @Test
    public void testParseURL3() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT;version=4;autoCommitTxn=ON;partialResultsMode=YES;", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("4", p.getProperty(BaseDataSource.VDB_VERSION));
        assertEquals("4", p.getProperty(BaseDataSource.VERSION));
        assertEquals("ON", p.getProperty(ExecutionProperties.PROP_TXN_AUTO_WRAP));
        assertEquals("YES", p.getProperty(ExecutionProperties.PROP_PARTIAL_RESULTS_MODE));
        assertEquals(6, p.size());
    }

    @Test
    public void testParseURL4() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT;partialResultsMode=true", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("true", p.getProperty(ExecutionProperties.PROP_PARTIAL_RESULTS_MODE));
        assertEquals(3, p.size());
    }

    @Test
    public void testParseURL5() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
    }

    @Test
    public void testParseURL55() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT;", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
    }

    @Test
    public void testParseURL6() throws SQLException {
        Properties p = new Properties();
        KublingDriver.parseURL("jdbc:kubling:BQT;partialResultsMode=true;version=1", p);
        assertEquals("BQT", p.getProperty(BaseDataSource.VDB_NAME));
        assertEquals("true", p.getProperty(ExecutionProperties.PROP_PARTIAL_RESULTS_MODE));
        assertEquals("1", p.getProperty(BaseDataSource.VDB_VERSION));
        assertEquals(5, p.size());

    }

    @Test
    public void test() throws Exception {
        try {
            Class.forName("com.kubling.jdbc.KublingDriver");
            DriverManager.getConnection("jdbc:kubling:Parts@invalidConfig.properties;version=1");
            fail();
        } catch (SQLException e) {
        }
    }
}
