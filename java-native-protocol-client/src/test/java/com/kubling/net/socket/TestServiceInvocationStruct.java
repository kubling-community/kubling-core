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

import com.kubling.core.util.UnitTestUtil;
import com.kubling.core.util.ObjectInputStreamWithClassloader;
import com.kubling.netty.handler.codec.serialization.CompactObjectInputStream;
import com.kubling.netty.handler.codec.serialization.CompactObjectOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestServiceInvocationStruct {

    private interface RegisteredService {
    }

    private interface UnregisteredService {
    }

    @Test
    public void testSerialize() throws Exception {
        ServiceInvocationStruct struct =
                new ServiceInvocationStruct(new Object[]{1, "hello"}, "doSomething", TestServiceInvocationStruct.class);

        ServiceInvocationStruct serialized = UnitTestUtil.helpSerialize(struct);
        assertNotNull(serialized);
        assertInstanceOf(ServiceInvocationStruct.class, serialized);
        assertArrayEquals(struct.args, serialized.args);
        assertEquals(struct.methodName, serialized.methodName);
        assertEquals(struct.targetClass, serialized.targetClass);
    }

    @Test
    public void testRegisteredServiceInterface() throws Exception {
        ObjectInputStreamWithClassloader.registerSafeClass(RegisteredService.class);
        ServiceInvocationStruct struct =
                new ServiceInvocationStruct(new Object[]{1}, "invoke", RegisteredService.class);

        ServiceInvocationStruct result = roundTrip(struct);
        assertEquals(RegisteredService.class, result.targetClass);
    }

    @Test
    public void testUnregisteredServiceInterfaceIsRejected() throws Exception {
        ServiceInvocationStruct struct =
                new ServiceInvocationStruct(new Object[]{1}, "invoke", UnregisteredService.class);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (CompactObjectOutputStream out = new CompactObjectOutputStream(bytes)) {
            out.writeObject(struct);
        }

        try (CompactObjectInputStream in = new CompactObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader())) {
            assertThrows(ClassNotFoundException.class, in::readObject);
        }
    }

    private ServiceInvocationStruct roundTrip(ServiceInvocationStruct value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (CompactObjectOutputStream out = new CompactObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (CompactObjectInputStream in = new CompactObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader())) {
            return (ServiceInvocationStruct) in.readObject();
        }
    }
}
