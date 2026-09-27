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
import com.kubling.client.plan.Annotation;
import com.kubling.client.plan.PlanNode;
import com.kubling.client.util.ResultsFuture;
import com.kubling.client.xa.XATransactionException;
import com.kubling.client.xa.XidImpl;
import com.kubling.core.KublingComponentException;
import com.kubling.core.KublingProcessingException;
import com.kubling.core.types.ArrayImpl;
import com.kubling.core.util.PropertiesUtils;
import com.kubling.core.util.SqlUtil;
import com.kubling.net.CommunicationException;
import com.kubling.net.ConnectionException;
import com.kubling.net.ServerConnection;
import com.kubling.net.KublingURL;
import com.kubling.net.socket.SocketServerConnection;

import javax.transaction.xa.Xid;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Kubling's Connection implementation.
 */
public class ConnectionImpl extends WrapperImpl implements KublingConnection {
    private static final int MAX_OPEN_STATEMENTS =
            PropertiesUtils.getHierarchicalProperty("com.kubling.maxOpenStatements", 1000, Integer.class);

    private static final Logger logger = Logger.getLogger("com.kubling.jdbc");

    public static final int DEFAULT_ISOLATION = Connection.TRANSACTION_READ_COMMITTED;

    // constant value giving product name
    private final static String SERVER_NAME = "Kubling DBVirt Server";
    private final static String EMBEDDED_NAME = "Kubling DBVirt Embedded";

    // Unique request ID generator
    private long requestIDGenerator;

    // url used to create the connection
    private final String url;

    // properties object containing the connection properties.
    protected Properties propInfo;

    // status of connection object
    private boolean closed = false;
    // determines if a statement executed should be immediately committed.
    private boolean autoCommitFlag = true;
    private boolean inLocalTxn;

    // collection of all open statements on this connection
    private final Collection<StatementImpl> statements = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // cached DatabaseMetadata
    private DatabaseMetaDataImpl dbmm;

    //Xid for participating in TXN
    private XidImpl transactionXid;

    //  Flag to represent if the connection state needs to be readOnly, default value false.
    private boolean readOnly = false;

    private final DQP dqp;
    protected ServerConnection serverConn;
    private int transactionIsolation = DEFAULT_ISOLATION;

    //  the last query plan description
    private PlanNode currentPlanDescription;
    // the last query debug log
    private String debugLog;
    // the last query annotations
    private Collection<Annotation> annotations;
    private final Properties connectionProps;
    private Properties payload;

    //used to mimic transaction level, rather than connection level characteristics
    private Boolean savedReadOnly;
    private int savedIsolationLevel;

    public ConnectionImpl(ServerConnection serverConn, Properties info, String url) {
        this.connectionProps = info;
        this.serverConn = serverConn;
        this.url = url;
        this.dqp = serverConn.getService(DQP.class);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(JDBCPlugin.Util.getString("MMConnection.Session_success"));
            logConnectionProperties(url, info);
        }

        setExecutionProperties(info);
    }

    boolean isInLocalTxn() {
        return inLocalTxn;
    }

    private void setExecutionProperties(Properties info) {
        this.propInfo = new Properties();

        String defaultFetchSize = info.getProperty(ExecutionProperties.PROP_FETCH_SIZE);
        propInfo.put(
                ExecutionProperties.PROP_FETCH_SIZE,
                Objects.requireNonNullElseGet(defaultFetchSize, () -> String.valueOf(BaseDataSource.DEFAULT_FETCH_SIZE)));

        String partialResultsMode = info.getProperty(ExecutionProperties.PROP_PARTIAL_RESULTS_MODE);
        propInfo.put(
                ExecutionProperties.PROP_PARTIAL_RESULTS_MODE,
                Objects.requireNonNullElse(partialResultsMode, BaseDataSource.DEFAULT_PARTIAL_RESULTS_MODE));

        String resultSetCacheMode = info.getProperty(ExecutionProperties.RESULT_SET_CACHE_MODE);
        propInfo.put(
                ExecutionProperties.RESULT_SET_CACHE_MODE,
                Objects.requireNonNullElse(resultSetCacheMode, BaseDataSource.DEFAULT_RESULT_SET_CACHE_MODE));

        String ansiQuotes = info.getProperty(ExecutionProperties.ANSI_QUOTED_IDENTIFIERS);
        propInfo.put(
                ExecutionProperties.ANSI_QUOTED_IDENTIFIERS,
                Objects.requireNonNullElseGet(ansiQuotes, Boolean.TRUE::toString));

        for (String key : info.stringPropertyNames()) {
            String actualKey = JDBCURL.EXECUTION_PROPERTIES.get(key);
            if (actualKey != null) {
                propInfo.setProperty(actualKey, info.getProperty(key));
            }
        }
    }

    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Collection<Annotation> annotations) {
        this.annotations = annotations;
    }

    public String getDebugLog() {
        return debugLog;
    }

    public void setDebugLog(String debugLog) {
        this.debugLog = debugLog;
    }

    public PlanNode getCurrentPlanDescription() {
        return currentPlanDescription;
    }

    public void setCurrentPlanDescription(PlanNode currentPlanDescription) {
        this.currentPlanDescription = currentPlanDescription;
    }

    protected Properties getExecutionProperties() {
        return this.propInfo;
    }

    public void setExecutionProperty(String key, String value) {
        JDBCURL.addNormalizedProperty(key, value, getExecutionProperties());
    }

    public String getExecutionProperty(String key) {
        return this.getExecutionProperties().getProperty(JDBCURL.getValidKey(key));
    }

    DQP getDQP() {
        return this.dqp;
    }

    /**
     * Remove password & trusted token and log all other properties
     *
     * @param connUrl - URL used to connect to server
     * @param info    - properties object supplied
     */
    private void logConnectionProperties(String connUrl, Properties info) {
        StringBuilder modifiedUrl = new StringBuilder();

        // If we have valid URL
        if (connUrl != null) {
            // We need wipe out the password here, before we write to the log
            int startIndex = connUrl.indexOf("password=");
            if (startIndex != -1) {
                modifiedUrl.append(connUrl, 0, startIndex);
                modifiedUrl.append("password=***");
                int endIndex = connUrl.indexOf(";", startIndex + 9);
                if (endIndex != -1) {
                    modifiedUrl.append(";").append(connUrl.substring(endIndex));
                }
            }
            logger.fine("Connection Url=" + modifiedUrl);
        }

        // Now clone the properties object and remove password and trusted token
        if (info != null) {
            Enumeration<?> enumeration = info.keys();
            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                Object anObj = info.get(key);
                // Log each property except for password and token.
                if (!KublingURL.CONNECTION.PASSWORD.equalsIgnoreCase(key)) {
                    logger.fine(key + "=" + anObj);
                }
            }
        }
    }

    String getUrl() {
        return this.url;
    }

    /**
     * Connection identifier of this connection
     *
     * @return identifier
     */
    public String getConnectionId() {
        return this.serverConn.getLogonResult().getSessionID();
    }

    /**
     * Generate the next unique requestID for matching up requests with responses.
     * These IDs should be unique only in the context of a ServerConnection instance.
     *
     * @return Request ID
     */
    protected synchronized long nextRequestID() {
        return requestIDGenerator++;
    }

    /**
     * Cancel the request
     */
    public void cancelRequest(String id) throws KublingProcessingException, KublingComponentException {
        this.dqp.cancelRequest(Long.parseLong(id));
    }

    public void clearWarnings() {
        // do nothing
    }

    public void close() throws SQLException {

        SQLException firstException = null;

        if (closed) {
            return;
        }

        try {
            // close any statements that were created on this connection
            try {
                closeStatements();
            } catch (SQLException se) {
                firstException = se;
            } finally {
                this.serverConn.close();
                if (firstException != null) {
                    throw firstException;
                }
            }
        } catch (SQLException se) {
            throw KublingSQLException.create(se,
                    JDBCPlugin.Util.getString("MMConnection.Err_connection_close", se.getMessage()));
        } finally {
            logger.fine(JDBCPlugin.Util.getString("MMConnection.Connection_close_success"));
            // set the status of the connection to closed
            closed = true;
        }
    }

    /**
     * <p>
     * Close all the statements open on this connection
     *
     * @throws SQLException server statement object could not be closed.
     */
    void closeStatements() throws SQLException {
        // Closing the statement will cause the
        // MMConnection.closeStatement() method to be called,
        // which will modify this.statements.  So, we do this iteration
        // in a separate safe copy of the list
        List<StatementImpl> statementsSafe = new ArrayList<>(this.statements);
        SQLException ex = null;
        for (StatementImpl statement : statementsSafe) {
            try {
                statement.close();
            } catch (SQLException e) {
                ex = e;
            }
        }
        if (ex != null) {
            throw KublingSQLException.create(ex, JDBCPlugin.Util.getString("MMConnection.Err_closing_stmts"));
        }
    }

    /**
     * Called by MMStatement to notify the connection that the
     * statement has been closed.
     */
    void closeStatement(Statement statement) {
        this.statements.remove(statement);
    }

    /**
     * <p>This method makes any changes involved in a transaction permanent and releases
     * any locks held by the connection object.  This is only used when auto-commit
     * is set to false.
     *
     * @throws SQLException if the transaction had been rolled back or marked to roll back.
     */
    public void commit() throws SQLException {
        checkConnection();
        if (!autoCommitFlag) {
            try {
                directCommit();
            } finally {
                restoreTransactionCharacteristics();
                inLocalTxn = false;
            }
        }
    }

    private void directCommit() throws SQLException {
        if (inLocalTxn) {
            try {
                ResultsFuture<?> future = this.dqp.commit();
                future.get();
            } catch (Exception e) {
                throw KublingSQLException.create(e);
            }
            logger.fine(JDBCPlugin.Util.getString("MMConnection.Commit_success"));
        }
    }

    void beginLocalTxnIfNeeded() throws SQLException {
        if (this.transactionXid != null || inLocalTxn || this.autoCommitFlag || isDisableLocalTxn()) {
            return;
        }
        try {
            try {
                this.dqp.begin();
            } catch (XATransactionException e) {
                throw KublingSQLException.create(e);
            }
            inLocalTxn = true;
        } finally {
            if (!inLocalTxn) {
                autoCommitFlag = true;
            }
        }
    }

    private boolean isDisableLocalTxn() {
        String prop = this.propInfo.getProperty(ExecutionProperties.DISABLE_LOCAL_TRANSACTIONS);
        return Boolean.parseBoolean(prop);
    }

    public StatementImpl createStatement() throws SQLException {
        return createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    @Override
    public StatementImpl createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement(resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    /**
     * @since 4.3
     */
    private void validateResultSetType(int resultSetType) throws KublingSQLException {
        if (resultSetType == ResultSet.TYPE_SCROLL_SENSITIVE) {
            String msg = JDBCPlugin.Util.getString("MMConnection.Scrollable_type_not_supported",
                    "ResultSet.TYPE_SCROLL_SENSITIVE");
            throw new KublingSQLException(msg);
        }
    }

    /**
     * @since 4.3
     */
    private void validateResultSetConcurrency(int resultSetConcurrency) throws KublingSQLException {
        if (resultSetConcurrency == ResultSet.CONCUR_UPDATABLE) {
            String msg = JDBCPlugin.Util.getString("MMConnection.Concurrency_type_not_supported", "ResultSet.CONCUR_UPDATABLE");
            throw new KublingSQLException(msg);
        }
    }

    public boolean getAutoCommit() throws SQLException {
        //Check to see the connection is open
        checkConnection();
        return autoCommitFlag;
    }

    public String getCatalog() throws SQLException {
        //Check to see the connection is open
        checkConnection();
        //catalogs are not supported
        return this.serverConn.getLogonResult().getVdbName();
    }

    /**
     * <p>This method gets the ServerConnection object wrapped by this object.
     *
     * @return ServerConnection object
     */
    public ServerConnection getServerConnection() throws SQLException {
        //Check to see the connection is open
        checkConnection();
        return serverConn;
    }

    String getVDBName() throws SQLException {
        //Check to see the connection is open
        checkConnection();
        //get the virtual database name to which we are connected.

        return this.serverConn.getLogonResult().getVdbName();
    }

    @Deprecated
    public int getVDBVersion() throws SQLException {
        checkConnection();
        return this.serverConn.getLogonResult().getVdbVersion();
    }

    /**
     * Get's the name of the user who got this connection.
     *
     * @return Sring object giving the username
     * @throws SQLException if the connection is closed
     */
    String getUserName() throws SQLException {
        checkConnection();

        return this.serverConn.getLogonResult().getUserName();
    }

    public DatabaseMetaDataImpl getMetaData() throws SQLException {
        //Check to see the connection is open
        checkConnection();

        if (dbmm == null) {
            dbmm = new DatabaseMetaDataImpl(this);
        }
        return dbmm;
    }

    /**
     * Get the database name that this connection is representing
     *
     * @return String name of the database
     */
    public String getDatabaseName() {
        if (this.serverConn instanceof SocketServerConnection) {
            return SERVER_NAME;
        }
        return EMBEDDED_NAME;
    }

    @Override
    public int getHoldability() {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    public int getTransactionIsolation() {
        return this.transactionIsolation;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() {
        return Collections.emptyMap();
    }

    /**
     * <p>This method will return the first warning reported by calls on this connection,
     * or null if none exist.
     *
     * @return A SQLWarning object if there are any warnings.
     * @throws SQLException should never occur
     */
    public SQLWarning getWarnings() throws SQLException {
        //Check to see the connection is open
        checkConnection();
        return null;  // we don't have any warnings
    }

    /**
     * <p>This method will return whether this connection is closed or not.
     *
     * @return booleanvalue indicating if the connection is closed
     */
    public boolean isClosed() {
        return closed;
    }

    public boolean isReadOnly() throws SQLException {
        checkConnection();
        return readOnly;
    }

    public String nativeSQL(String sql) {
        // return the string argument without any modifications.
        // escape syntaxes are directly supported in the server
        return sql;
    }

    /**
     * <p>Creates a CallableStatement object that contains sql and that will produce
     * ResultSet objects that are non-scrollable and non-updatable. A SQL stored
     * procedure call statement is handled by creating a CallableStatement for it.
     *
     * @param sql String(escape syntax) for invoking a stored procedure.
     * @return CallableStatement object that can be used to execute the storedProcedure
     * @throws SQLException if there is an error creating the callable statement object
     */
    public CallableStatementImpl prepareCall(String sql) throws SQLException {
        //there is a problem setting the result set type to be non-scrollable
        //See defect 17768
        return prepareCall(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    }

    @Override
    public CallableStatementImpl prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return prepareCall(sql, resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    /**
     * @since 4.3
     */
    private void validateSQL(String sql) throws KublingSQLException {
        if (sql == null) {
            String msg = JDBCPlugin.Util.getString("MMConnection.SQL_cannot_be_null");
            throw new KublingSQLException(msg);
        }
    }

    public PreparedStatementImpl prepareStatement(String sql) throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    public PreparedStatementImpl prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return prepareStatement(sql, resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    public PreparedStatementImpl prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                                  int resultSetHoldability, int autoGeneratedKeys) throws SQLException {
        //Check to see the connection is open
        checkConnection();

        validateResultSetType(resultSetType);
        validateResultSetConcurrency(resultSetConcurrency);
        validateSQL(sql);

        // add the statement object to the map
        PreparedStatementImpl newStatement = new PreparedStatementImpl(this, sql, resultSetType, resultSetConcurrency);
        newStatement.setAutoGeneratedKeys(autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS);
        addStatement(newStatement);
        return newStatement;
    }

    public PreparedStatementImpl prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                                  int resultSetHoldability) throws SQLException {
        return prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability, Statement.NO_GENERATED_KEYS);
    }

    public void rollback() throws SQLException {
        rollback(true);
    }

    /**
     * Rollback the current local transaction
     */
    public void rollback(boolean startTxn) throws SQLException {

        //Check to see the connection is open
        checkConnection();
        if (!autoCommitFlag) {
            if (this.transactionXid != null) {
                throw new KublingSQLException(JDBCPlugin.Util.getString("MMStatement.In_XA_Transaction"));
            }
            try {
                if (this.inLocalTxn) {
                    this.inLocalTxn = false;
                    try {
                        ResultsFuture<?> future = this.dqp.rollback();
                        future.get();
                    } catch (Exception e) {
                        throw KublingSQLException.create(e);
                    }
                    logger.fine(JDBCPlugin.Util.getString("MMConnection.Rollback_success"));
                }
            } finally {
                restoreTransactionCharacteristics();
                if (startTxn) {
                    this.inLocalTxn = false;
                } else {
                    this.autoCommitFlag = true;
                }
            }
        }
    }

    public ResultsFuture<?> submitSetAutoCommitTrue(boolean commit) throws SQLException {
        //Check to see the connection is open
        checkConnection();

        if (this.autoCommitFlag) {
            return ResultsFuture.NULL_FUTURE;
        }

        this.autoCommitFlag = true;

        if (isDisableLocalTxn()) {
            return ResultsFuture.NULL_FUTURE;
        }

        try {
            if (commit) {
                return dqp.commit();
            }
            return dqp.rollback();
        } catch (XATransactionException e) {
            throw KublingSQLException.create(e);
        }
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        //Check to see the connection is open
        checkConnection();

        if (autoCommit == this.autoCommitFlag) {
            return;
        }

        if (autoCommit && this.transactionXid != null) {
            throw new KublingSQLException(JDBCPlugin.Util.getString("MMStatement.In_XA_Transaction"));
        }

        this.autoCommitFlag = autoCommit;

        if (autoCommit) {
            directCommit();
        } else {
            inLocalTxn = false;
        }
    }

    /**
     * <p>Kubling does not allow setting a catalog through a connection. This
     * method silently ignores the request as per the specification.
     */
    public void setCatalog(String catalog) {
        // do nothing, silently ignore the request
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkConnection();
        // During transaction do not allow to change this flag
        if (isInLocalTxn() || this.transactionXid != null) {
            throw new KublingSQLException(JDBCPlugin.Util.getString("MMStatement.Invalid_During_Transaction",
                    "setReadOnly(" + readOnly + ")"));
        }
        this.readOnly = readOnly;
    }

    /**
     * <p> This utility method checks if the jdbc connection is closed and
     * throws an exception if it is closed.
     *
     * @throws KublingSQLException if the connection object is closed.
     */
    void checkConnection() throws KublingSQLException {
        //Check to see the connection is closed and proceed if it is not
        if (closed) {
            throw new KublingSQLException(JDBCPlugin.Util.getString("MMConnection.Cant_use_closed_connection"));
        }
    }

    protected void commitTransaction(XidImpl arg0, boolean arg1) throws SQLException {
        checkConnection();
        transactionXid = null;
        this.autoCommitFlag = true;
        try {
            ResultsFuture<?> future = this.dqp.commit(arg0, arg1);
            future.get();
        } catch (Exception e) {
            throw KublingSQLException.create(e);
        }
    }

    protected void endTransaction(XidImpl arg0, int arg1) throws SQLException {
        checkConnection();
        this.autoCommitFlag = true;
        try {
            ResultsFuture<?> future = this.dqp.end(arg0, arg1);
            future.get();
        } catch (Exception e) {
            throw KublingSQLException.create(e);
        }
    }

    protected void forgetTransaction(XidImpl arg0) throws SQLException {
        checkConnection();
        try {
            ResultsFuture<?> future = this.dqp.forget(arg0);
            future.get();
        } catch (Exception e) {
            throw KublingSQLException.create(e);
        }
    }

    protected int prepareTransaction(XidImpl arg0) throws SQLException {
        checkConnection();
        transactionXid = null;
        try {
            ResultsFuture<Integer> future = this.dqp.prepare(arg0);
            return future.get();
        } catch (Exception e) {
            throw KublingSQLException.create(e);
        }
    }

    protected Xid[] recoverTransaction(int arg0) throws SQLException {
        checkConnection();
        try {
            ResultsFuture<Xid[]> future = this.dqp.recover(arg0);
            return future.get();
        } catch (Exception e) {
            throw KublingSQLException.create(e);
        }
    }

    protected void rollbackTransaction(XidImpl arg0) throws SQLException {
        checkConnection();
        transactionXid = null;
        this.autoCommitFlag = true;
        try {
            ResultsFuture<?> future = this.dqp.rollback(arg0);
            future.get();
        } catch (Exception e) {
            throw KublingSQLException.create(e);
        }
    }

    protected void startTransaction(XidImpl arg0, int arg1, int timeout) throws SQLException {
        checkConnection();
        try {
            ResultsFuture<?> future = this.dqp.start(arg0, arg1, timeout);
            future.get();
        } catch (Exception e) {
            throw KublingSQLException.create(e);
        }
        transactionXid = arg0;
        this.autoCommitFlag = false;
    }

    protected XidImpl getTransactionXid() {
        return transactionXid;
    }

    public boolean isValid(int timeout) throws SQLException {
        return this.getServerConnection().isOpen(timeout * 1000L);
    }

    public void recycleConnection() {
        this.payload = null;
        try {
            //close all open statements
            this.closeStatements();
        } catch (SQLException e) {
            logger.log(Level.WARNING, JDBCPlugin.Util.getString("MMXAConnection.rolling_back_error"), e);
        }
        try {
            //rollback if still in a transaction
            if (!this.getAutoCommit()) {
                logger.warning(JDBCPlugin.Util.getString("MMXAConnection.rolling_back"));

                if (this.getTransactionXid() == null) {
                    this.rollback(false);
                } else {
                    this.rollbackTransaction(getTransactionXid());
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, JDBCPlugin.Util.getString("MMXAConnection.rolling_back_error"), e);
        }
    }

    public boolean isSameProcess(ConnectionImpl conn) throws CommunicationException {
        return this.serverConn.isSameInstance(conn.serverConn);
    }

    public void setClientInfo(Properties properties) {
    }

    public void setClientInfo(String name, String value) {
    }

    public Properties getClientInfo() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public String getClientInfo(String name) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public Array createArrayOf(String typeName, Object[] elements) {
        return new ArrayImpl(elements);
    }

    public Blob createBlob() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public Clob createClob() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public NClob createNClob() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public SQLXML createSQLXML() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public StatementImpl createStatement(int resultSetType,
                                         int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        //Check to see the connection is open
        checkConnection();

        validateResultSetType(resultSetType);
        validateResultSetConcurrency(resultSetConcurrency);
        //TODO: implement close cursors at commit

        // add the statement object to the map
        StatementImpl newStatement = new StatementImpl(this, resultSetType, resultSetConcurrency);
        addStatement(newStatement);

        return newStatement;
    }

    private void addStatement(StatementImpl newStatement) throws SQLException {
        if (statements.size() >= MAX_OPEN_STATEMENTS) {
            this.close();
            throw new KublingSQLException(JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20036, MAX_OPEN_STATEMENTS));
        }
        statements.add(newStatement);
    }

    public Struct createStruct(String typeName, Object[] attributes)
            throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public CallableStatementImpl prepareCall(String sql, int resultSetType,
                                             int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        //Check to see the connection is open
        checkConnection();

        validateResultSetType(resultSetType);
        validateResultSetConcurrency(resultSetConcurrency);
        validateSQL(sql);
        //TODO: implement close cursors at commit

        // add the statement object to the map
        CallableStatementImpl newStatement = new CallableStatementImpl(this, sql, resultSetType, resultSetConcurrency);
        addStatement(newStatement);
        return newStatement;
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.HOLD_CURSORS_OVER_COMMIT, Statement.RETURN_GENERATED_KEYS);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.HOLD_CURSORS_OVER_COMMIT, Statement.RETURN_GENERATED_KEYS);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.HOLD_CURSORS_OVER_COMMIT, Statement.RETURN_GENERATED_KEYS);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public void setHoldability(int holdability) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public Savepoint setSavepoint() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public void setTransactionIsolation(int level) {
        this.transactionIsolation = level;
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    Object setPassword(Object newPassword) {
        if (newPassword != null) {
            return this.connectionProps.put(KublingURL.CONNECTION.PASSWORD, newPassword);
        }
        return this.connectionProps.remove(KublingURL.CONNECTION.PASSWORD);
    }

    String getPassword() {
        Object result = this.connectionProps.get(KublingURL.CONNECTION.PASSWORD);
        if (result == null) {
            return null;
        }
        return result.toString();
    }

    @Override
    public void changeUser(String userName, String newPassword)
            throws SQLException {
        //TODO: recycleConnection();
        Object oldName;
        Object oldPassword;
        if (userName != null) {
            oldName = this.connectionProps.put(KublingURL.CONNECTION.USER_NAME, userName);
        } else {
            oldName = this.connectionProps.remove(KublingURL.CONNECTION.USER_NAME);
        }
        oldPassword = setPassword(newPassword);
        boolean success = false;
        try {
            this.serverConn.authenticate();
            success = true;
        } catch (ConnectionException | CommunicationException e) {
            throw KublingSQLException.create(e);
        } finally {
            if (!success) {
                if (oldName != null) {
                    this.connectionProps.put(KublingURL.CONNECTION.USER_NAME, oldName);
                } else {
                    this.connectionProps.remove(KublingURL.CONNECTION.USER_NAME);
                }
                setPassword(oldPassword);
            }
        }
    }

    public void abort(Executor executor) throws SQLException {
        if (closed) {
            return;
        }
        //TODO: ensure that threads are released.  In theory they will be since close effectively cancels current executions
        close();
    }

    public int getNetworkTimeout() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public String getSchema() {
        return null;
    }

    public void setNetworkTimeout(Executor executor, int milliseconds)
            throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public void setSchema(String schema) {

    }

    public Properties getPayload() {
        return payload;
    }

    public void setPayload(Properties payload) {
        this.payload = payload;
    }

    public Properties getConnectionProps() {
        return connectionProps;
    }

    void setTransactionXid(XidImpl transactionXid) {
        this.transactionXid = transactionXid;
    }

    public void setInLocalTxn(boolean inLocalTxn) {
        this.inLocalTxn = inLocalTxn;
    }

    public void saveTransactionCharacteristics() {
        this.savedReadOnly = this.readOnly;
        this.savedIsolationLevel = this.transactionIsolation;
    }

    public void restoreTransactionCharacteristics() {
        if (savedReadOnly != null) {
            this.readOnly = savedReadOnly;
            this.transactionIsolation = savedIsolationLevel;
            savedReadOnly = null;
        }
    }

}
