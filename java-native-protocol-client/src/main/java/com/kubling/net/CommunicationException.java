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

package com.kubling.net;

import com.kubling.core.BundleUtil;
import com.kubling.core.KublingException;

import java.io.Serial;

/**
 * An error occurred in communication between client and server.  This
 * error may or may not be recoverable.  Generally the communication
 * transport should be able to tell the difference and recover if possible.
 */
public class CommunicationException extends KublingException {
    @Serial
    private static final long serialVersionUID = -8352601998078723446L;

    /**
     * No-Arg Constructor
     */
    public CommunicationException() {
        super();
    }

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(Throwable e) {
        super(e);
    }

    public CommunicationException(Throwable e, String message) {
        super(e, message);
    }

    public CommunicationException(BundleUtil.Event event, Throwable t, String message) {
        super(event, t, message);
    }

    public CommunicationException(BundleUtil.Event event, Throwable t) {
        super(event, t);
    }

    public CommunicationException(BundleUtil.Event event, String message) {
        super(event, message);
    }
}
