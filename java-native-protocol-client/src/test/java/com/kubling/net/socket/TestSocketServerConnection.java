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
import com.kubling.client.security.LogonResult;
import com.kubling.client.security.SessionToken;
import com.kubling.client.util.ResultsFuture;
import com.kubling.core.KublingComponentException;
import com.kubling.core.crypto.NullCryptor;
import com.kubling.net.CommunicationException;
import com.kubling.net.ConnectionException;
import com.kubling.net.HostInfo;
import com.kubling.net.KublingURL;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <code>TestCase</case> for <code>SocketServerConnection</code>
 *
 * @see SocketServerConnection
 */
@SuppressWarnings("nls")
public class TestSocketServerConnection {

    private static final class FakeILogon implements ILogon {

        Throwable t;

        public FakeILogon(Throwable t) {
            this.t = t;
        }

        @Override
        public void assertIdentity(SessionToken sessionId) {

        }

        @Override
        public ResultsFuture<?> logoff() {
            return null;
        }

        @Override
        public LogonResult logon(
                Properties connectionProperties) {
            return new LogonResult(new SessionToken(1,
                    connectionProperties.getProperty(KublingURL.CONNECTION.USER_NAME, "fooUser")),
                    "foo", "fake");
        }

        @Override
        public ResultsFuture<?> ping()
                throws KublingComponentException, CommunicationException {
            if (t != null) {
                if (t instanceof CommunicationException ce) {
                    t = null;
                    throw ce;
                }
                KublingComponentException e = new KublingComponentException(t);
                t = null;
                throw e;
            }
            return ResultsFuture.NULL_FUTURE;
        }

        @Override
        public ResultsFuture<?> ping(Collection<String> sessions)
                throws KublingComponentException, CommunicationException {
            return ping();
        }

    }

    /**
     * Validate that the client host name and IP address property in
     * the connection properties object is set after a <code>SocketServerConnection</code>
     * is established.
     *
     * <p>The expected results contains the host name and IP address
     * of the local machine as returned by <code>NetUtils</code>.
     * These values are not put into the initial connection object
     * and it is up to <code>SocketServerConnection</code> to place
     * the values into the connection properties object during the
     * connection process.
     *
     * @since Westport
     */
    @Test
    public void testSocketServerConnection_PropertiesClientHost() throws Throwable {
        Properties p = new Properties();

        SocketServerConnection.updateConnectionProperties(p, InetAddress.getLocalHost(), true, null);

        assertTrue(p.containsKey(KublingURL.CONNECTION.CLIENT_HOSTNAME));
        assertTrue(p.containsKey(KublingURL.CONNECTION.CLIENT_IP_ADDRESS));
    }

    @Test
    public void testLogonFailsWithMultipleHosts() throws Exception {
        Properties p = new Properties();
        SocketServerInstanceFactory instanceFactory = Mockito.mock(SocketServerInstanceFactory.class);
        Mockito.when(instanceFactory.getServerInstance(Mockito.any()))
                .thenThrow(new SingleInstanceCommunicationException());
        UrlServerDiscovery discovery = new UrlServerDiscovery(new KublingURL("mm://host1:1,host2:2"));
        try {
            new SocketServerConnection(instanceFactory, false, discovery, p);
            fail("exception expected");
        } catch (CommunicationException e) {
            assertEquals("KBL20021 No valid host available. " +
                    "Attempted connections to: [host1:1, host2:2]", e.getMessage());
        }
    }

    @Test
    public void testLogon() throws Exception {
        SocketServerConnection connection = createConnection(null);
        assertEquals(String.valueOf(1), connection.getLogonResult().getSessionID());
    }

    @Test
    public void testChangeUser() throws Exception {
        Properties p = new Properties();
        SocketServerConnection connection = createConnection(null, p);
        assertEquals("fooUser", connection.getLogonResult().getUserName());
        p.setProperty(KublingURL.CONNECTION.USER_NAME, "newUser");
        connection.authenticate();
        assertEquals("newUser", connection.getLogonResult().getUserName());
    }

    /**
     * Since the original instance is still open, this will be a transparent retry
     */
    @Test
    public void testRetry() throws Exception {
        SocketServerConnection connection = createConnection(new SingleInstanceCommunicationException());
        connection.setFailOver(true);
        connection.setFailOverPingInterval(50);
        ILogon logon = connection.getService(ILogon.class);
        Thread.sleep(70);
        logon.ping();
    }

    @Test
    public void testImmediateFail() throws Exception {
        SocketServerConnection connection = createConnection(new CommunicationException());
        ILogon logon = connection.getService(ILogon.class);
        assertThrows(CommunicationException.class, logon::ping);
    }

    @Test
    public void testImmediateFail1() throws Exception {
        SocketServerConnection connection = createConnection(new CommunicationException());
        connection.setFailOver(true);
        ILogon logon = connection.getService(ILogon.class);
        assertThrows(CommunicationException.class, logon::ping);
    }

    private SocketServerConnection createConnection(final Throwable throwException)
            throws CommunicationException, ConnectionException {
        return createConnection(throwException, new Properties());
    }

    private SocketServerConnection createConnection(final Throwable throwException, Properties p)
            throws CommunicationException, ConnectionException {
        return createConnection(throwException, new HostInfo("0.0.0.2", 1), p);
    }

    private SocketServerConnection createConnection(final Throwable t, final HostInfo hostInfo, Properties p)
            throws CommunicationException, ConnectionException {
        UrlServerDiscovery discovery =
                new UrlServerDiscovery(new KublingURL(hostInfo.getHostName(), hostInfo.getPortNumber(), false));
        SocketServerInstanceFactory instanceFactory = new SocketServerInstanceFactory() {
            final FakeILogon logon = new FakeILogon(t);

            @Override
            public SocketServerInstance getServerInstance(HostInfo info) {
                SocketServerInstance instance = Mockito.mock(SocketServerInstance.class);
                Mockito.when(instance.getCryptor()).thenReturn(new NullCryptor());
                Mockito.when(instance.getHostInfo()).thenReturn(hostInfo);
                Mockito.when(instance.getService(ILogon.class)).thenReturn(logon);
                Mockito.when(instance.getServerVersion()).thenReturn("07.03");
                if (t != null) {
                    try {
                        Mockito.doAnswer((Answer<Void>) invocation -> {
                            if (logon.t == null) {
                                return null;
                            }
                            throw logon.t;
                        }).when(instance).send(Mockito.any(), Mockito.any(), Mockito.any());
                    } catch (Exception e) {

                    }
                }
                Mockito.when(instance.isOpen()).thenReturn(true);
                return instance;
            }

            @Override
            public String resolveHostname(InetAddress addr) {
                return null;
            }

        };
        return new SocketServerConnection(instanceFactory, false, discovery, p);
    }

    @Test
    public void testIsSameInstance() throws Exception {
        SocketServerConnection conn = createConnection(null, new HostInfo("0.0.0.0", 1), new Properties());
        SocketServerConnection conn1 = createConnection(null, new HostInfo("0.0.0.1", 1), new Properties());

        assertFalse(conn.isSameInstance(conn1));
        assertTrue(conn.isSameInstance(conn));
    }

}
