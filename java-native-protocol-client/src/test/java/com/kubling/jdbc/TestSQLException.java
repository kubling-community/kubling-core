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

import com.kubling.client.ProcedureErrorInstructionException;
import com.kubling.core.BundleUtil;
import com.kubling.core.KublingException;
import com.kubling.core.KublingProcessingException;
import com.kubling.core.KublingRuntimeException;
import com.kubling.net.CommunicationException;
import com.kubling.net.ConnectionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;


public class TestSQLException {

    /*
     * Test method for 'com.metamatrix.jdbc.MMSQLException.MMSQLException()'
     */
    @Test
    public void testMMSQLException() {
        KublingSQLException e = new KublingSQLException();
        String sqlState = e.getSQLState();
        Throwable cause = e.getCause();
        int errorCode = e.getErrorCode();
        Throwable nestedException = e.getCause();
        SQLException nextException = e.getNextException();

        assertNull(sqlState, "Expected MMSQLException.getSQLState() to return <null> but got \""
                + sqlState + "\" instead.");
        assertNull(cause, "Expected MMSQLException.getCause() to return <null> but got ["
                + (cause != null ? cause.getClass().getName()
                : "<unknown>") + "] instead.");
        assertEquals(0, errorCode, "Expected MMSQLException.getErrorCode() to return [0] but got ["
                + errorCode + "] instead.");
        assertNull(nestedException, "Expected MMSQLException.getNestedException() to return <null> but got ["
                + (nestedException != null ? nestedException.getClass()
                .getName() : "<unknown>") + "] instead.");
        assertNull(nextException, "Expected MMSQLException.getNextException() to return <null> " +
                "but got a SQLException with message \""
                + (nextException != null ? nextException.getMessage()
                : "") + "\" instead.");
    }

    /*
     * Test method for 'com.metamatrix.jdbc.MMSQLException.create(Throwable)'
     *
     * Tests various simple exceptions to see if the expected SQLState is
     * returend.
     */
    @Test
    public void testCreateThrowable_01() {
        testCreateThrowable(new CommunicationException(
                        "A test MM Communication Exception"),
                SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
        testCreateThrowable(
                new ConnectException("A test connection attempt exception"),
                SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
        testCreateThrowable(
                new ConnectionException("A test MM Connection Exception"),
                SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
        testCreateThrowable(new IOException(
                        "A test Generic java.io.IOException"),
                SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
        testCreateThrowable(
                new MalformedURLException(
                        "A test java.net.MalformedURLException"),
                SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
        testCreateThrowable(new KublingException(
                "A test Generic MM Core Exception"), SQLStates.DEFAULT);
        testCreateThrowable(new KublingException("A test MM Exception"),
                SQLStates.DEFAULT);
        testCreateThrowable(new KublingProcessingException(
                        "A test Generic MM Query Processing Exception"),
                SQLStates.USAGE_ERROR);
        testCreateThrowable(new KublingRuntimeException(
                "A test MM Runtime Exception"), SQLStates.DEFAULT);
        testCreateThrowable(new KublingSQLException(
                "A test Generic MM SQL Exception"), SQLStates.DEFAULT);
        testCreateThrowable(
                new NoRouteToHostException(
                        "A test java.net.NoRouteToHostException"),
                SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
        testCreateThrowable(new NullPointerException("A test NPE"),
                SQLStates.DEFAULT);
        testCreateThrowable(new ProcedureErrorInstructionException(
                        "A test SQL Procedure Error exception"),
                SQLStates.VIRTUAL_PROCEDURE_ERROR);
        testCreateThrowable(new SocketTimeoutException(
                        "A test socket timeout exception"),
                SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
        testCreateThrowable(
                new UnknownHostException("A test connection attempt exception"),
                SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
    }

    /*
     * Helper method to test SQLState and general MMSQLException validation
     */
    private void testCreateThrowable(Throwable ecause, String esqlState) {
        KublingSQLException e = KublingSQLException.create(ecause);
        if (ecause.getClass() == KublingSQLException.class) {
            ecause = null;
        }
        String sqlState = e.getSQLState();
        Throwable cause = e.getCause();
        int errorCode = e.getErrorCode();
        Throwable nestedException = e.getCause();
        SQLException nextException = e.getNextException();

        assertEquals(esqlState, sqlState);
        assertEquals(ecause, cause);
        assertEquals(0, errorCode);
        assertEquals(nestedException, ecause);
        assertNull(nextException);
    }

    @Test
    public void testCreate() {
        KublingSQLException exception = KublingSQLException.create(new Exception());

        assertEquals(exception.getMessage(), Exception.class.getName());
        assertNotNull(exception.getSQLState());
        assertEquals("38000", exception.getSQLState());

        assertEquals(exception, KublingSQLException.create(exception));
    }

    @Test
    public void testCreateFromSQLException() {
        SQLException sqlexception = new SQLException("foo", "21");

        SQLException nested = new SQLException("bar");

        sqlexception.setNextException(nested);

        String message = "top level message";

        KublingSQLException exception = KublingSQLException.create(sqlexception, message);
        exception.printStackTrace();
        assertEquals(sqlexception, exception.getCause());
        assertEquals(message, exception.getMessage());
        assertEquals(exception.getSQLState(), sqlexception.getSQLState());
    }

    public enum Event implements BundleUtil.Event {
        KBL21,
    }

    @Test
    public void testCodeAsVendorCode() {

        KublingException sqlexception = new KublingException(Event.KBL21, "foo");

        String message = "top level message";

        KublingSQLException exception = KublingSQLException.create(sqlexception, message);

        assertEquals(sqlexception.getCode(), exception.getKublingCode());
        assertEquals(21, exception.getErrorCode());
    }

}
