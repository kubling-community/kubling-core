/*
 * Copyright Kubling and/or its affiliates
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

package com.kubling.jdbc;

import com.kubling.client.DQP;
import com.kubling.client.ResultsMessage;
import com.kubling.client.util.ResultsFuture;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("nls")
public class TestMultipleResults {

    private static final long REQUEST_ID = 77;

    @Test
    public void testSingleResultCompatibility() throws Exception {
        DQP dqp = mock(DQP.class);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(resultSet(1, false, 10)));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("select 1"));
        assertEquals(-1, statement.getUpdateCount());
        assertEquals(1, readSingleValue(statement.getResultSet()));
        assertFalse(statement.getMoreResults());
        assertEquals(-1, statement.getUpdateCount());
        verify(dqp, times(1)).executeRequest(anyLong(), any());
        verify(dqp).executeRequest(anyLong(), argThat(request -> request.supportsMultipleResults()));
        verify(dqp, never()).processNextResultRequest(anyLong(), anyLong());

        ResultsMessage update = updateCount(4, false, 20);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(update));
        assertFalse(statement.execute("update t set x = 1"));
        assertEquals(4, statement.getUpdateCount());
        assertFalse(statement.getMoreResults());
        assertEquals(-1, statement.getUpdateCount());
    }

    @Test
    public void testMixedSequenceAndZeroUpdateCount() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage first = resultSet(11, true, 1);
        ResultsMessage zero = updateCount(0, true, 2);
        ResultsMessage count = updateCount(3, true, 3);
        ResultsMessage empty = emptyResultSet(false, 4);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(first));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(completed(zero));
        when(dqp.processNextResultRequest(REQUEST_ID, 2)).thenReturn(completed(count));
        when(dqp.processNextResultRequest(REQUEST_ID, 3)).thenReturn(completed(empty));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("call mixed_results()"));
        ResultSet firstResultSet = statement.getResultSet();
        assertEquals(11, readSingleValue(firstResultSet));

        assertFalse(statement.getMoreResults());
        assertTrue(firstResultSet.isClosed());
        assertEquals(0, statement.getUpdateCount());

        assertFalse(statement.getMoreResults());
        assertEquals(3, statement.getUpdateCount());

        assertTrue(statement.getMoreResults());
        ResultSet emptyResultSet = statement.getResultSet();
        assertEquals(1, emptyResultSet.getMetaData().getColumnCount());
        assertFalse(emptyResultSet.next());

        assertFalse(statement.getMoreResults());
        assertEquals(-1, statement.getUpdateCount());
        verify(dqp, times(1)).executeRequest(anyLong(), any());
        verify(dqp, times(3)).processNextResultRequest(eq(REQUEST_ID), anyLong());
    }

    @Test
    public void testUpdateThenConsecutiveResultSets() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage update = updateCount(2, true, 1);
        ResultsMessage first = resultSet(21, true, 2);
        ResultsMessage second = resultSet(22, false, 3);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(update));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(completed(first));
        when(dqp.processNextResultRequest(REQUEST_ID, 2)).thenReturn(completed(second));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertFalse(statement.execute("call update_then_rows()"));
        assertEquals(2, statement.getUpdateCount());
        assertTrue(statement.getMoreResults());
        assertEquals(21, readSingleValue(statement.getResultSet()));
        assertTrue(statement.getMoreResults());
        assertEquals(22, readSingleValue(statement.getResultSet()));
        assertFalse(statement.getMoreResults());
        assertEquals(-1, statement.getUpdateCount());
    }

    @Test
    public void testKeepAndCloseAllResults() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage first = resultSet(1, true, 1);
        ResultsMessage second = resultSet(2, true, 2);
        ResultsMessage third = resultSet(3, false, 3);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(first));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(completed(second));
        when(dqp.processNextResultRequest(REQUEST_ID, 2)).thenReturn(completed(third));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("call three_results()"));
        ResultSet retainedFirst = statement.getResultSet();
        assertTrue(statement.getMoreResults(Statement.KEEP_CURRENT_RESULT));
        ResultSet retainedSecond = statement.getResultSet();
        assertFalse(retainedFirst.isClosed());

        assertTrue(statement.getMoreResults(Statement.KEEP_CURRENT_RESULT));
        ResultSet current = statement.getResultSet();
        assertFalse(retainedFirst.isClosed());
        assertFalse(retainedSecond.isClosed());

        assertFalse(statement.getMoreResults(Statement.CLOSE_ALL_RESULTS));
        assertTrue(retainedFirst.isClosed());
        assertTrue(retainedSecond.isClosed());
        assertTrue(current.isClosed());
        verify(dqp).closeResultRequest(REQUEST_ID, 1);
        verify(dqp).closeResultRequest(REQUEST_ID, 2);
        verify(dqp).closeResultRequest(REQUEST_ID, 3);
    }

    @Test
    public void testStatementCloseClosesCurrentAndRetainedResults() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage first = resultSet(1, true, 1);
        ResultsMessage second = resultSet(2, false, 2);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(first));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(completed(second));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("call two_results()"));
        ResultSet retained = statement.getResultSet();
        assertTrue(statement.getMoreResults(Statement.KEEP_CURRENT_RESULT));
        ResultSet current = statement.getResultSet();

        statement.close();

        assertTrue(retained.isClosed());
        assertTrue(current.isClosed());
        assertTrue(statement.isClosed());
        verify(dqp, times(1)).closeRequest(REQUEST_ID);
        verify(dqp, never()).closeResultRequest(anyLong(), anyLong());
    }

    @Test
    public void testReexecutionDiscardsPreviousSequence() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage first = resultSet(1, true, 1);
        ResultsMessage replacement = updateCount(5, false, 2);
        when(dqp.executeRequest(anyLong(), any()))
                .thenReturn(completed(first), completed(replacement));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("call unfinished_results()"));
        ResultSet abandoned = statement.getResultSet();

        assertFalse(statement.execute("update t set x = 2"));

        assertTrue(abandoned.isClosed());
        assertEquals(5, statement.getUpdateCount());
        verify(dqp, times(2)).executeRequest(anyLong(), any());
        verify(dqp, never()).processNextResultRequest(anyLong(), anyLong());
    }

    @Test
    public void testErrorAfterFirstResultPreservesSqlDetails() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage first = resultSet(1, true, 1);
        ResultsMessage error = new ResultsMessage();
        error.setException(new SQLException("late failure", "42001", 991));
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(first));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(completed(error));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("call fails_late()"));
        SQLException exception = assertThrows(SQLException.class, statement::getMoreResults);

        assertEquals("42001", exception.getSQLState());
        assertEquals(991, exception.getErrorCode());
        assertNotNull(exception.getCause());
        verify(dqp, times(1)).executeRequest(anyLong(), any());
    }

    @Test
    public void testTransportErrorWhileAdvancingIsNotEndOfResults() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage first = resultSet(1, true, 1);
        ResultsFuture<ResultsMessage> failed = new ResultsFuture<>();
        failed.getResultsReceiver().exceptionOccurred(
                new SQLException("connection lost", "08006", 73));
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(first));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(failed);
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("call disconnected_results()"));
        SQLException exception = assertThrows(SQLException.class, statement::getMoreResults);

        assertEquals("08006", exception.getSQLState());
        assertEquals(73, exception.getErrorCode());
        assertNotNull(exception.getCause());
    }

    @Test
    public void testIncompleteContinuationIsRejected() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage first = resultSet(1, true, 1);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(first));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(completed(null));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertTrue(statement.execute("call incomplete_results()"));
        SQLException exception = assertThrows(SQLException.class, statement::getMoreResults);

        assertTrue(exception.getMessage().contains("incomplete result sequence"));
    }

    @Test
    public void testGeneratedKeysAreNotASequenceResult() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage updateWithKeys = resultSet(99, true, 1);
        updateWithKeys.setUpdateResult(true);
        updateWithKeys.setUpdateCount(2);
        ResultsMessage next = updateCount(7, false, 2);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(updateWithKeys));
        when(dqp.processNextResultRequest(REQUEST_ID, 1)).thenReturn(completed(next));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);

        assertFalse(statement.execute("insert into t values (1)", Statement.RETURN_GENERATED_KEYS));
        assertEquals(2, statement.getUpdateCount());
        try (ResultSet keys = statement.getGeneratedKeys()) {
            assertEquals(99, readSingleValue(keys));
        }

        assertFalse(statement.getMoreResults());
        assertEquals(7, statement.getUpdateCount());
        assertFalse(statement.getMoreResults());
        assertEquals(-1, statement.getUpdateCount());
    }

    @Test
    public void testResultSpecificCursorPaging() throws Exception {
        DQP dqp = mock(DQP.class);
        ResultsMessage firstPage = resultSet(1, false, 42);
        firstPage.setFinalRow(2);
        ResultsMessage secondPage = resultSet(2, false, 42);
        secondPage.setFirstRow(2);
        secondPage.setLastRow(2);
        secondPage.setFinalRow(2);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(firstPage));
        when(dqp.processCursorRequest(REQUEST_ID, 42, 2, 2048))
                .thenReturn(completed(secondPage));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_SCROLL_INSENSITIVE);

        assertTrue(statement.execute("select two_rows"));
        ResultSet resultSet = statement.getResultSet();
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getInt(1));
        assertTrue(resultSet.next());
        assertEquals(2, resultSet.getInt(1));
        assertFalse(resultSet.next());
        verify(dqp).processCursorRequest(REQUEST_ID, 42, 2, 2048);
        verify(dqp, never()).processCursorRequest(anyLong(), anyInt(), anyInt());
    }

    @Test
    public void testInvalidCloseModeIsRejected() throws Exception {
        DQP dqp = mock(DQP.class);
        when(dqp.executeRequest(anyLong(), any())).thenReturn(completed(updateCount(1, false, 1)));
        StatementImpl statement = statement(dqp, ResultSet.TYPE_FORWARD_ONLY);
        statement.execute("update t set x = 1");

        assertThrows(SQLException.class, () -> statement.getMoreResults(Integer.MIN_VALUE));
    }

    private static StatementImpl statement(DQP dqp, int resultSetType) throws Exception {
        ConnectionImpl connection = mock(ConnectionImpl.class);
        when(connection.getConnectionProps()).thenReturn(new Properties());
        when(connection.getExecutionProperties()).thenReturn(new Properties());
        when(connection.getDQP()).thenReturn(dqp);
        when(connection.nextRequestID()).thenReturn(REQUEST_ID);
        return new StatementImpl(connection, resultSetType, ResultSet.CONCUR_READ_ONLY) {
            @Override
            protected TimeZone getServerTimeZone() {
                return null;
            }
        };
    }

    private static ResultsMessage resultSet(int value, boolean more, long resultID) {
        ResultsMessage result = new ResultsMessage(
                List.of(List.of(value)), new String[]{"value"}, new String[]{"integer"});
        result.setFinalRow(1);
        result.setResultId(resultID);
        result.setHasMoreResults(more);
        return result;
    }

    private static ResultsMessage emptyResultSet(boolean more, long resultID) {
        ResultsMessage result = new ResultsMessage(
                Collections.emptyList(), new String[]{"value"}, new String[]{"integer"});
        result.setFinalRow(0);
        result.setResultId(resultID);
        result.setHasMoreResults(more);
        return result;
    }

    private static ResultsMessage updateCount(int count, boolean more, long resultID) {
        ResultsMessage result = new ResultsMessage(
                List.of(List.of(count)), new String[]{"Count"}, new String[]{"integer"});
        result.setFinalRow(1);
        result.setUpdateResult(true);
        result.setResultId(resultID);
        result.setHasMoreResults(more);
        return result;
    }

    private static int readSingleValue(ResultSet resultSet) throws SQLException {
        assertTrue(resultSet.next());
        int value = resultSet.getInt(1);
        assertFalse(resultSet.next());
        return value;
    }

    private static ResultsFuture<ResultsMessage> completed(ResultsMessage message) {
        ResultsFuture<ResultsMessage> future = new ResultsFuture<>();
        future.getResultsReceiver().receiveResults(message);
        return future;
    }
}
