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
import com.kubling.client.ResultsMessage;
import com.kubling.client.metadata.ParameterInfo;
import com.kubling.client.security.LogonResult;
import com.kubling.core.types.DataTypeManager;
import com.kubling.net.ServerConnection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestCallableStatement {

    @Test
    public void testWasNull() throws Exception {
        CallableStatementImpl mmcs = getCallableStatement();

        Map<Integer, Integer> params = new HashMap<>();
        mmcs.outParamIndexMap = params;
        params.put(1, 1);
        params.put(2, 2);
        ResultSetImpl rs = Mockito.mock(ResultSetImpl.class);
        mmcs.resultSet = rs;
        Mockito.when(rs.getOutputParamValue(1)).thenReturn(null);
        Mockito.when(rs.getOutputParamValue(2)).thenReturn(Boolean.TRUE);
        mmcs.getBoolean(1);
        assertTrue(mmcs.wasNull());
        assertTrue(mmcs.getBoolean(2));
        assertFalse(mmcs.wasNull());
    }

    @Test
    public void testGetOutputParameter() throws Exception {
        CallableStatementImpl mmcs = getCallableStatement();

        RequestMessage request = new RequestMessage();
        request.setExecutionId(1);
        ResultsMessage resultsMsg = new ResultsMessage();
        List<?>[] results = new List[]{Arrays.asList(null, null, null), Arrays.asList(null, 1, 2)};
        resultsMsg.setResults(results);
        resultsMsg.setColumnNames(new String[]{"IntNum", "Out1", "Out2"});
        resultsMsg.setDataTypes(new String[]{DataTypeManager.DefaultDataTypes.INTEGER, DataTypeManager.DefaultDataTypes.INTEGER, DataTypeManager.DefaultDataTypes.INTEGER});
        resultsMsg.setFinalRow(results.length);
        resultsMsg.setLastRow(results.length);
        resultsMsg.setFirstRow(1);
        resultsMsg.setParameters(Arrays.asList(
                new ParameterInfo(ParameterInfo.RESULT_SET, 1),
                new ParameterInfo(ParameterInfo.OUT, 1),
                new ParameterInfo(ParameterInfo.OUT, 1)));
        mmcs.createResultSet(resultsMsg);
        assertEquals(1, mmcs.getInt(1));
        assertEquals(2, mmcs.getInt(2));
        assertEquals(1, mmcs.getInt("Out1"));
        assertEquals(2, mmcs.getInt("Out2"));
    }

    @Test
    public void testUnknownIndex() throws Exception {
        CallableStatementImpl mmcs = getCallableStatement();

        mmcs.outParamIndexMap = new HashMap<>();

        try {
            mmcs.getBoolean(0);
            fail("expected exception");
        } catch (SQLException e) {
            assertEquals("Parameter 0 was not found.", e.getMessage());
        }
    }

    @Test
    public void testSetLobs() throws Exception {
        CallableStatementImpl mmcs = getCallableStatement();
        mmcs.paramsByName = new TreeMap<>();
        mmcs.paramsByName.put("foo", 2);
        mmcs.paramsByName.put("bar", 4);

        mmcs.setBlob(1, Mockito.mock(InputStream.class));
        mmcs.setBlob("foo", Mockito.mock(InputStream.class));
        mmcs.setNClob(3, Mockito.mock(Reader.class));
        mmcs.setBlob("bar", Mockito.mock(InputStream.class), 1);
        mmcs.setClob(5, Mockito.mock(Reader.class));

        List<Object> params = mmcs.getParameterValues();
        assertInstanceOf(Blob.class, params.get(0));
        assertInstanceOf(Blob.class, params.get(1));
        assertInstanceOf(Clob.class, params.get(2));
        assertInstanceOf(Blob.class, params.get(3));
        assertInstanceOf(Clob.class, params.get(4));
    }

    private CallableStatementImpl getCallableStatement() throws SQLException {
        ConnectionImpl conn = Mockito.mock(ConnectionImpl.class);
        ServerConnection sc = Mockito.mock(ServerConnection.class);

        Mockito.when(sc.getLogonResult()).thenReturn(new LogonResult());
        Mockito.when(conn.getServerConnection()).thenReturn(sc);

        return new CallableStatementImpl(conn, "{?=call x(?)}",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

}
