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

import com.kubling.client.DQP;
import com.kubling.client.security.InvalidSessionException;
import com.kubling.client.util.ExceptionUtil;
import com.kubling.client.xa.XATransactionException;
import com.kubling.core.KublingComponentException;
import com.kubling.core.KublingRuntimeException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SuppressWarnings("nls")
public class TestSocketServiceRegistry {

    interface Foo {
        void someMethod();
    }

    @Test
    public void testExceptionConversionNoException() throws Exception {

        Method m = Foo.class.getMethod("someMethod");

        Throwable t = ExceptionUtil.convertException(m, new KublingComponentException());

        assertInstanceOf(KublingRuntimeException.class, t);
    }

    @Test
    public void testComponentExceptionConversion() throws Exception {

        Method m = DQP.class.getMethod("getMetadata", Long.TYPE);

        Throwable t = ExceptionUtil.convertException(m, new NullPointerException());

        assertInstanceOf(KublingComponentException.class, t);
    }

    @Test
    public void testXATransactionExceptionConversion() throws Exception {

        Method m = DQP.class.getMethod("recover", Integer.TYPE);

        Throwable t = ExceptionUtil.convertException(m, new KublingComponentException());

        assertInstanceOf(XATransactionException.class, t);
    }

    @Test
    public void testSubclass() throws Exception {

        Method m = DQP.class.getMethod("getMetadata", Long.TYPE);

        Throwable t = ExceptionUtil.convertException(m, new InvalidSessionException());

        assertInstanceOf(InvalidSessionException.class, t);
    }
}
