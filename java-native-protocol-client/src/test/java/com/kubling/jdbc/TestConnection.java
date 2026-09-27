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

import com.kubling.client.DQP;
import com.kubling.client.security.LogonResult;
import com.kubling.client.security.SessionToken;
import com.kubling.client.util.ResultsFuture;
import com.kubling.client.xa.XATransactionException;
import com.kubling.client.xa.XidImpl;
import com.kubling.net.ServerConnection;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.sql.Array;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("nls")
public class TestConnection {

    protected static final String STD_DATABASE_NAME = "QT_Ora9DS";

    static String serverUrl = "jdbc:kubling:QT_Ora9DS@mm://localhost:7001;version=1;user=metamatrixadmin;password=mm";

    static class InnerDriver extends KublingDriver {
        String iurl;

        public InnerDriver(String url) {
            iurl = url;
        }

        public void parseUrl(Properties props) throws SQLException {
            super.parseURL(iurl, props);
        }
    }

    public static ConnectionImpl getMMConnection() {
        return getMMConnection(serverUrl);
    }

    public static ConnectionImpl getMMConnection(String url) {
        ServerConnection mock = mock(ServerConnection.class);
        DQP dqp = mock(DQP.class);
        try {
            when(dqp.start(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).then((Answer) invocation -> ResultsFuture.NULL_FUTURE);
            when(dqp.rollback(Mockito.any())).then((Answer) invocation -> ResultsFuture.NULL_FUTURE);
            when(dqp.rollback()).then((Answer) invocation -> ResultsFuture.NULL_FUTURE);
        } catch (XATransactionException e) {
            throw new RuntimeException(e);
        }

        Properties props = new Properties();

        try {
            new InnerDriver(url).parseUrl(props);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        when(mock.getService(DQP.class)).thenReturn(dqp);

        when(mock.getLogonResult()).thenReturn(
                new LogonResult(
                        new SessionToken(1, "admin"), STD_DATABASE_NAME, "fake"));
        return new ConnectionImpl(mock, props, url);
    }

    @Test
    public void testGetMetaData() throws Exception {
        assertNotNull(getMMConnection().getMetaData());
    }

    @Test
    public void testNullSorts() throws Exception {
        DatabaseMetaData metadata = getMMConnection("jdbc:kubling:QT_Ora9DS@mm://localhost:7001;version=1;NullsAreSorted=AtEnd").getMetaData();
        assertTrue(metadata.nullsAreSortedAtEnd());
        assertFalse(metadata.nullsAreSortedLow());
        metadata = getMMConnection("jdbc:kubling:QT_Ora9DS@mm://localhost:7001;version=1").getMetaData();
        assertFalse(metadata.nullsAreSortedAtEnd());
    }

    @Test
    public void testGetSchema() throws Exception {
        assertEquals(STD_DATABASE_NAME, getMMConnection().getVDBName(),
                "Actual schema is not equal to the expected one. ");
    }

    @Test
    public void testNativeSql() {
        String sql = "SELECT * FROM BQT1.SmallA";
        assertEquals(sql, getMMConnection().nativeSQL(sql),
                "Actual schema is not equal to the expected one. ");
    }

    /**
     * test getUserName() through DriverManager
     */
    @Test
    public void testGetUserName2() throws Exception {
        assertEquals("admin", getMMConnection().getUserName(),
                "Actual userName is not equal to the expected one. ");
    }

    /**
     * test isReadOnly default value on Connection
     */
    @Test
    public void testIsReadOnly() throws Exception {
        assertFalse(getMMConnection().isReadOnly());
    }

    /**
     * test setReadOnly on Connection
     */
    @Test
    public void testSetReadOnly1() throws Exception {
        ConnectionImpl conn = getMMConnection();
        conn.setReadOnly(true);
        assertTrue(conn.isReadOnly());
    }

    /**
     * test setReadOnly on Connection during a transaction
     */
    @Test
    public void testSetReadOnly2() throws Exception {
        ConnectionImpl conn = getMMConnection();
        conn.setAutoCommit(false);
        conn.setReadOnly(true);
        conn.setInLocalTxn(true);
        try {
            conn.setReadOnly(false);
            fail("Error Expected");
        } catch (SQLException e) {
            // error expected
        }
    }

    /**
     * Test the default of the JDBC4 spec semantics is true
     */
    @Test
    public void testDefaultSpec() {
        assertEquals("true",
                (getMMConnection().getExecutionProperties()
                        .getProperty(ExecutionProperties.JDBC4COLUMNNAMEANDLABELSEMANTICS) == null ? "true" : "false"));
    }

    /**
     * Test turning off the JDBC 4 semantics
     */
    @Test
    public void testTurnOnSpec() {
        assertEquals("true",
                getMMConnection(serverUrl + ";useJDBC4ColumnNameAndLabelSemantics=true")
                        .getExecutionProperties().getProperty(ExecutionProperties.JDBC4COLUMNNAMEANDLABELSEMANTICS));
    }

    /**
     * Test turning off the JDBC 4 semantics
     */
    @Test
    public void testTurnOffSpec() {
        assertEquals("false",
                getMMConnection(serverUrl + ";useJDBC4ColumnNameAndLabelSemantics=false")
                        .getExecutionProperties().getProperty(ExecutionProperties.JDBC4COLUMNNAMEANDLABELSEMANTICS));
    }

    @Test
    public void testCreateArray() throws SQLException {
        Array array = getMMConnection().createArrayOf("integer[]", new Integer[]{3, 4});
        assertEquals(3, java.lang.reflect.Array.get(array.getArray(), 0));
    }

    @Test
    public void testXACommit() throws Exception {
        ConnectionImpl conn = getMMConnection();
        conn.setAutoCommit(false);
        conn.setTransactionXid(Mockito.mock(XidImpl.class));
        try {
            conn.setAutoCommit(true);
            fail("Error Expected");
        } catch (SQLException e) {
            // error expected
        }
    }

    @Test
    public void testMaxOpenStatements() throws SQLException {
        ConnectionImpl conn = getMMConnection();
        for (int i = 0; i < 1000; i++) {
            conn.createStatement();
        }
        try {
            conn.createStatement();
            fail("MaxOpenStatements not limited to required number.");
        } catch (KublingSQLException ex) {
            MatcherAssert.assertThat(ex.getMessage(), CoreMatchers.containsString(JDBCPlugin.Event.KBL20036.name()));
        }
    }
}
