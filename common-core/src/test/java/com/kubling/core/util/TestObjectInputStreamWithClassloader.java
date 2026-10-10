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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestObjectInputStreamWithClassloader {

    private static final class RegisteredType {
    }

    private static final class UnregisteredType {
    }

    private static final class BlockedSerializable implements Serializable {
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
}
