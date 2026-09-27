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

import com.kubling.core.KublingException;
import com.kubling.net.KublingURL;
import com.kubling.net.socket.OioObjectChannelFactory;
import com.kubling.net.socket.SocketServerConnection;
import com.kubling.net.socket.SocketServerConnectionFactory;

import java.util.Properties;


/**
 * <p> The java.sql.DriverManager class uses this class to connect to Kubling Server.
 * The KublingDriver class has a static initializer, which
 * is used to instantiate and register itself with java.sql.DriverManager. The
 * DriverManager's <code>getConnection</code> method calls <code>connect</code>
 * method on available registered drivers.
 */

final class SocketProfile implements ConnectionProfile {


    /**
     * This method tries to make a connection to the given URL. This class
     * will return a null if this is not the right driver to connect to the given URL.
     *
     * @param url used to establish a connection.
     * @return Connection object created
     * @throws KublingSQLException if it is unable to establish a connection to the server.
     */
    public ConnectionImpl connect(String url, Properties info) throws KublingSQLException {

        int loginTimeoutSeconds = 0;
        SocketServerConnection serverConn;
        try {
            String timeout = info.getProperty(KublingURL.CONNECTION.LOGIN_TIMEOUT);
            if (timeout != null) {
                loginTimeoutSeconds = Integer.parseInt(timeout);
            }

            if (loginTimeoutSeconds > 0) {
                OioObjectChannelFactory.TIMEOUTS.set(System.currentTimeMillis() +
                        Integer.valueOf(loginTimeoutSeconds * 1000).longValue());
            }
            serverConn = SocketServerConnectionFactory.getInstance(info).getConnection(info);
        } catch (KublingException e) {
            throw KublingSQLException.create(e);
        } finally {
            if (loginTimeoutSeconds > 0) {
                OioObjectChannelFactory.TIMEOUTS.remove();
            }
        }

        // construct a MMConnection object.
        return new ConnectionImpl(serverConn, info, url);
    }

}
