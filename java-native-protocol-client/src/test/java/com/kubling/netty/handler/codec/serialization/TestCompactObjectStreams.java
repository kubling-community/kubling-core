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

package com.kubling.netty.handler.codec.serialization;

import com.kubling.client.DQP;
import com.kubling.client.RequestMessage;
import com.kubling.client.ResultsMessage;
import com.kubling.client.lob.LobChunk;
import com.kubling.client.metadata.MetadataResult;
import com.kubling.client.metadata.ParameterInfo;
import com.kubling.client.plan.Annotation;
import com.kubling.client.plan.PlanNode;
import com.kubling.client.security.ILogon;
import com.kubling.client.security.InvalidSessionException;
import com.kubling.client.security.KublingSecurityException;
import com.kubling.client.security.LogonException;
import com.kubling.client.security.LogonResult;
import com.kubling.client.security.SessionToken;
import com.kubling.client.util.ExceptionHolder;
import com.kubling.client.xa.XATransactionException;
import com.kubling.client.xa.XidImpl;
import com.kubling.core.ComponentNotFoundException;
import com.kubling.core.KublingComponentException;
import com.kubling.core.KublingException;
import com.kubling.core.KublingProcessingException;
import com.kubling.core.KublingRuntimeException;
import com.kubling.core.types.BlobImpl;
import com.kubling.core.types.BlobType;
import com.kubling.core.types.ClobImpl;
import com.kubling.core.types.ClobType;
import com.kubling.core.types.SQLXMLImpl;
import com.kubling.core.types.XMLType;
import com.kubling.core.util.ObjectInputStreamWithClassloader;
import com.kubling.net.socket.Handshake;
import com.kubling.net.socket.Message;
import com.kubling.net.socket.ServiceInvocationStruct;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestCompactObjectStreams {

    private static final class AdditionalKnownType implements Externalizable {
        @Override
        public void writeExternal(ObjectOutput out) {
        }

        @Override
        public void readExternal(ObjectInput in) {
        }
    }

    public static final class InitializationTracker {
        public static boolean initialized;
    }

    private static final class UnregisteredExternalizable implements Externalizable {
        static {
            InitializationTracker.initialized = true;
        }

        @Override
        public void writeExternal(ObjectOutput out) {
        }

        @Override
        public void readExternal(ObjectInput in) {
        }
    }

    private static final class IsolatedClassLoader extends ClassLoader {
        private IsolatedClassLoader() {
            super(TestCompactObjectStreams.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(UnregisteredExternalizable.class.getName())) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> result = findLoadedClass(name);
                if (result == null) {
                    String resource = name.replace('.', '/') + ".class";
                    try (InputStream in = getParent().getResourceAsStream(resource)) {
                        if (in == null) {
                            throw new ClassNotFoundException(name);
                        }
                        byte[] bytes = in.readAllBytes();
                        result = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                if (resolve) {
                    resolveClass(result);
                }
                return result;
            }
        }
    }

    @Test
    public void testPlanNodeAndPropertyRoundTrip() throws Exception {
        PlanNode plan = new PlanNode("root");
        plan.addProperty("values", List.of("one", "two"));

        PlanNode result = roundTrip(plan);
        assertEquals(plan.toXml(), result.toXml());

        PlanNode.Property property = roundTrip(plan.getProperties().getFirst());
        assertEquals("values", property.getName());
        assertEquals(List.of("one", "two"), property.getValues());
    }

    @Test
    public void testAnnotationRoundTrip() throws Exception {
        Annotation annotation = new Annotation(
                Annotation.HINTS, "used hint", "none", Annotation.Priority.HIGH);

        Annotation result = roundTrip(annotation);
        assertEquals(annotation.getCategory(), result.getCategory());
        assertEquals(annotation.getAnnotation(), result.getAnnotation());
        assertEquals(annotation.getResolution(), result.getResolution());
        assertEquals(annotation.getPriority(), result.getPriority());
    }

    @Test
    public void testSerializableStreamWrappersRoundTrip() throws Exception {
        assertInstanceOf(CompactObjectOutputStream.SerializableInputStream.class,
                roundTrip(new CompactObjectOutputStream.SerializableInputStream()));
        assertInstanceOf(CompactObjectOutputStream.SerializableReader.class,
                roundTrip(new CompactObjectOutputStream.SerializableReader()));
    }

    @Test
    public void testAddingKnownClassRegistersItAsSafe() throws Exception {
        CompactObjectOutputStream.addKnownClass(AdditionalKnownType.class, (byte) 100);
        ObjectInputStreamWithClassloader.checkClass(AdditionalKnownType.class.getName());
    }

    @Test
    public void testUnknownDescriptorIsCheckedBeforeInitialization() throws Exception {
        Class<?> writerType = new IsolatedClassLoader().loadClass(UnregisteredExternalizable.class.getName());
        Constructor<?> constructor = writerType.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object value = constructor.newInstance();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (CompactObjectOutputStream out = new CompactObjectOutputStream(bytes)) {
            out.writeObject(value);
        }

        InitializationTracker.initialized = false;
        try (CompactObjectInputStream in = new CompactObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), new IsolatedClassLoader())) {
            InvalidClassException exception = assertThrows(InvalidClassException.class, in::readObject);
            assertInstanceOf(ClassNotFoundException.class, exception.getCause());
        }
        assertFalse(InitializationTracker.initialized);
    }

    @Test
    public void testKnownClassCodesRemainStable() {
        Map<Integer, Class<?>> expected = Map.ofEntries(
                Map.entry(2, ServiceInvocationStruct.class),
                Map.entry(3, Handshake.class),
                Map.entry(4, Message.class),
                Map.entry(5, CompactObjectOutputStream.SerializableReader.class),
                Map.entry(6, CompactObjectOutputStream.SerializableInputStream.class),
                Map.entry(10, DQP.class),
                Map.entry(11, LobChunk.class),
                Map.entry(12, RequestMessage.class),
                Map.entry(13, ResultsMessage.class),
                Map.entry(14, PlanNode.class),
                Map.entry(15, PlanNode.Property.class),
                Map.entry(16, Annotation.class),
                Map.entry(17, MetadataResult.class),
                Map.entry(18, ParameterInfo.class),
                Map.entry(19, XidImpl.class),
                Map.entry(20, BlobImpl.class),
                Map.entry(21, ClobImpl.class),
                Map.entry(22, SQLXMLImpl.class),
                Map.entry(23, BlobType.class),
                Map.entry(24, ClobType.class),
                Map.entry(25, XMLType.class),
                Map.entry(26, XATransactionException.class),
                Map.entry(30, ILogon.class),
                Map.entry(31, LogonResult.class),
                Map.entry(32, SessionToken.class),
                Map.entry(33, LogonException.class),
                Map.entry(34, KublingSecurityException.class),
                Map.entry(35, InvalidSessionException.class),
                Map.entry(40, ExceptionHolder.class),
                Map.entry(41, KublingRuntimeException.class),
                Map.entry(42, KublingComponentException.class),
                Map.entry(43, KublingException.class),
                Map.entry(44, KublingProcessingException.class),
                Map.entry(45, ComponentNotFoundException.class));

        expected.forEach((code, type) -> assertSame(type, CompactObjectOutputStream.KNOWN_CODES.get(code),
                "code " + code));
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (CompactObjectOutputStream out = new CompactObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (CompactObjectInputStream in = new CompactObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()), TestCompactObjectStreams.class.getClassLoader())) {
            return (T) in.readObject();
        }
    }
}
