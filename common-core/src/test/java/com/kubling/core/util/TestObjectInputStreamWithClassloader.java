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

package com.kubling.core.util;

import com.kubling.core.KublingException;
import com.kubling.core.types.DataTypeManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestObjectInputStreamWithClassloader {

    private static final class RegisteredType {
    }

    private static final class UnregisteredType {
    }

    private static final class BlockedSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private static class RegisteredSerializableBase implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    private static final class RegisteredSerializableType extends RegisteredSerializableBase {
        private static final long serialVersionUID = 1L;
    }

    @Test
    public void testRegisterSafeClass() {
        assertThrows(NullPointerException.class,
                () -> ObjectInputStreamWithClassloader.registerSafeClass(null));

        ObjectInputStreamWithClassloader.registerSafeClass(RegisteredType.class);
        assertDoesNotThrow(() -> ObjectInputStreamWithClassloader.checkClass(RegisteredType.class.getName()));
        assertThrows(ClassNotFoundException.class,
                () -> ObjectInputStreamWithClassloader.checkClass(UnregisteredType.class.getName()));
    }

    @Test
    public void testRegistrationIncludesSerializableHierarchy() throws Exception {
        ObjectInputStreamWithClassloader.registerSafeClass(RegisteredSerializableType.class);

        assertDoesNotThrow(() -> ObjectInputStreamWithClassloader.checkClass(
                RegisteredSerializableBase.class.getName()));
        assertEquals(RegisteredSerializableType.class,
                roundTrip(new RegisteredSerializableType()).getClass());
    }

    @Test
    public void testNativeTypeSerializableHierarchiesAreSafe() {
        for (Class<?> type : DataTypeManager.getAllDataTypeClasses()) {
            assertSafeSerializableHierarchy(type);
        }
    }

    @Test
    public void testRejectedClassDoesNotReachFallback() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new BlockedSerializable());
        }

        try (ObjectInputStreamWithClassloader in = new ObjectInputStreamWithClassloader(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader())) {
            assertThrows(ClassNotFoundException.class, in::readObject);
        }
    }

    @Test
    public void testExistingSafeTypes() {
        Class<?>[] safeTypes = {
                int[].class,
                int[][].class,
                String[][].class,
                ArrayList.class,
                LinkedList.class,
                HashSet.class,
                LinkedHashSet.class,
                TreeSet.class,
                HashMap.class,
                LinkedHashMap.class,
                TreeMap.class,
                Properties.class,
                Hashtable.class,
                Arrays.asList("value").getClass(),
                Collections.emptyList().getClass(),
                Exception.class,
                KublingException.class
        };

        for (Class<?> safeType : safeTypes) {
            assertDoesNotThrow(() -> ObjectInputStreamWithClassloader.checkClass(safeType.getName()),
                    safeType.getName());
        }
    }

    @Test
    public void testNonEmptyPropertiesRoundTrip() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", "test-user");
        properties.setProperty("applicationName", "native-logon");

        assertEquals(properties, roundTrip(properties));
    }

    @Test
    public void testNativeTemporalValuesRoundTrip() throws Exception {
        Object[] values = {
                new java.util.Date(1_700_000_000_123L),
                new java.sql.Date(1_700_000_000_123L),
                new Time(1_700_000_000_123L),
                new Timestamp(1_700_000_000_123L)
        };

        assertArrayEquals(values, (Object[]) roundTrip(values));
    }

    @Test
    public void testNonEmptyAllowedCollectionsRoundTrip() throws Exception {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("key", "value");
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("key", "value");
        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("key", "value");
        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("key", "value");

        Object[] containers = {
                new ArrayList<>(Collections.singletonList("value")),
                new LinkedList<>(Collections.singletonList("value")),
                new HashSet<>(Collections.singleton("value")),
                new LinkedHashSet<>(Collections.singleton("value")),
                new TreeSet<>(Collections.singleton("value")),
                hashMap,
                linkedHashMap,
                treeMap,
                hashtable
        };

        for (Object container : containers) {
            assertEquals(container, roundTrip(container), container.getClass().getName());
        }
    }

    @Test
    public void testContainerInternalArrayIsAllowed() {
        ObjectInputFilter.FilterInfo filterInfo = new ObjectInputFilter.FilterInfo() {
            @Override
            public Class<?> serialClass() {
                return Map.Entry[].class;
            }

            @Override
            public long arrayLength() {
                return 1;
            }

            @Override
            public long depth() {
                return 1;
            }

            @Override
            public long references() {
                return 1;
            }

            @Override
            public long streamBytes() {
                return 0;
            }
        };

        assertEquals(ObjectInputFilter.Status.ALLOWED,
                ObjectInputStreamWithClassloader.safeClassFilter().checkInput(filterInfo));
        assertThrows(ClassNotFoundException.class,
                () -> ObjectInputStreamWithClassloader.checkClass(Map.Entry[].class.getName()));
    }

    @Test
    public void testAllowedContainerStillRejectsBlockedElement() throws Exception {
        ArrayList<Object> values = new ArrayList<>();
        values.add(new BlockedSerializable());

        assertThrows(ClassNotFoundException.class, () -> roundTrip(values));
    }

    private Object roundTrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStreamWithClassloader in = new ObjectInputStreamWithClassloader(
                new ByteArrayInputStream(bytes.toByteArray()), getClass().getClassLoader())) {
            return in.readObject();
        }
    }

    private void assertSafeSerializableHierarchy(Class<?> type) {
        Class<?> current = type;
        do {
            Class<?> safeType = current;
            assertDoesNotThrow(() -> ObjectInputStreamWithClassloader.checkClass(safeType.getName()),
                    safeType.getName());
            current = current.getSuperclass();
        } while (current != null && Serializable.class.isAssignableFrom(current));
    }
}
