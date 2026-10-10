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

package com.kubling.core.util;

import com.kubling.core.*;
import com.kubling.core.types.DataTypeManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ObjectInputStreamWithClassloader extends
        ObjectInputStream {

    private final ClassLoader cl;

    private static final Set<String> safeClasses = ConcurrentHashMap.newKeySet();

    static {

        safeClasses.add(java.lang.Number.class.getName());
        safeClasses.add(java.lang.Exception.class.getName());
        safeClasses.add(java.util.Properties.class.getName());
        safeClasses.add(java.util.Hashtable.class.getName());
        safeClasses.add("java.util.Arrays$ArrayList");
        safeClasses.add(Collections.emptyList().getClass().getName());

        safeClasses.add("sun.util.calendar.ZoneInfo");
        safeClasses.add(java.util.TimeZone.class.getName());

        safeClasses.add("com.kubling.net.socket.Handshake");
        safeClasses.add("com.kubling.net.socket.Message");
        safeClasses.add("com.kubling.net.socket.ServiceInvocationStruct");
        safeClasses.add("com.kubling.client.security.ILogon");
        safeClasses.add("com.kubling.client.security.LogonResult");
        safeClasses.add("com.kubling.client.security.SessionToken");
        safeClasses.add("com.kubling.client.DQP");
        safeClasses.add("com.kubling.client.RequestMessage");
        safeClasses.add("com.kubling.client.ResultsMessage");
        safeClasses.add("com.kubling.client.SourceWarning");
        safeClasses.add("com.kubling.client.security.LogonException");
        safeClasses.add("com.kubling.client.security.KublingSecurityException");
        safeClasses.add("com.kubling.client.security.InvalidSessionException");
        safeClasses.add(StackTraceElement.class.getName());

        safeClasses.add("com.kubling.client.metadata.MetadataResult");
        safeClasses.add("com.kubling.client.metadata.ParameterInfo");
        safeClasses.add("com.kubling.client.metadata.ResultsMetadataConstants");

        safeClasses.add("com.kubling.client.xa.XidImpl");
        safeClasses.add("com.kubling.client.xa.XATransactionException");

        safeClasses.add("com.kubling.client.lob.LobChunk");

        safeClasses.addAll(
                DataTypeManager.getAllDataTypeClasses().stream()
                        .map(Class::getName)
                        .collect(Collectors.toSet())
        );

        safeClasses.add(com.kubling.core.types.ClobType.class.getName());
        safeClasses.add(com.kubling.core.types.BaseClobType.class.getName());
        safeClasses.add(com.kubling.core.types.ClobImpl.class.getName());
        safeClasses.add(com.kubling.core.types.BlobType.class.getName());
        safeClasses.add(com.kubling.core.types.BlobImpl.class.getName());
        safeClasses.add(com.kubling.core.types.BaseLob.class.getName());
        safeClasses.add(com.kubling.core.types.XMLType.class.getName());
        safeClasses.add(com.kubling.core.types.SQLXMLImpl.class.getName());
        safeClasses.add(com.kubling.core.types.Streamable.class.getName());
        safeClasses.add(com.kubling.core.types.ArrayImpl.class.getName());
        safeClasses.add(com.kubling.core.types.BinaryType.class.getName());
        safeClasses.add(com.kubling.core.types.NullType.class.getName());
        safeClasses.add(com.kubling.core.types.GeometryType.class.getName());
        safeClasses.add(com.kubling.core.types.GeographyType.class.getName());
        safeClasses.add(com.kubling.core.types.AbstractGeospatialType.class.getName());

        safeClasses.add("com.kubling.client.util.ExceptionHolder");
        safeClasses.add(ExecutionDiagnosticException.class.getName());
        safeClasses.add(KublingRuntimeException.class.getName());
        safeClasses.add(KublingComponentException.class.getName());
        safeClasses.add(KublingException.class.getName());
        safeClasses.add(KublingProcessingException.class.getName());
        safeClasses.add(ComponentNotFoundException.class.getName());

        safeClasses.add(ComponentNotFoundException.class.getName());

    }

    public ObjectInputStreamWithClassloader(
            InputStream in,
            ClassLoader cl) throws IOException {
        super(in);
        this.cl = cl;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {

        //see java bug id 6434149
        checkClass(desc.getName());
        try {
            return Class.forName(desc.getName(), false, cl);
        } catch (ClassNotFoundException e) {
            return super.resolveClass(desc);
        }
    }

    /**
     * Registers an exact class as safe for deserialization. Array types are
     * represented by their component type so the registration does not retain
     * the class or its class loader.
     *
     * @param type the class to register
     * @throws NullPointerException if {@code type} is null
     */
    public static void registerSafeClass(Class<?> type) {
        Objects.requireNonNull(type, "type");
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (!type.isPrimitive()) {
            safeClasses.add(type.getName());
        }
    }

    public static void checkClass(String name) throws ClassNotFoundException {

        if (name.startsWith("[")) {

            if ("BCDFIJSZ".indexOf(name.charAt(1)) >= 0) {
                return; // byte[], char[], double[], etc.
            }
            String elementType = name;
            while (elementType.startsWith("[")) {
                elementType = elementType.substring(1);
            }
            if (elementType.startsWith("L") && elementType.endsWith(";")) {
                elementType = elementType.substring(1, elementType.length() - 1);
            }
            checkClass(elementType);
            return;
        }

        // Allow lists, sets, and maps if they are from trusted packages (raw)
        if (isJavaUtilCollection(name)) {
            return;
        }

        if (!safeClasses.contains(name)) {
            throw new ClassNotFoundException("Blocked class: " + name);
        }
    }

    private static boolean isJavaUtilCollection(String name) {
        return name.equals("java.util.ArrayList")
                || name.equals("java.util.LinkedList")
                || name.equals("java.util.HashSet")
                || name.equals("java.util.LinkedHashSet")
                || name.equals("java.util.TreeSet")
                || name.equals("java.util.HashMap")
                || name.equals("java.util.LinkedHashMap")
                || name.equals("java.util.TreeMap");
    }
}
