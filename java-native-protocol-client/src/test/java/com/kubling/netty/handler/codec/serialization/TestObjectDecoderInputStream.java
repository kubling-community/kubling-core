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

package com.kubling.netty.handler.codec.serialization;

import com.kubling.core.types.ClobImpl;
import com.kubling.core.types.InputStreamFactory;
import com.kubling.core.types.Streamable;
import com.kubling.core.util.AccessibleBufferedInputStream;
import com.kubling.core.util.Assertion;
import com.kubling.core.util.ReaderInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class TestObjectDecoderInputStream {

    @Test
    public void testTimeoutException() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectEncoderOutputStream oeos = new ObjectEncoderOutputStream(new DataOutputStream(baos), 512);
        List<Integer> obj = Arrays.asList(1, 2, 3);
        oeos.writeObject(obj);
        oeos.close();
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        InputStream is = new InputStream() {
            int count;

            @Override
            public int read() throws IOException {
                if (count++ % 2 == 0) {
                    throw new SocketTimeoutException();
                }
                return bais.read();
            }
        };
        ObjectDecoderInputStream odis = new ObjectDecoderInputStream(
                new AccessibleBufferedInputStream(is, 1024), Thread.currentThread().getContextClassLoader(), 1024);
        Object result = null;
        do {
            try {
                result = odis.readObject();
            } catch (IOException ignored) {

            }
        } while (result == null);
        Assertions.assertEquals(obj, result);
    }

    @Test
    public void testReplaceObject() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectEncoderOutputStream out = new ObjectEncoderOutputStream(new DataOutputStream(baos), 512);

        ClobImpl clob = new ClobImpl(new InputStreamFactory() {
            @Override
            public InputStream getInputStream() {
                return new ReaderInputStream(new StringReader("Clob contents"), Charset.forName(Streamable.ENCODING));
            }

        }, -1);

        out.writeObject(clob);

        ObjectDecoderInputStream in = new ObjectDecoderInputStream(new AccessibleBufferedInputStream(new ByteArrayInputStream(baos.toByteArray()), 1024), Thread.currentThread().getContextClassLoader(), 1024);
        Object result = in.readObject();
        Assertion.assertTrue(result instanceof ClobImpl);
    }
}
