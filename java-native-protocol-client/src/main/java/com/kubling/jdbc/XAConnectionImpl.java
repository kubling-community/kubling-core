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

import com.kubling.client.security.InvalidSessionException;
import com.kubling.client.util.ExceptionUtil;
import com.kubling.client.xa.XATransactionException;
import com.kubling.client.xa.XidImpl;
import com.kubling.net.CommunicationException;
import com.kubling.net.ServerConnection;
import com.kubling.net.socket.SingleInstanceCommunicationException;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of XAConnection.
 */
public class XAConnectionImpl implements XAConnection, XAResource {

    private final class CloseInterceptor implements
            InvocationHandler {

        private final ConnectionImpl proxiedConnection;

        CloseInterceptor(ConnectionImpl connection) {
            this.proxiedConnection = connection;
        }

        public Object invoke(Object proxy,
                             Method method,
                             Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                close();
                return null;
            }

            try {
                return method.invoke(this.proxiedConnection, args);
            } catch (InvocationTargetException e) {
                Exception ex = ExceptionUtil.getExceptionOfType(e, InvalidSessionException.class);
                if (ex == null) {
                    ex = ExceptionUtil.getExceptionOfType(e, CommunicationException.class);
                    if (ex instanceof SingleInstanceCommunicationException) {
                        ServerConnection sc = proxiedConnection.getServerConnection();
                        if (!sc.isOpen(ServerConnection.PING_INTERVAL)) {
                            ex = null;
                        }
                    }
                }
                if (ex != null) {
                    SQLException se;
                    if (e.getCause() instanceof SQLException) {
                        se = (SQLException) e.getCause();
                    } else {
                        se = KublingSQLException.create(e.getCause());
                    }
                    notifyListener(se);
                }
                throw e.getTargetException();
            }
        }

        void close() {
            this.proxiedConnection.recycleConnection();
            XAConnectionImpl.this.notifyListener(null);
        }
    }

    private static final Logger logger = Logger.getLogger("com.kubling.jdbc");

    private int timeOut;
    private Set<ConnectionEventListener> listeners;
    private final ConnectionImpl connection;
    private CloseInterceptor handler;
    private boolean isClosed;

    public XAConnectionImpl(ConnectionImpl conn) {
        this.connection = conn;
    }

    public Connection getConnection() throws SQLException {
        ConnectionImpl conn = getConnectionImpl();
        if (handler != null) {
            handler.close();
        }
        handler = new CloseInterceptor(conn);
        return (Connection) Proxy
                .newProxyInstance(this.getClass().getClassLoader(), new Class[]{Connection.class}, handler);
    }

    ConnectionImpl getConnectionImpl() throws SQLException {
        if (isClosed) {
            throw new SQLException(JDBCPlugin.Util.getString("MMXAConnection.connection_is_closed"));
        }

        return connection;
    }

    public synchronized void addConnectionEventListener(ConnectionEventListener listener) {
        if (listeners == null) {
            listeners = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        this.listeners.add(listener);
    }

    public synchronized void removeConnectionEventListener(ConnectionEventListener listener) {
        if (listeners == null) {
            return;
        }
        this.listeners.remove(listener);
    }

    public XAResource getXAResource() {
        return this;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        isClosed = true;
    }

    /**
     * Notify listeners, if there is any, about the connection status.
     * If e is null, the connection is properly closed.
     */
    protected synchronized void notifyListener(SQLException e) {
        if (listeners != null && !listeners.isEmpty()) {
            for (ConnectionEventListener listener : listeners) {
                if (e == null) {
                    //no exception
                    listener.connectionClosed(new ConnectionEvent(this));
                } else {
                    //exception occurred
                    listener.connectionErrorOccurred(new ConnectionEvent(this, e));
                }
            }
        }
    }

    public void addStatementEventListener(StatementEventListener arg0) {
    }

    public void removeStatementEventListener(StatementEventListener arg0) {
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        XidImpl mmXid = getMMXid(xid);
        try {
            getMMConnection().commitTransaction(mmXid, onePhase);
        } catch (SQLException e) {
            String logMsg = JDBCPlugin.Util.getString("MMXAResource.FailedCommitTXN", xid, onePhase ? "true" : "false");
            throw handleError(e, logMsg);
        }
    }

    private XAException handleError(Exception e, String logMsg) {
        logger.log(Level.FINE, logMsg, e);

        if (e instanceof KublingSQLException) {
            Throwable ex = e.getCause();
            if (ex instanceof XAException) {
                return (XAException) ex;
            }
            if (ex instanceof XATransactionException) {
                return ((XATransactionException) ex).getXAException();
            }
        }
        return new XAException(XAException.XAER_RMERR);
    }

    /**
     * @see XAResource#end(Xid, int)
     */
    public void end(Xid xid, int flag) throws XAException {
        XidImpl mmXid = getMMXid(xid);
        try {
            getMMConnection().endTransaction(mmXid, flag);
        } catch (SQLException e) {
            String logMsg = JDBCPlugin.Util.getString("MMXAResource.FailedEndTXN", xid, flag);
            throw handleError(e, logMsg);
        }
    }

    /**
     * @see XAResource#forget(Xid)
     */
    public void forget(Xid xid) throws XAException {
        XidImpl mmXid = getMMXid(xid);
        try {
            getMMConnection().forgetTransaction(mmXid);
        } catch (SQLException e) {
            String logMsg = JDBCPlugin.Util.getString("MMXAResource.FailedForgetTXN", xid);
            throw handleError(e, logMsg);
        }
    }

    public int getTransactionTimeout() {
        return timeOut;
    }

    public boolean isSameRM(XAResource arg0) throws XAException {
        if (arg0 == this) {
            return true;
        }
        if (!(arg0 instanceof XAConnectionImpl other)) {
            return false;
        }
        try {
            return this.getMMConnection().isSameProcess(other.getMMConnection());
        } catch (CommunicationException e) {
            throw handleError(e, JDBCPlugin.Util.getString("MMXAResource.FailedISSameRM"));
        }
    }

    public int prepare(Xid xid) throws XAException {
        XidImpl mmXid = getMMXid(xid);
        try {
            return getMMConnection().prepareTransaction(mmXid);
        } catch (SQLException e) {
            String logMsg = JDBCPlugin.Util.getString("MMXAResource.FailedPrepareTXN", xid);
            throw handleError(e, logMsg);
        }
    }

    /**
     * @see XAResource#recover(int)
     */
    public Xid[] recover(int flag) throws XAException {
        try {
            return getMMConnection().recoverTransaction(flag);
        } catch (SQLException e) {
            String logMsg = JDBCPlugin.Util.getString("MMXAResource.FailedRecoverTXN", flag);
            throw handleError(e, logMsg);
        }
    }

    public void rollback(Xid xid) throws XAException {
        XidImpl mmXid = getMMXid(xid);
        try {
            getMMConnection().rollbackTransaction(mmXid);
        } catch (SQLException e) {
            String logMsg = JDBCPlugin.Util.getString("MMXAResource.FailedRollbackTXN", xid);
            throw handleError(e, logMsg);
        }
    }

    public boolean setTransactionTimeout(int seconds) {
        timeOut = seconds;
        return true;
    }

    public void start(Xid xid, int flag) throws XAException {
        XidImpl mmXid = getMMXid(xid);
        try {
            getMMConnection().startTransaction(mmXid, flag, timeOut);
        } catch (SQLException e) {
            String logMsg = JDBCPlugin.Util.getString("MMXAResource.FailedStartTXN", xid, flag);
            throw handleError(e, logMsg);
        }
    }

    private ConnectionImpl getMMConnection() throws XAException {
        try {
            return this.getConnectionImpl();
        } catch (SQLException e) {
            throw new XAException(XAException.XAER_RMFAIL);
        }
    }

    private XidImpl getMMXid(Xid originalXid) {
        return new XidImpl(originalXid);
    }
}
