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
import com.kubling.net.ServerConnection;

import java.util.Properties;

public interface LocalProfile extends ConnectionProfile {

    String USE_CALLING_THREAD = "useCallingThread";
    String WAIT_FOR_LOAD = "waitForLoad";
    String TRANSPORT_NAME = "transportName";
    Object DQP_WORK_CONTEXT = "dqpWorkContext";
    Object SSL_SESSION = "sslSession";

    ServerConnection createServerConnection(Properties info) throws KublingException;

}
