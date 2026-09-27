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
import com.kubling.client.RequestMessage;
import com.kubling.client.ResultsMessage;
import com.kubling.client.util.ResultsFuture;
import com.kubling.net.ServerConnection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.sql.*;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestStatement {

    @Test
    public void testBatchExecution() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Mockito.when(conn.getConnectionProps()).thenReturn(new Properties());
        DQP dqp = Mockito.mock(DQP.class);
        ResultsFuture<ResultsMessage> results = new ResultsFuture<>();
        Mockito.when(dqp.executeRequest(Mockito.anyLong(), Mockito.any())).thenReturn(results);
        ResultsMessage rm = new ResultsMessage();
        rm.setResults(new List<?>[]{List.of(1), List.of(2)});
        rm.setUpdateResult(true);
        results.getResultsReceiver().receiveResults(rm);
        Mockito.when(conn.getDQP()).thenReturn(dqp);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.clearBatch(); //previously caused npe
        statement.addBatch("delete from table");
        statement.addBatch("delete from table1");
        assertArrayEquals(new int[]{1, 2}, statement.executeBatch());
    }

    @Test
    public void testWarnings() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Mockito.when(conn.getConnectionProps()).thenReturn(new Properties());
        DQP dqp = Mockito.mock(DQP.class);
        ResultsFuture<ResultsMessage> results = new ResultsFuture<>();
        Mockito.when(dqp.executeRequest(Mockito.anyLong(), Mockito.any())).thenReturn(results);
        ResultsMessage rm = new ResultsMessage();
        rm.setResults(new List<?>[]{List.of(1)});
        rm.setWarnings(List.of(new Throwable()));
        rm.setColumnNames(new String[]{"expr1"});
        rm.setDataTypes(new String[]{"string"});
        results.getResultsReceiver().receiveResults(rm);
        Mockito.when(conn.getDQP()).thenReturn(dqp);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY) {
            @Override
            protected TimeZone getServerTimeZone() {
                return null;
            }
        };
        statement.execute("select 'a'");
        assertNotNull(statement.getResultSet());
        SQLWarning warning = statement.getWarnings();
        assertNotNull(warning);
        assertNull(warning.getNextWarning());
    }

    @Test
    public void testGetMoreResults() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Mockito.when(conn.getConnectionProps()).thenReturn(new Properties());
        DQP dqp = Mockito.mock(DQP.class);
        ResultsFuture<ResultsMessage> results = new ResultsFuture<>();
        Mockito.when(dqp.executeRequest(Mockito.anyLong(), (RequestMessage) Mockito.any())).thenReturn(results);
        ResultsMessage rm = new ResultsMessage();
        rm.setUpdateResult(true);
        rm.setColumnNames(new String[]{"expr1"});
        rm.setDataTypes(new String[]{"integer"});
        rm.setResults(new List<?>[]{List.of(1)});
        results.getResultsReceiver().receiveResults(rm);
        Mockito.when(conn.getDQP()).thenReturn(dqp);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY) {
            @Override
            protected TimeZone getServerTimeZone() {
                return null;
            }
        };
        statement.execute("update x set a = b");
        assertEquals(1, statement.getUpdateCount());
        statement.getMoreResults(Statement.CLOSE_ALL_RESULTS);
        assertEquals(-1, statement.getUpdateCount());

        statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY) {
            @Override
            protected TimeZone getServerTimeZone() {
                return null;
            }
        };
        statement.execute("update x set a = b");
        assertEquals(1, statement.getUpdateCount());
        statement.getMoreResults();
        assertEquals(-1, statement.getUpdateCount());
    }

    @Test
    public void testSetStatement() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertFalse(statement.execute("set foo bar"));
        Mockito.verify(conn).setExecutionProperty("foo", "bar");

        assertFalse(statement.execute("set foo 'b''ar' ; "));
        Mockito.verify(conn).setExecutionProperty("foo", "b'ar");

        assertFalse(statement.execute("set \"foo\" 'b''a1r' ; "));
        Mockito.verify(conn).setExecutionProperty("foo", "b'a1r");

        assertFalse(statement.execute("set \"foo\" = 'bar'; "));
        Mockito.verify(conn).setExecutionProperty("foo", "bar");
    }

    @Test
    public void testSetPayloadStatement() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Properties p = new Properties();
        Mockito.when(conn.getExecutionProperties()).thenReturn(p);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertFalse(statement.execute("set payload foo bar"));
    }

    @Test
    public void testSetAuthorizationStatement() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Properties p = new Properties();
        Mockito.when(conn.getExecutionProperties()).thenReturn(p);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertFalse(statement.execute("set session authorization bar"));
        Mockito.verify(conn).changeUser("bar", null);
    }

    @Test
    public void testPropertiesOverride() {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Properties p = new Properties();
        p.setProperty(ExecutionProperties.ANSI_QUOTED_IDENTIFIERS, Boolean.TRUE.toString());
        Mockito.when(conn.getExecutionProperties()).thenReturn(p);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertEquals(Boolean.TRUE.toString(), statement.getExecutionProperty(ExecutionProperties.ANSI_QUOTED_IDENTIFIERS));
        statement.setExecutionProperty(ExecutionProperties.ANSI_QUOTED_IDENTIFIERS, Boolean.FALSE.toString());
        assertEquals(Boolean.FALSE.toString(), statement.getExecutionProperty(ExecutionProperties.ANSI_QUOTED_IDENTIFIERS));
        assertEquals(Boolean.TRUE.toString(), p.getProperty(ExecutionProperties.ANSI_QUOTED_IDENTIFIERS));
    }

    @Test
    public void testTransactionStatements() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Properties p = new Properties();
        Mockito.when(conn.getExecutionProperties()).thenReturn(p);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertFalse(statement.execute("start transaction"));
        Mockito.verify(conn).setAutoCommit(false);
        assertFalse(statement.execute("commit"));
        Mockito.verify(conn).setAutoCommit(true);
        assertFalse(statement.execute("start transaction"));
        assertFalse(statement.execute("rollback"));
        Mockito.verify(conn).rollback(false);
        assertFalse(statement.execute("abort transaction"));
        Mockito.verify(conn, Mockito.times(2)).rollback(false);
        assertFalse(statement.execute("rollback work"));
        Mockito.verify(conn, Mockito.times(3)).rollback(false);

        assertFalse(statement.execute("start transaction isolation level repeatable read"));
        Mockito.verify(conn).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    }

    @Test
    public void testDisableLocalTransations() throws Exception {
        ServerConnection mock = Mockito.mock(ServerConnection.class);
        DQP dqp = Mockito.mock(DQP.class);
        Mockito.when(mock.getService(DQP.class)).thenReturn(dqp);
        ConnectionImpl conn = new ConnectionImpl(mock, new Properties(), "x");
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertTrue(conn.getAutoCommit());
        statement.execute("set disablelocaltxn true");
        assertFalse(statement.execute("start transaction"));
        conn.beginLocalTxnIfNeeded();
        assertFalse(conn.isInLocalTxn());

        statement.execute("set disablelocaltxn false");
        assertFalse(statement.execute("start transaction"));
        conn.beginLocalTxnIfNeeded();
        assertTrue(conn.isInLocalTxn());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTransactionStatementsAsynch() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Mockito.when(conn.submitSetAutoCommitTrue(Mockito.anyBoolean()))
                .thenReturn((ResultsFuture) ResultsFuture.NULL_FUTURE);
        Properties p = new Properties();
        Mockito.when(conn.getExecutionProperties()).thenReturn(p);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.submitExecute("start transaction", null);
        Mockito.verify(conn).setAutoCommit(false);
        statement.submitExecute("commit", null);
        Mockito.verify(conn).submitSetAutoCommitTrue(true);
        statement.submitExecute("start transaction", null);
        statement.submitExecute("rollback", null);
        Mockito.verify(conn).submitSetAutoCommitTrue(false);
    }

    @Test
    public void testAsynchTimeout() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Mockito.when(conn.getConnectionProps()).thenReturn(new Properties());
        final StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setQueryTimeoutMS(1);
        DQP dqp = Mockito.mock(DQP.class);
        Mockito.when(statement.getDQP()).thenReturn(dqp);
        final AtomicInteger counter = new AtomicInteger();
        Mockito.when(dqp.cancelRequest(0)).then((Answer<Boolean>) invocation -> {
            synchronized (statement) {
                counter.incrementAndGet();
                statement.notifyAll();
            }
            return true;
        });
        ResultsFuture<ResultsMessage> future = new ResultsFuture<>();
        Mockito.when(dqp.executeRequest(Mockito.anyLong(), Mockito.any())).thenReturn(future);
        statement.submitExecute("select 'hello world'", null);
        synchronized (statement) {
            while (counter.get() != 1) {
                statement.wait();
            }
        }
        statement.setQueryTimeoutMS(1);
        statement.submitExecute("select 'hello world'", null);
        synchronized (statement) {
            while (counter.get() != 2) {
                statement.wait();
            }
        }
    }

    @Test
    public void testTimeoutProperty() throws Exception {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Properties p = new Properties();
        p.setProperty(ExecutionProperties.QUERYTIMEOUT, "2");
        Mockito.when(conn.getExecutionProperties()).thenReturn(p);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertEquals(2, statement.getQueryTimeout());
    }

    @Test
    public void testUseJDBC4ColumnNameAndLabelSemantics() {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Properties p = new Properties();
        p.setProperty(ExecutionProperties.JDBC4COLUMNNAMEANDLABELSEMANTICS, "false");

        Mockito.when(conn.getExecutionProperties()).thenReturn(p);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertEquals(Boolean.FALSE.toString(),
                statement.getExecutionProperty(ExecutionProperties.JDBC4COLUMNNAMEANDLABELSEMANTICS));

    }

    @Test
    public void testSet() {
        Matcher m = StatementImpl.SET_STATEMENT.matcher("set foo to 1");
        assertTrue(m.matches());
    }

    @Test
    public void testQuotedSet() {
        Matcher m = StatementImpl.SET_STATEMENT.matcher("set \"foo\"\"\" to 1");
        assertTrue(m.matches());
        assertEquals("\"foo\"\"\"", m.group(2));
        m = StatementImpl.SHOW_STATEMENT.matcher("show \"foo\"");
        assertTrue(m.matches());
    }

    @Test
    public void testSetTxnIsolationLevel() throws SQLException {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        assertFalse(statement.execute("set session characteristics as transaction isolation level read committed"));
        Mockito.verify(conn).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        assertFalse(statement.execute("set session characteristics as transaction isolation level read uncommitted"));
        Mockito.verify(conn).setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        assertFalse(statement.execute("set session characteristics as transaction isolation level serializable"));
        Mockito.verify(conn).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        assertFalse(statement.execute("set session characteristics as transaction isolation level repeatable read"));
        Mockito.verify(conn).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
    }

    @Test
    public void testShowTxnIsolationLevel() throws SQLException {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        StatementImpl statement = new StatementImpl(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY) {
            @Override
            protected TimeZone getServerTimeZone() {
                return TimeZone.getDefault();
            }
        };
        Mockito.when(conn.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        assertTrue(statement.execute("show transaction isolation level"));
        ResultSet rs = statement.getResultSet();
        rs.next();
        assertEquals("READ COMMITTED", rs.getString(1));
        assertFalse(rs.next());
    }

}
