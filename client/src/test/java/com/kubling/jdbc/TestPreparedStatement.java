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
import com.kubling.client.ParameterType;
import com.kubling.client.RequestMessage;
import com.kubling.client.ResultsMessage;
import com.kubling.client.security.LogonResult;
import com.kubling.client.util.ResultsFuture;
import com.kubling.core.KublingException;
import com.kubling.core.types.DataTypeManager;
import com.kubling.net.ServerConnection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test case to validate general operations on an <code>MMPreparedStatement
 * </code>
 */
public class TestPreparedStatement {

    /**
     * Verify that the <code>executeBatch()</code> method of <code>
     * MMPreparedStatement</code> is resulting in the correct command,
     * parameter values for each command of the batch, and the request type
     * are being set in the request message that would normally be sent to the
     * server.
     *
     */
    @Test
    public void testBatchedUpdateExecution() throws Exception {
        // Build up a fake connection instance for use with the prepared statement
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        Mockito.when(conn.getConnectionProps()).thenReturn(new Properties());
        DQP dqp = Mockito.mock(DQP.class);
        ServerConnection serverConn = Mockito.mock(ServerConnection.class);
        LogonResult logonResult = Mockito.mock(LogonResult.class);

        // stub methods
        Mockito.when(conn.getServerConnection()).thenReturn(serverConn);
        Mockito.when(serverConn.getLogonResult()).thenReturn(logonResult);
        Mockito.when(logonResult.getTimeZone()).thenReturn(TimeZone.getDefault());

        // a dummy result message that is specific to this test case
        final ResultsFuture<ResultsMessage> results = new ResultsFuture<>();
        final int[] count = new int[1];
        final ResultsMessage rm = new ResultsMessage();
        Mockito.when(dqp.executeRequest(ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    RequestMessage requestMessage = (RequestMessage) invocation.getArguments()[1];
                    count[0] += requestMessage.getParameterValues().size();
                    if (count[0] == 100000) {
                        rm.setException(new KublingException());
                        rm.setResults(new List<?>[]{List.of(Statement.EXECUTE_FAILED)});
                    } else {
                        List<?>[] vals = new List<?>[requestMessage.getParameterValues().size()];
                        Arrays.fill(vals, List.of(0));
                        rm.setResults(Arrays.asList(vals));
                    }
                    return results;
                });
        rm.setUpdateResult(true);
        results.getResultsReceiver().receiveResults(rm);
        Mockito.when(conn.getDQP()).thenReturn(dqp);

        // some update SQL
        String sqlCommand = "delete from table where col=?";
        TestableMMPreparedStatement statement = (TestableMMPreparedStatement) getMMPreparedStatement(conn, sqlCommand);

        ArrayList<ArrayList<Object>> expectedParameterValues = new ArrayList<>(3);
        // Add some batches and their parameter values
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(1)));
        statement.setInt(1, 1);
        statement.addBatch();
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(2)));
        statement.setInt(1, 2);
        statement.addBatch();
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(3)));
        statement.setInt(1, 3);
        statement.addBatch();

        // execute the batch and verify that it matches our dummy results
        // message set earlier
        assertArrayEquals(new int[]{0, 0, 0}, statement.executeBatch());

        // Now verify the statement's RequestMessage is what we expect
        assertEquals(sqlCommand, statement.requestMessage.getCommandString());
        assertEquals(expectedParameterValues, statement.requestMessage.getParameterValues());
        assertTrue(statement.requestMessage.isBatchedUpdate());
        assertFalse(statement.requestMessage.isCallableStatement());
        assertTrue(statement.requestMessage.isPreparedStatement());

        count[0] = 0;
        //large batch handling - should split into 5
        for (int i = 0; i < 100000; i++) {
            statement.setInt(1, 1);
            statement.addBatch();
        }
        try {
            statement.executeBatch();
            fail();
        } catch (BatchUpdateException e) {
            assertEquals(100000, count[0]);
            assertEquals(95309, e.getUpdateCounts().length);
            assertEquals(Statement.EXECUTE_FAILED, e.getUpdateCounts()[95308]);
        }
    }

    /**
     * Verify that the <code>clearBatch()</code> method of
     * <code>MMPreparedStatement</code> is clearing the list of batched
     * commands.
     * <p>
     * This is done by first adding command parameter values to the batch and
     * then invoking the <code>clearBatch()</code> method.
     *
     */
    @Test
    public void testClearBatch() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("delete from table where col=?");
        // Add some stuff
        statement.setInt(1, 1);
        statement.addBatch();
        statement.setInt(1, 2);
        statement.addBatch();
        // Make sure something is really there
        assertFalse(statement.getParameterValuesList().isEmpty());
        // Now clear it
        statement.clearBatch();
        assertEquals(0, statement.getParameterValuesList().size());
    }

    /**
     * Adds additional batches of command parameter values to a prepared
     * statement after a previous list has been cleared.
     * <p>
     * This is done by first adding command parameter values to the batch and
     * then invoking the <code>clearBatch()</code> method.  Then a different
     * set of command parameter values are added to the existing batch command.
     * <p>
     * The expected result is the command parameter list for the batches will
     * only reflect what was added after <code>clearBatch()</code> was invoked.
     *
     */
    @Test
    public void testClearBatchAddBatch() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("delete from table where col=?");

        statement.setInt(1, 1);
        statement.addBatch();
        statement.setInt(1, 2);
        statement.addBatch();
        // Make sure something is really there
        assertFalse(statement.getParameterValuesList().isEmpty());
        // Now clear it
        statement.clearBatch();
        // Make sure it is empty now
        assertEquals(0, statement.getParameterValuesList().size());

        ArrayList<ArrayList<Object>> expectedParameterValues = new ArrayList<>(1);

        // Now add something for validation
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(5)));
        statement.setInt(1, 5);
        statement.addBatch();
        assertEquals(expectedParameterValues, statement.getParameterValuesList());
    }

    /**
     * Test the <code>addBatch()</code> method of <code>MMPreparedStatement</code>
     * to verify that the command parameter values of the batch are added to the
     * command parameter values list.
     *
     */
    @Test
    public void testAddBatch() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("delete from table where col=?");

        ArrayList<ArrayList<Object>> expectedParameterValues = new ArrayList<>(1);

        // First we add a single batch
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(1)));
        statement.setInt(1, 1);
        statement.addBatch();
        assertEquals(expectedParameterValues, statement.getParameterValuesList());

        // Now add some more batches just for sanity’s sake
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(3)));
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(5)));
        statement.setInt(1, 3);
        statement.addBatch();
        statement.setInt(1, 5);
        statement.addBatch();
        assertEquals(expectedParameterValues, statement.getParameterValuesList());

        assertEquals(List.of(5), statement.getParameterValues());
    }

    @Test
    public void testSetBlob() throws Exception {
        PreparedStatementImpl stmt = getMMPreparedStatement("delete from table where col=?");
        stmt.setBlob(1, (Blob) null);
    }

    @Test
    public void testShowParameterMetadata() throws Exception {
        PreparedStatementImpl stmt = getMMPreparedStatement("show plan");
        assertEquals(0, stmt.getParameterMetaData().getParameterCount());
    }

    /**
     * Test the <code>addBatch()</code> method of <code>MMPreparedStatement</code>
     * using a batch with an empty parameter value list.  The test will verify
     * no failures occur when there are no command parameter values defined
     * when the <code>addBatch()</code> method is invoked.
     * <p>
     * It is valid to add an empty parameter value list to a batch list.
     * <p>
     * For example:
     * <p>
     * <code>PreparedStatement stmt = conn.prepareStatement(sql);<br \>
     * stmt.addBatch();<br \>
     * stmt.addBatch();<br \>
     * stmt.executeBatch();</code>
     *
     */
    @Test
    public void testAddBatchNoParameterValues() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("delete from table where col=?");

        // This will hold our expected values list
        ArrayList<ArrayList<Object>> expectedParameterValues = new ArrayList<>(1);

        // First batch has an empty parameter value list
        expectedParameterValues.add(new ArrayList<>(Collections.emptyList()));

        // No values have been set  so we are adding a batch with an empty
        // parameter value list
        statement.addBatch();

        // Second batch contains a parameter value list
        expectedParameterValues.add(new ArrayList<>(Collections.singletonList(1)));

        // We now are adding a batch that does have parameter values
        statement.setInt(1, 1);
        statement.addBatch();

        // Check to see if our statement contains our expected parameter value list
        assertEquals(expectedParameterValues, statement.getParameterValuesList());
    }

    @Test
    public void testTypedNullWithoutSqlContext() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("SELECT ?");

        statement.setNull(1, Types.INTEGER);

        RequestMessage request = statement.createRequestMessage(
                new String[]{"SELECT ?"}, false, RequestMessage.ResultsMode.EITHER);
        assertEquals(Collections.singletonList(null), request.getParameterValues());
        assertEquals(List.of(new ParameterType(Types.INTEGER,
                DataTypeManager.DefaultDataTypes.INTEGER)), request.getParameterTypes());
    }

    @Test
    public void testSetObjectTypedAndUntypedNull() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("SELECT ?");

        statement.setObject(1, null, Types.INTEGER);
        assertEquals(List.of(new ParameterType(Types.INTEGER,
                DataTypeManager.DefaultDataTypes.INTEGER)), statement.getParameterTypes());

        statement.setObject(1, null, Types.INTEGER, 0);
        assertEquals(List.of(new ParameterType(Types.INTEGER,
                DataTypeManager.DefaultDataTypes.INTEGER)), statement.getParameterTypes());

        statement.setObject(1, null, JDBCType.INTEGER);
        assertEquals(List.of(new ParameterType(Types.INTEGER,
                DataTypeManager.DefaultDataTypes.INTEGER)), statement.getParameterTypes());

        statement.setObject(1, null, new SQLType() {
            @Override
            public String getName() {
                return DataTypeManager.DefaultDataTypes.GEOGRAPHY;
            }

            @Override
            public String getVendor() {
                return "Kubling";
            }

            @Override
            public Integer getVendorTypeNumber() {
                return Types.OTHER;
            }
        });
        assertEquals(List.of(new ParameterType(Types.OTHER,
                DataTypeManager.DefaultDataTypes.GEOGRAPHY)), statement.getParameterTypes());

        statement.setObject(1, null);
        RequestMessage request = statement.createRequestMessage(
                new String[]{"SELECT ?"}, false, RequestMessage.ResultsMode.EITHER);
        assertEquals(Collections.singletonList(null), request.getParameterValues());
        assertTrue(request.getParameterTypes().isEmpty());
    }

    @Test
    public void testTypedNullBatchMetadataIsIndependent() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("SELECT ?, ?, ?");

        statement.setNull(1, Types.INTEGER);
        statement.setNull(2, Types.ARRAY, "INTEGER[]");
        statement.setNull(3, Types.OTHER, "geography");
        statement.addBatch();

        statement.setNull(1, Types.VARCHAR);
        statement.setObject(2, null);
        statement.setNull(3, Types.OTHER, "geometry");
        statement.addBatch();

        RequestMessage request = statement.createRequestMessage(
                new String[]{"SELECT ?, ?, ?"}, true, RequestMessage.ResultsMode.EITHER);
        assertEquals(2, request.getParameterValues().size());
        assertEquals(List.of(
                        new ParameterType(Types.INTEGER, DataTypeManager.DefaultDataTypes.INTEGER),
                        new ParameterType(Types.ARRAY, "integer[]"),
                        new ParameterType(Types.OTHER, DataTypeManager.DefaultDataTypes.GEOGRAPHY)),
                request.getBatchedParameterTypes().get(0));
        assertEquals(Arrays.asList(
                        new ParameterType(Types.VARCHAR, DataTypeManager.DefaultDataTypes.STRING),
                        null,
                        new ParameterType(Types.OTHER, DataTypeManager.DefaultDataTypes.GEOMETRY)),
                request.getBatchedParameterTypes().get(1));
    }

    @Test
    public void testTypedNullValidation() throws Exception {
        PreparedStatementImpl statement = getMMPreparedStatement("SELECT ?");

        assertThrows(SQLException.class, () -> statement.setNull(1, Integer.MIN_VALUE));
        assertThrows(SQLException.class, () -> statement.setNull(1, Types.OTHER));
        assertThrows(SQLException.class, () -> statement.setNull(1, Types.OTHER, "not-a-type"));
        assertThrows(SQLException.class, () -> statement.setNull(1, Types.ARRAY, "integer"));
        assertThrows(SQLException.class, () -> statement.setNull(1, Types.INTEGER, "geography"));
        assertThrows(SQLException.class, () -> statement.setObject(1, null, new SQLType() {
            @Override
            public String getName() {
                return "invalid";
            }

            @Override
            public String getVendor() {
                return "test";
            }

            @Override
            public Integer getVendorTypeNumber() {
                return null;
            }
        }));
        assertTrue(statement.getParameterValues().isEmpty());
    }

    /**
     * A helper method to get an <code>MMPreparedStatement</code> that can be
     * used for simple test cases.
     * <p>
     * The returned value is an instance of <code>TestableMMPreparedStatement</code>
     * <p>
     * This method invokes <code>getMMPreparedStatement(final MMConnection conn,
     * final String sql)</code> with a fake connection object constructed by
     * <code>Mockito</code>.
     *
     * @param sql the query for the prepared statement
     * @return an instance of TestableMMPreparedStatement
     */
    protected PreparedStatementImpl getMMPreparedStatement(final String sql) throws SQLException {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        ServerConnection serverConn = Mockito.mock(ServerConnection.class);
        LogonResult logonResult = Mockito.mock(LogonResult.class);

        Mockito.when(conn.getServerConnection()).thenReturn(serverConn);
        Mockito.when(serverConn.getLogonResult()).thenReturn(logonResult);
        Mockito.when(logonResult.getTimeZone()).thenReturn(TimeZone.getDefault());

        return getMMPreparedStatement(conn, sql);
    }

    /**
     * A helper method to get an <code>MMPreparedStatement</code> that can be
     * used for simple test cases.
     * <p>
     * The returned value is an instance of <code>TestableMMPreparedStatement</code>
     * <p>
     * <code>conn</code> should be a valid instance of <code>MMConnection</code>
     * or this method will fail.
     *
     * @param conn an instance of <code>MMConnection</code>
     * @param sql  the query for the prepared statement
     * @return an instance of TestableMMPreparedStatement
     */
    protected PreparedStatementImpl getMMPreparedStatement(final ConnectionImpl conn, final String sql)
            throws SQLException {
        TestableMMPreparedStatement statement =
                new TestableMMPreparedStatement(conn, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        // Make sure everything is empty on start
        assertEquals(0, statement.getParameterValuesList().size());
        assertEquals(0, statement.getParameterValues().size());

        return statement;
    }

    /**
     * Represents an extension to <code>MMPreparedStatement</code> that
     * gives access to the <code>RequestMessage</code> that is passed
     * around inside <code>MMPreparedStatement</code>.
     * <p>
     * This extension simply adds a field named <code>requestMessage</code>
     * which is <code>public</code>.  This field gets set when the <code>protected</code>
     * method <code>createRequestMessage()</code> is called.
     * <p>
     * This extension also overrides <code>RequestMessage createRequestMessage(String[] commands,
     * boolean isBatchedCommand, Boolean requiresResultSet)</code> so that
     * reference to the created <code>RequestMessage</code> can be retained in
     * the field <code>requestMessage</code>.
     */
    static class TestableMMPreparedStatement extends PreparedStatementImpl {
        /**
         * Contains a reference to the <code>RequestMessage</code> created by
         * a call to <code>createRequestMessage(String[] commands,
         * boolean isBatchedCommand, Boolean requiresResultSet)</code>.  This
         * will allow easy access to the prepared statement's request message
         * generated by a call to one of the statement's execute methods.
         */
        public RequestMessage requestMessage;

        @Override
        protected RequestMessage createRequestMessage(String[] commands,
                                                      boolean isBatchedCommand, RequestMessage.ResultsMode resultsMode) {
            this.requestMessage = super
                    .createRequestMessage(commands, isBatchedCommand, resultsMode);
            return this.requestMessage;
        }

        public TestableMMPreparedStatement(ConnectionImpl connection,
                                           String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            super(connection, sql, resultSetType, resultSetConcurrency);
        }

    }

}
