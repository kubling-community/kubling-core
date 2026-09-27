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
import com.kubling.client.security.InvalidSessionException;
import com.kubling.client.security.LogonException;
import com.kubling.client.util.ExceptionUtil;
import com.kubling.core.KublingException;
import com.kubling.core.KublingProcessingException;
import com.kubling.core.KublingRuntimeException;
import com.kubling.net.CommunicationException;
import com.kubling.net.ConnectionException;

import java.io.IOException;
import java.io.Serial;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.sql.SQLException;


/**
 * Kubling specific SQLException
 */

public class KublingSQLException extends SQLException {

    @Serial
    private static final long serialVersionUID = 3672305321346173922L;
    private String kublingCode;

    /**
     * No-arg constructor required by Externalizable semantics.
     */
    public KublingSQLException() {
        super();
    }

    public KublingSQLException(String reason) {
        super(reason, SQLStates.DEFAULT);
    }

    public KublingSQLException(String reason, String state) {
        super(reason, state);
    }

    public static KublingSQLException create(Throwable exception) {
        if (exception instanceof KublingSQLException) {
            return (KublingSQLException) exception;
        }
        return create(exception, exception.getMessage());
    }

    public KublingSQLException(Throwable ex, String reason, String sqlState, int errorCode) {
        super(reason, sqlState, errorCode); // passing the message to the super class constructor.
        initCause(ex);
    }

    private KublingSQLException(SQLException ex, String message, boolean addChildren) {
        super(message, ex.getSQLState() == null ? SQLStates.DEFAULT : ex.getSQLState(), ex.getErrorCode(), ex);
        if (addChildren) {
            SQLException childException = ex.getNextException(); // this a child to the SQLException constructed from reason

            while (childException != null) {
                if (childException instanceof KublingSQLException) {
                    super.setNextException(ex);
                    break;
                }
                super.setNextException(new KublingSQLException(childException, getMessage(childException, null), false));
                childException = childException.getNextException();
            }
        }
    }

    public static KublingSQLException create(Throwable exception, String message) {
        message = getMessage(exception, message);
        Throwable origException = exception;
        if (exception instanceof KublingSQLException
                && message.equals(exception.getMessage())) {
            return (KublingSQLException) exception;
        }
        if (exception instanceof SQLException) {
            return new KublingSQLException((SQLException) exception, message, true);
        }
        String sqlState = null;
        int errorCode = 0;
        SQLException se = ExceptionUtil.getExceptionOfType(exception, SQLException.class);
        if (se != null && se.getSQLState() != null) {
            sqlState = se.getSQLState();
            errorCode = se.getErrorCode();
        }
        KublingException te = ExceptionUtil.getExceptionOfType(exception, KublingException.class);
        String code = null;
        if (te != null && te.getCode() != null) {
            code = te.getCode();
            if (errorCode == 0) {
                String intPart = code;
                if (code.startsWith("KBL")) {
                    intPart = code.substring(3);
                }
                try {
                    errorCode = Integer.parseInt(intPart);
                } catch (NumberFormatException e) {
                    // Ignored
                }
            }
        }
        if (sqlState == null) {
            exception = findRootException(exception);
            sqlState = determineSQLState(exception, sqlState);
        }
        if (sqlState == null) {
            sqlState = SQLStates.DEFAULT;
        }
        KublingSQLException tse = new KublingSQLException(origException, message, sqlState, errorCode);
        tse.kublingCode = code;
        return tse;
    }

    private static String determineSQLState(Throwable exception,
                                            String sqlState) {
        if (exception instanceof InvalidSessionException) {
            sqlState = SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION;
        } else if (exception instanceof LogonException) {
            sqlState = SQLStates.INVALID_AUTHORIZATION_SPECIFICATION_NO_SUBCLASS;
        } else if (exception instanceof ProcedureErrorInstructionException) {
            sqlState = SQLStates.VIRTUAL_PROCEDURE_ERROR;
        } else if (exception instanceof KublingProcessingException) {
            sqlState = SQLStates.USAGE_ERROR;
            if (SQLStates.QUERY_CANCELED.equals(((KublingException) exception).getCode())) {
                sqlState = SQLStates.QUERY_CANCELED;
            }
        } else if (exception instanceof UnknownHostException
                || exception instanceof ConnectException
                || exception instanceof MalformedURLException
                || exception instanceof NoRouteToHostException
                || exception instanceof ConnectionException) {
            sqlState = SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION;
        } else if (exception instanceof IOException) {
            sqlState = SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION;
        } else if (exception instanceof KublingException) {
            if (exception instanceof CommunicationException) {
                sqlState = SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION;
            }

            Throwable originalException = exception;
            exception = originalException.getCause();
            exception = findRootException(exception);

            if (exception != null && exception != originalException) {
                sqlState = determineSQLState(exception, sqlState);
            }
        }
        return sqlState;
    }

    private static Throwable findRootException(Throwable exception) {
        if (exception instanceof KublingRuntimeException) {
            while (exception.getCause() != exception
                    && exception.getCause() != null) {
                exception = exception.getCause();
            }
            if (exception instanceof KublingRuntimeException runtimeException) {
                while (runtimeException.getCause() != exception
                        && runtimeException.getCause() != null) {
                    if (runtimeException.getCause() instanceof KublingRuntimeException) {
                        runtimeException = (KublingRuntimeException) runtimeException
                                .getCause();
                    } else {
                        exception = runtimeException.getCause();
                        break;
                    }
                }
            }
        }
        return exception;
    }

    /**
     * @since 4.1
     */
    private static String getMessage(Throwable exception,
                                     String message) {
        if (message == null) {
            message = exception.getMessage();
            if (message == null) {
                message = exception.getClass().getName();
            }
        }
        return message;
    }

    public boolean isSystemErrorState() {
        return SQLStates.isSystemErrorState(getSQLState());
    }

    public boolean isUsageErrorState() {
        return SQLStates.isUsageErrorState(getSQLState());
    }

    public String getKublingCode() {
        return kublingCode;
    }
}
