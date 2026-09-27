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

package com.kubling.net.socket;

import com.kubling.client.security.ILogon;
import com.kubling.client.security.InvalidSessionException;
import com.kubling.client.security.LogonException;
import com.kubling.client.security.LogonResult;
import com.kubling.client.util.ExceptionUtil;
import com.kubling.client.util.ResultsFuture;
import com.kubling.core.KublingComponentException;
import com.kubling.core.KublingException;
import com.kubling.core.util.PropertiesUtils;
import com.kubling.jdbc.JDBCPlugin;
import com.kubling.net.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a client connection that maintains session state and allows for service fail over.
 * Implements a sticky random selection policy.
 */
public class SocketServerConnection implements ServerConnection {

    private static final int FAILOVER_PING_INTERVAL = 1000;
    private final SocketServerInstanceFactory connectionFactory;
    private final UrlServerDiscovery serverDiscovery;
    private static final Logger log = Logger.getLogger("com.kubling.client.sockets");

    private final boolean secure;
    private final Properties connProps;

    private SocketServerInstance serverInstance;
    private LogonResult logonResult;
    private final ILogon logon;
    private boolean closed;
    private boolean failOver;
    private long lastPing = System.currentTimeMillis();
    private int pingFailOverInterval = FAILOVER_PING_INTERVAL;
    private final String serverVersion;

    public SocketServerConnection(
            SocketServerInstanceFactory connectionFactory, boolean secure,
            UrlServerDiscovery serverDiscovery, Properties connProps)
            throws CommunicationException, ConnectionException {
        this.connectionFactory = connectionFactory;
        this.serverDiscovery = serverDiscovery;
        this.connProps = connProps;
        this.secure = secure;
        //ILogon that is allowed to failover
        this.logon = this.getService(ILogon.class);
        this.failOver = Boolean.parseBoolean(connProps.getProperty(KublingURL.CONNECTION.AUTO_FAILOVER));
        this.serverVersion = selectServerInstance().getServerVersion();
    }

    /**
     * Implements a sticky random selection policy
     * TODO: make this customizable
     * TODO: put more information on hostinfo as to process response time, last successful connect, etc.
     */
    public synchronized SocketServerInstance selectServerInstance()
            throws CommunicationException, ConnectionException {
        if (closed) {
            throw new CommunicationException(JDBCPlugin.Event.KBL20016, JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20016));
        }
        if (this.serverInstance != null && (!failOver || this.serverInstance.isOpen())) {
            return this.serverInstance;
        }
        List<HostInfo> hostKeys = new ArrayList<>(this.serverDiscovery.getKnownHosts());
        closeServerInstance();
        List<HostInfo> hostCopy = new ArrayList<>(hostKeys);
        int knownHosts = hostKeys.size();
        while (!hostKeys.isEmpty()) {
            HostInfo hostInfo = this.serverDiscovery.selectNextInstance(hostKeys);

            Exception ex;
            try {
                if (!hostInfo.isResolved()) {
                    InetAddress inetAddress = hostInfo.getInetAddress();
                    if (!hostInfo.isResolved()) {
                        //create a resolved version
                        hostInfo = new HostInfo(hostInfo.getHostName(),
                                new InetSocketAddress(inetAddress, hostInfo.getPortNumber()));
                    }
                }
                hostInfo.setSsl(secure);
                this.serverInstance = connectionFactory.getServerInstance(hostInfo);
                connectSession();
                return this.serverInstance;
            } catch (IOException | SingleInstanceCommunicationException e) {
                ex = e;
            }
            if (knownHosts == 1) { //just a single host, use the exception
                if (ex instanceof UnknownHostException) {
                    throw new SingleInstanceCommunicationException(JDBCPlugin.Event.KBL20019, ex,
                            JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20019, hostInfo.getHostName()));
                }
                throw new SingleInstanceCommunicationException(JDBCPlugin.Event.KBL20020, ex,
                        JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20020, hostInfo.getHostName(),
                                String.valueOf(hostInfo.getPortNumber()), ex.getMessage()));
            }
            log.log(Level.FINE, "Unable to connect to host", ex);
        }
        throw new CommunicationException(JDBCPlugin.Event.KBL20021,
                JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20021, hostCopy.toString()));
    }

    private void logon(ILogon newLogon) throws LogonException, KublingComponentException, CommunicationException {

        SocketServerInstance instance = this.serverInstance;

        updateConnectionProperties(connProps, instance.getLocalAddress(), true, this.connectionFactory);

        LogonResult newResult = newLogon.logon(connProps);

        AuthenticationType type = (AuthenticationType) newResult.getProperty(ILogon.AUTH_TYPE);

        if (type != null) {
            //server has issued an additional challenge
            throw new LogonException(JDBCPlugin.Event.KBL20034, JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20034, type));
        }

        logoff();

        this.logonResult = newResult;
    }

    public static void updateConnectionProperties(
            Properties connectionProperties, InetAddress addr, boolean setMac, HostnameResolver resolver) {
        if (addr == null) {
            return;
        }
        String address = addr.getHostAddress();
        Object old = connectionProperties.put(KublingURL.CONNECTION.CLIENT_IP_ADDRESS, address);
        if (!address.equals(old)) {
            String hostname = null;
            if (resolver != null) {
                hostname = resolver.resolveHostname(addr);
            }
            if (hostname == null) {
                hostname = "unknown";
            }
            connectionProperties.put(KublingURL.CONNECTION.CLIENT_HOSTNAME, hostname);
            if (setMac) {
                try {
                    NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
                    if (ni != null && ni.getHardwareAddress() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : ni.getHardwareAddress()) {
                            sb.append(PropertiesUtils.toHex(b >> 4));
                            sb.append(PropertiesUtils.toHex(b));
                        }
                        connectionProperties.put(KublingURL.CONNECTION.CLIENT_MAC, sb.toString());
                    }
                } catch (SocketException e) {
                    connectionProperties.remove(KublingURL.CONNECTION.CLIENT_MAC);
                }
            } else {
                connectionProperties.remove(KublingURL.CONNECTION.CLIENT_MAC);
            }
        }
    }

    private ILogon connectSession() throws CommunicationException, ConnectionException {
        ILogon newLogon = this.serverInstance.getService(ILogon.class);
        if (this.logonResult != null) {
            try {
                newLogon.assertIdentity(logonResult.getSessionToken());
            } catch (KublingException e) {
                // session is no longer valid
                disconnect();
            }
        }
        if (this.logonResult == null) {
            try {
                logon(newLogon);
            } catch (LogonException e) {
                // Propagate the original message as it contains the message we want
                // to give to the user
                throw new ConnectionException(e);
            } catch (KublingComponentException e) {
                if (e.getCause() instanceof CommunicationException) {
                    throw (CommunicationException) e.getCause();
                }
                throw new CommunicationException(JDBCPlugin.Event.KBL20018, e,
                        JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20018));
            }
        }
        return newLogon;
    }

    public <T> T getService(Class<T> iface) {
        return iface.cast(Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{iface},
                new SocketServerInstanceImpl.RemoteInvocationHandler(iface,
                        PropertiesUtils.getBooleanProperty(connProps, KublingURL.CONNECTION.ENCRYPT_REQUESTS, false)) {
                    @Override
                    protected SocketServerInstance getInstance() throws CommunicationException {
                        if (failOver && System.currentTimeMillis() - lastPing > pingFailOverInterval) {
                            try {
                                ResultsFuture<?> future = selectServerInstance().getService(ILogon.class).ping();
                                future.get();
                            } catch (SingleInstanceCommunicationException e) {
                                closeServerInstance();
                            } catch (CommunicationException e) {
                                throw e;
                            } catch (InvalidSessionException e) {
                                disconnect();
                                closeServerInstance();
                            } catch (Exception e) {
                                closeServerInstance();
                            }
                        }
                        lastPing = System.currentTimeMillis();
                        try {
                            return selectServerInstance();
                        } catch (ConnectionException e) {
                            throw new CommunicationException(e);
                        }
                    }

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {
                        try {
                            return super.invoke(proxy, method, args);
                        } catch (Exception e) {
                            if (ExceptionUtil.getExceptionOfType(e, InvalidSessionException.class) != null) {
                                disconnect();
                            }
                            throw e;
                        }
                    }

                }));
    }

    public synchronized void close() {
        if (this.closed) {
            return;
        }

        if (this.serverInstance != null) {
            logoff();
            closeServerInstance();
        }

        this.closed = true;
    }

    private void logoff() {
        if (this.logonResult == null) {
            return;
        }
        disconnect();
        try {
            //make the best effort to send the logoff, and wait a small amount for a response
            //this will ensure a "proper" session termination on the server side,
            //rather than implicit closure when the socket is closed
            Future<?> writeFuture = this.serverInstance.getService(ILogon.class).logoff();
            writeFuture.get(100, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            //ignore
        }
    }

    private void disconnect() {
        this.logonResult = null;
    }

    private synchronized ResultsFuture<?> isOpen()
            throws CommunicationException, InvalidSessionException, KublingComponentException {
        if (this.closed) {
            throw new CommunicationException(JDBCPlugin.Event.KBL20023, JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20023));
        }
        return logon.ping();
    }

    public boolean isOpen(long msToTest) {
        try {
            ResultsFuture<?> future = isOpen();
            future.get(msToTest, TimeUnit.MILLISECONDS);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    public LogonResult getLogonResult() {
        return logonResult;
    }

    synchronized void closeServerInstance() {
        if (this.serverInstance != null) {
            this.serverInstance.shutdown();
            this.serverInstance = null;
        }
    }

    public boolean isSameInstance(ServerConnection otherService) throws CommunicationException {
        if (!(otherService instanceof SocketServerConnection)) {
            return false;
        }
        try {
            return selectServerInstance().getHostInfo()
                    .equals(((SocketServerConnection) otherService).selectServerInstance().getHostInfo());
        } catch (ConnectionException e) {
            throw new CommunicationException(e);
        }
    }

    public void setFailOver(boolean failOver) {
        this.failOver = failOver;
    }

    public void setFailOverPingInterval(int pingFailOverInterval) {
        this.pingFailOverInterval = pingFailOverInterval;
    }

    @Override
    public void authenticate() throws ConnectionException,
            CommunicationException {
        if (this.serverInstance == null) {
            selectServerInstance(); //this will trigger a logon with the new credentials
        } else {
            ILogon logonInstance = this.serverInstance.getService(ILogon.class);
            try {
                this.logon(logonInstance);
            } catch (LogonException e) {
                throw new ConnectionException(e);
            } catch (KublingComponentException e) {
                throw new CommunicationException(e);
            }
        }
    }

    @Override
    public boolean supportsContinuous() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public String getServerVersion() {
        return this.serverVersion;
    }
}
