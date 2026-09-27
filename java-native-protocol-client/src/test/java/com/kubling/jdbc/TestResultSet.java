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
import com.kubling.client.ResultsMessage;
import com.kubling.client.lob.LobChunk;
import com.kubling.client.util.ResultsFuture;
import com.kubling.core.KublingProcessingException;
import com.kubling.core.types.XMLType;
import org.junit.jupiter.api.Test;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("nls")
public class TestResultSet {

    private static final int BATCH_SIZE = 400;

    /**
     * test next() without walking through
     */
    @Test
    public void testNext1() throws SQLException {
        ResultSet cs = helpExecuteQuery();
        assertEquals(Integer.valueOf(0), Integer.valueOf(cs.getRow()));
        cs.close();
    }

    /**
     * test next() with walking through all the rows and compare records
     */
    @Test
    public void testNext2() throws SQLException {
        List<?>[] expected = TestAllResultsImpl.exampleResults1(1000);
        ResultSetImpl cs = helpExecuteQuery();

        int i = 0;
        while (cs.next()) {
            assertEquals(expected[i], cs.getCurrentRecord());
            assertEquals((i < 800 ? BATCH_SIZE : 200) - (i % BATCH_SIZE) - 1, cs.available());
            i++;
        }

        cs.close();
    }

    /**
     * test with LargeA -- only work with real model rather than fake metadata
     */

    // Note for all the following: processor batch size is 100,
    // so some of these tests check what happens when the client
    // fetch size is above, the same, or below it
    public static final int PROC_BATCH_SIZE = 100;

    /**
     * Test stability when next() is called beyond the rowcount.
     */
    @Test
    public void testNextBeyondEnd_fetchEqualsCount() throws Exception {
        helpTestNextBeyondResultSet(1000, 1000);
    }

    /**
     * Test stability when next() is called beyond the rowcount.
     */
    @Test
    public void testNextBeyondEnd_fetchLessThanCount() throws Exception {
        helpTestNextBeyondResultSet(100, 1000);
    }

    /**
     * Test stability when next() is called beyond the rowcount with one more row.
     */
    @Test
    public void testNextBeyondEnd_fetchLessThanCount1() throws Exception {
        helpTestNextBeyondResultSet(100, 101);
    }

    /**
     * Test stability when next() is called beyond the rowcount.
     */
    @Test
    public void testNextBeyondEnd_fetchLessThanCountNonMultiple() throws Exception {
        helpTestNextBeyondResultSet(120, 1000);
    }

    /**
     * Test stability when next() is called beyond the rowcount.
     */
    @Test
    public void testNextBeyondEnd_fetchGreaterThanCount() throws Exception {
        helpTestNextBeyondResultSet(300, PROC_BATCH_SIZE);
    }

    /**
     * Test stability when next() is called beyond the rowcount.
     */
    @Test
    public void testNextBeyondEnd_fetchGreaterThanCountNonMultiple() throws Exception {
        helpTestNextBeyondResultSet(310, PROC_BATCH_SIZE - 50);
    }

    /**
     * Test stability when next() is called beyond the rowcount.
     */
    @Test
    public void testNextBeyondEnd_fetchGreaterThanCountNonMultiple2() throws Exception {
        helpTestNextBeyondResultSet(300, PROC_BATCH_SIZE + 10);
    }

    /**
     * Test that the returned results walks through all results if
     * fetchSize &lt; rows &lt; proc batch size.
     * Test for defect 11356
     */
    @Test
    public void testNextBeyondEnd_fetchLessThanCount_ResultsBetweenFetchAndProcBatch() throws Exception {
        helpTestNextBeyondResultSet(30, PROC_BATCH_SIZE - 25);
    }

    public void helpTestNextBeyondResultSet(int fetchSize, int numRows) throws Exception {
        try (ResultSet cs = helpExecuteQuery(fetchSize, numRows, ResultSet.TYPE_SCROLL_INSENSITIVE)) {
            Object lastRowValue;
            for (int rowNum = 1; rowNum <= numRows; rowNum++) {
                assertTrue(cs.next(), "Should return true before end cs.next()");
            }

            lastRowValue = cs.getObject(1);

            // Should just return false and leave cursor where it is
            for (int i = numRows + 1; i < numRows + 4; i++) {
                assertFalse(cs.next(), "Should return false when going past the end: " + i);
                assertTrue(cs.isAfterLast(), "Is after last should be true: " + i);
            }

            // Should still be just after last row
            cs.previous();
            assertTrue(cs.isLast(), "Is last should be true");
            assertEquals(lastRowValue, cs.getObject(1), "Not on last row");

        }
    }

    /**
     * test both next() and previous() -- when result set scroll in bidirection
     */
    @Test
    public void testBidirection() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        assertNotNull(cs);
        cs.absolute(290);
        assertEquals(290, cs.getCurrentRecord().getFirst());
        cs.next();
        assertEquals(291, cs.getCurrentRecord().getFirst());
        cs.next();
        assertEquals(292, cs.getCurrentRecord().getFirst());
        cs.previous();
        assertEquals(291, cs.getCurrentRecord().getFirst());
        cs.next();
        assertEquals(292, cs.getCurrentRecord().getFirst());
        cs.close();
    }

    /**
     * test hasNext() without walking through any row
     */
    @Test
    public void testHasNext1() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        assertTrue(cs.hasNext());
        cs.close();
    }

    /**
     * test hasNext() with blocking for the Next batch -- triggering point
     */
    @Test
    public void testHasNext2() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(100);
        assertTrue(cs.hasNext());
        cs.close();
    }

    /**
     * test hasNext() with nextBatch!=null -- short response
     */
    @Test
    public void testHasNext3() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        int i = 0;
        while (cs.next()) {
            if (i == 289) {
                break;
            }
            i++;
        }
        assertTrue(cs.hasNext());
        cs.close();
    }

    /**
     * at the end of all batches
     */
    @Test
    public void testHasNext4() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(1000);
        assertFalse(cs.hasNext());
        cs.close();
    }

    /**
     * walk all way through from the end back to first row
     */
    @Test
    public void testPrevious1() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        List<?>[] expected = TestAllResultsImpl.exampleResults1(1000);
        while (cs.next()) {
            //System.out.println(" rs.next == " + cs.getCurrentRecord());
        }
        // cursor is after the last row. getRow() should return 0 when not on a valid row
        assertEquals(0, cs.getRow());

        int i = 1000;
        while (cs.previous()) {
            //System.out.println(" rs.previous == " + cs.getCurrentRecord());
            assertEquals(expected[i - 1], cs.getCurrentRecord());
            i--;
        }
        assertEquals(0, cs.getRow());
        cs.close();
    }

    /**
     * test the previous in the middle of a batch
     */
    @Test
    public void testPrevious2() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(290);

        // cursor is at the row of 289 now
        assertTrue(cs.previous());
        assertEquals(289, cs.getRow());
        cs.close();
    }

    /**
     * walk all way through from the end back to first row
     */
    @Test
    public void testPrevious3() throws Exception {
        //large batch size
        ResultSetImpl cs = helpExecuteQuery(600, 10000, ResultSet.TYPE_SCROLL_INSENSITIVE);
        List<?>[] expected = TestAllResultsImpl.exampleResults1(10000);
        while (cs.next()) {
        }
        // cursor is after the last row. getRow() should return 0 when not on a valid row
        assertEquals(0, cs.getRow());

        int i = 10000;
        while (cs.previous()) {
            //System.out.println(" rs.previous == " + cs.getCurrentRecord());
            assertEquals(expected[i - 1], cs.getCurrentRecord());
            i--;
        }
        assertEquals(0, cs.getRow());
        cs.close();
    }

    /**
     * walk all way through from the end back to first row
     */
    @Test
    public void testPrevious4() throws Exception {
        //small batch size
        ResultSetImpl cs = helpExecuteQuery(50, 1000, ResultSet.TYPE_SCROLL_INSENSITIVE);
        List<?>[] expected = TestAllResultsImpl.exampleResults1(1000);
        while (cs.next()) {
            //System.out.println(" rs.next == " + cs.getCurrentRecord());
        }
        // cursor is after the last row. getRow() should return 0 when not on a valid row
        assertEquals(0, cs.getRow());

        int i = 1000;
        while (cs.previous()) {
            //System.out.println(" rs.previous == " + cs.getCurrentRecord());
            assertEquals(expected[i - 1], cs.getCurrentRecord());
            i--;
        }
        assertEquals(0, cs.getRow());
        cs.close();
    }

    /**
     * test rare case that cursor change direction
     */
    @Test
    public void testChangeDirection() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(291);
        cs.previous();

        assertEquals(290, cs.getRow());
        cs.close();
    }

    @Test
    public void testIsFirst() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.next();
        assertTrue(cs.isFirst());
        cs.close();
    }

    /**
     * test cursor is in the middle of all batches
     */
    @Test
    public void testIsLast1() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.next();
        assertFalse(cs.isLast());
        cs.close();
    }

    /**
     * test cursor at the triggering point -- blocking case
     */
    @Test
    public void testIsLast2() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        int i = 0;
        while (cs.next()) {
            if (i == 99) {
                break;
            }
            i++;
        }

        assertFalse(cs.isLast());
        cs.close();
    }

    /**
     * test cursor at the last row of all batches
     */
    @Test
    public void testIsLast3() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(1000);
        assertTrue(cs.isLast());
        cs.close();
    }

    @Test
    public void testIsBeforeFirst() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        assertTrue(cs.isBeforeFirst());
        cs.close();
    }

    @Test
    public void testBeforeFirst() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to row 1
        cs.next();
        assertEquals(1, cs.getRow());

        // move back to before first row
        cs.beforeFirst();
        assertTrue(cs.isBeforeFirst());
        cs.close();
    }

    @Test
    public void testFirst() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to row #2
        cs.next();
        cs.next();
        assertEquals(2, cs.getRow());

        // move back to the 1st row
        cs.first();
        assertEquals(1, cs.getRow());
        cs.close();
    }

    @Test
    public void testAfterLast() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.afterLast();
        assertTrue(cs.isAfterLast());
        cs.close();
    }

    /**
     * right after the last row
     */
    @Test
    public void testIsAfterLast1() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(1000);
        cs.next();
        assertTrue(cs.isAfterLast());
        cs.close();
    }

    /**
     * right before the first
     */
    @Test
    public void testIsAfterLast2() throws Exception {
        ResultSetImpl cs = helpExecuteQuery();
        assertFalse(cs.isAfterLast());
        cs.close();
    }

    /**
     * absolute with cursor movement backward in the same batch -- absolute(positive)
     */
    @Test
    public void testAbsolute1() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to row #2
        cs.next();
        cs.next();
        assertEquals(2, cs.getRow());

        // move back to the 1st row
        cs.absolute(1);
        assertEquals(1, cs.getRow());
        cs.close();
    }

    /**
     * absolute with cursor movement forward in the same batch -- absolute(positive)
     */
    @Test
    public void testAbsolute2() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to row #2
        cs.next();
        cs.next();
        assertEquals(2, cs.getRow());

        // move back to the 1st row
        cs.absolute(3);
        assertEquals(3, cs.getRow());
        cs.close();
    }

    /**
     * absolute with cursor movement forward -- absolute(positive) -- blocking
     */
    @Test
    public void testAbsolute3() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to row #2
        cs.next();
        cs.next();
        assertEquals(2, cs.getRow());

        // move to row #100 -- blocking
        cs.absolute(100);
        assertEquals(100, cs.getRow());
        cs.close();
    }

    /**
     * absolute with cursor movement forward -- absolute(positive) -- triggering point
     */
    @Test
    public void testAbsolute4() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to row #2
        cs.next();
        cs.next();
        assertEquals(2, cs.getRow());

        // move to row #200 -- new batch
        cs.absolute(200);
        assertEquals(200, cs.getRow());
        cs.close();
    }

    /**
     * absolute with cursor movement back in the same batch -- absolute(negative)
     */
    @Test
    public void testAbsolute5() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to row #2
        cs.next();
        cs.next();
        assertEquals(2, cs.getRow());

        // move back to the 1st row
        cs.absolute(-1);
        assertEquals(1000, cs.getRow());
        cs.close();
    }

    /**
     * absolute after last row
     */
    @Test
    public void testAbsolute6() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(1005);
        // Cursor should be after last row. getRow() should return 0 because
        // cursor is not on a valid row
        assertEquals(0, cs.getRow());
        cs.close();
    }

    /**
     * relative(positive) -- forward to another batch
     */
    @Test
    public void testRelative1() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to the row #3
        cs.absolute(3);
        assertEquals(3, cs.getRow());

        // move to the row #140
        cs.relative(137);
        assertEquals(140, cs.getRow());
        cs.close();
    }

    /**
     * relative(negative) -- backward to another batch
     */
    @Test
    public void testRelative2() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to the row #137
        cs.absolute(137);
        assertEquals(137, cs.getRow());

        // move to the row #4
        cs.relative(-133);
        assertEquals(4, cs.getRow());
        cs.close();
    }

    /**
     * relative(negative) -- backward to triggering point or blocking batch
     */
    @Test
    public void testRelative3() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to the row #137
        cs.absolute(137);
        assertEquals(137, cs.getRow());

        // move to the row #100
        cs.relative(-37);
        assertEquals(100, cs.getRow());
        cs.close();
    }

    /**
     * relative(negative) -- backward to triggering point or blocking batch
     */
    @Test
    public void testRelative4() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // move to the row #237 in the third batch, so that the fourth batch has been requested when we switch direction
        cs.absolute(237);
        assertEquals(237, cs.getRow());

        // move to the row #37
        cs.relative(-200);
        assertEquals(37, cs.getRow());
        cs.close();
    }

    /**
     * in the first fetched batch
     */
    @Test
    public void testGetRow1() throws SQLException {
        ResultSet cs = helpExecuteQuery();

        int i = 0;
        while (cs.next()) {
            if (i == 102) {
                break;
            }
            i++;
        }

        assertEquals(i + 1, cs.getRow());
        cs.close();
    }

    /**
     * in the first batch
     */
    @Test
    public void testGetRow2() throws SQLException {
        ResultSet cs = helpExecuteQuery();

        cs.next();
        assertEquals(1, cs.getRow());
        cs.close();
    }

    /**
     * in the triggering point -- blocking
     */
    @Test
    public void testGetRow3() throws SQLException {
        ResultSet cs = helpExecuteQuery();
        int i = 0;
        while (cs.next()) {
            if (i == 99) {
                break;
            }
            i++;
        }
        assertEquals(100, cs.getRow());
        cs.close();
    }

    @Test
    public void testGetCurrentRecord() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        cs.absolute(103);
        assertEquals(103, cs.getCurrentRecord().getFirst());
        cs.close();
    }

    /**
     * test close() without walking through any of the record
     */
    @Test
    public void testClose() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        assertEquals(Integer.valueOf(0), Integer.valueOf(cs.getRow()));
        cs.close();
    }

    /**
     * test basic results-related metadata
     */
    @Test
    public void testGetMetaData() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();

        // check result set metadata
        // expected column info.
        List<String> columnName = getBQTRSMetaData1a();
        List<Integer> columnType = getBQTRSMetaData1b();
        List<String> columnTypeName = getBQTRSMetaData1c();

        ResultSetMetaData rm = cs.getMetaData();
        assertNotNull(rm);

        for (int j = 1; j <= rm.getColumnCount(); j++) {
            assertEquals(columnName.get(j - 1), rm.getColumnLabel(j));
            assertEquals(columnType.get(j - 1), Integer.valueOf(rm.getColumnType(j)));
            assertEquals(columnTypeName.get(j - 1), rm.getColumnTypeName(j));
        }

        cs.close();
    }

    @Test
    public void testFindColumn() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        ResultSetMetaData rm = cs.getMetaData();
        assertNotNull(rm);
        //assertEquals(1, cs.findColumn("BQT1.MediumA.IntKey"));
        assertEquals(1, cs.findColumn("IntKey"));
        cs.close();
    }

    @Test
    public void testFindNonExistentColumn() throws SQLException {
        ResultSet rs = helpExecuteQuery();
        rs.next();
        try {
            rs.findColumn("BOGUS");
        } catch (SQLException e) {
        }

        try {
            rs.getObject("BOGUS");
        } catch (SQLException e) {
        }
        rs.close();
    }

    @Test
    public void testGetStatement() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        assertNotNull(cs.getStatement());
        cs.close();
    }

    @Test
    public void testGetPlanDescription() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        assertNotNull(cs);

        assertNull((cs.getStatement()).getPlanDescription());
        cs.close();
    }

    /**
     * getObject(String)
     */
    @Test
    public void testGetObject2() throws SQLException {
        ResultSet cs = helpExecuteQuery();

        // move to the 1st row
        cs.next();
        assertEquals(1, cs.getObject("IntKey"));
        cs.close();
    }

    @Test
    public void testGetWarnings() throws SQLException {
        ResultSet cs = helpExecuteQuery();
        assertNull(cs.getWarnings());
        cs.close();
    }

    @Test
    public void testGetCursorName() throws SQLException {
        ResultSetImpl cs = helpExecuteQuery();
        assertNull(cs.getCursorName());
        cs.close();
    }

    @Test
    public void testAllGetters() throws SQLException {
        try (ResultSetImpl cs = helpExecuteQuery()) {
            cs.next();
            assertEquals(1, cs.getInt("IntKey"));
            assertEquals("1", cs.getString("IntKey"));

            assertEquals(Float.valueOf(1), Float.valueOf(cs.getFloat("IntKey")));
            assertEquals(1, cs.getLong("IntKey"));
            assertEquals(Double.valueOf(1), Double.valueOf(cs.getDouble("IntKey")));
            assertEquals((byte) 1, cs.getByte("IntKey"));
        }
    }

    /**
     * test wasNull() for ResultSet, this result actually is not a cursor result, but AllResults here.
     */
    @Test
    public void testWasNull() throws SQLException {
        try (ResultSet cs = helpExecuteQuery()) {
            cs.next();
            assertNotNull(cs.getObject("IntKey"));
            assertFalse(cs.wasNull());
        }
    }

    @Test
    public void testForwardOnly() throws Exception {
        ResultSetImpl cs = helpExecuteQuery(400, 1300, ResultSet.TYPE_FORWARD_ONLY);
        int i = 0;
        while (cs.next()) {
            i++;
            if (i <= 1200) {
                assertNotNull(cs.getPrefetch());
            } else {
                assertNull(cs.getPrefetch());
            }
            cs.getObject(1);
        }

        assertTrue(cs.isAfterLast());
        cs.close();
    }

    @Test
    public void testForwardOnlyPrefetchSmallFetchSize() throws Exception {
        StatementImpl statement = createMockStatement(ResultSet.TYPE_FORWARD_ONLY);
        ResultSetImpl cs = TestAllResultsImpl.helpTestBatching(statement, 10, 128, 256, true);
        for (int i = 0; i < 256; i++) {
            cs.next();
            cs.getObject(1);
        }
        Mockito.verify(statement.getDQP(), Mockito.atMost(1)).processCursorRequest(TestAllResultsImpl.REQUEST_ID, 11, 10);
        assertFalse(cs.next());
        assertTrue(cs.isAfterLast());
        cs.close();
    }

    @Test
    public void testOutputParameter() throws Exception {
        StatementImpl statement = createMockStatement(ResultSet.TYPE_FORWARD_ONLY);
        ResultsMessage resultsMsg = new ResultsMessage();
        resultsMsg.setResults(new List<?>[]{Arrays.asList(1, null, null), Arrays.asList(null, 2, 3)});
        resultsMsg.setLastRow(2);
        resultsMsg.setFirstRow(1);
        resultsMsg.setFinalRow(2);
        resultsMsg.setColumnNames(new String[]{"x", "out1", "out2"});
        resultsMsg.setDataTypes(new String[]{"integer", "integer", "integer"});
        ResultSetImpl cs = new ResultSetImpl(resultsMsg, statement, null, 2);

        int count = 0;
        while (cs.next()) {
            cs.getObject(1);
            count++;
        }
        assertEquals(1, count);
        assertTrue(cs.isAfterLast());
        assertEquals(2, cs.getOutputParamValue(2));
        assertEquals(3, cs.getOutputParamValue(3));
    }

    @Test
    public void testXML() throws Exception {
        StatementImpl statement = createMockStatement(ResultSet.TYPE_FORWARD_ONLY);
        ResultsFuture<LobChunk> future = new ResultsFuture<>();
        future.getResultsReceiver().receiveResults(new LobChunk("<a/>".getBytes(StandardCharsets.UTF_8), true));
        XMLType result = new XMLType();
        Mockito.when(statement.getDQP().requestNextLobChunk(0, 0, result.getReferenceStreamId()))
                .thenReturn(future);
        ResultsMessage resultsMsg = new ResultsMessage();
        result.setEncoding("UTF-8");
        resultsMsg.setResults(new List<?>[]{List.of(result)});
        resultsMsg.setLastRow(1);
        resultsMsg.setFirstRow(1);
        resultsMsg.setFinalRow(1);
        resultsMsg.setColumnNames(new String[]{"x"});
        resultsMsg.setDataTypes(new String[]{"xml"});
        ResultSetImpl cs = new ResultSetImpl(resultsMsg, statement);
        cs.next();
        assertEquals("<a/>", cs.getString(1));
    }

    /// //////////////////// Helper Method ///////////////////

    private ResultSetImpl helpExecuteQuery() {
        try {
            return helpExecuteQuery(BATCH_SIZE, 1000, ResultSet.TYPE_SCROLL_INSENSITIVE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResultSetImpl helpExecuteQuery(int fetchSize, int totalResults, int cursorType) throws SQLException, KublingProcessingException {
        StatementImpl statement = createMockStatement(cursorType, withSettings().stubOnly());
        return TestAllResultsImpl.helpTestBatching(statement, fetchSize, Math.min(fetchSize, totalResults), totalResults);
    }

    static StatementImpl createMockStatement(int cursorType) throws SQLException {
        return createMockStatement(cursorType, withSettings());
    }

    static StatementImpl createMockStatement(int cursorType, MockSettings mockSetting) throws SQLException {
        StatementImpl statement = Mockito.mock(StatementImpl.class, mockSetting);
        DQP dqp = mock(DQP.class);
        when(statement.getDQP()).thenReturn(dqp);
        when(statement.getResultSetType()).thenReturn(cursorType);
        TimeZone tz = TimeZone.getTimeZone("GMT-06:00");
        TimeZone serverTz = TimeZone.getTimeZone("GMT-05:00");
        when(statement.getDefaultCalendar()).thenReturn(Calendar.getInstance(tz));
        when(statement.getServerTimeZone()).thenReturn(serverTz);
        return statement;
    }

    ////////////////////////Expected Results////////////////
    /**
     * column name
     */
    private List<String> getBQTRSMetaData1a() {
        List<String> results = new ArrayList<>();
        results.add("IntKey");
        return results;
    }

    /**
     * column type
     */
    private List<Integer> getBQTRSMetaData1b() {
        List<Integer> results = new ArrayList<>();
        results.add(Types.INTEGER);
        return results;
    }

    /**
     * column type name
     */
    private List<String> getBQTRSMetaData1c() {
        List<String> results = new ArrayList<>();
        results.add("integer");
        return results;
    }
}
