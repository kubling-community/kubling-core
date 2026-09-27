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

import com.kubling.client.util.ResultsFuture;
import com.kubling.client.util.ResultsReceiver;
import com.kubling.core.crypto.Cryptor;
import com.kubling.net.CommunicationException;
import com.kubling.net.HostInfo;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public interface SocketServerInstance {

    <T> T getService(Class<T> iface);

    void shutdown();

    HostInfo getHostInfo();

    boolean isOpen();

    Cryptor getCryptor();

    long getSynchTimeout();

    void send(Message message, ResultsReceiver<Object> receiver, Serializable key)
            throws CommunicationException, InterruptedException;

    void read(long timeout, TimeUnit unit, ResultsFuture<?> resultsFuture) throws TimeoutException, InterruptedException;

    String getServerVersion();

    InetAddress getLocalAddress();
}