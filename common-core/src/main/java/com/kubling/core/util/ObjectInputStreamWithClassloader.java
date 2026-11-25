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
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ObjectInputStreamWithClassloader extends
        ObjectInputStream {

    private final ClassLoader cl;

    private static final Set<String> safeClasses = ConcurrentHashMap.newKeySet();
    private static final ObjectInputFilter safeClassFilter = filterInfo -> {
        Class<?> type = filterInfo.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        if (type == Map.Entry[].class) {
            // ObjectInputStream requests this exact array while rebuilding
            // non-empty JDK map-backed containers. Its contents are filtered
            // independently and Map.Entry is not added to the payload allowlist.
            return ObjectInputFilter.Status.ALLOWED;
        }
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        try {
            if (Proxy.isProxyClass(type)) {
                for (Class<?> interfaceType : type.getInterfaces()) {
                    checkClass(interfaceType.getName());
                }
            } else {
                checkClass(type.getName());
            }
            return ObjectInputFilter.Status.ALLOWED;
        } catch (ClassNotFoundException e) {
            return ObjectInputFilter.Status.REJECTED;
        }
    };

    static {

        registerSafeClass(java.lang.Number.class);
        registerSafeClass(java.lang.Throwable.class);
        registerSafeClass(java.lang.Exception.class);
        registerSafeClass(java.lang.RuntimeException.class);
        registerSafeClass(java.lang.Enum.class);
        registerSafeClass(Proxy.class);
        registerSafeClass(java.util.Properties.class);
        registerSafeClass(java.util.Hashtable.class);
        safeClasses.add("java.util.Arrays$ArrayList");
        registerSafeClass(Collections.emptyList().getClass());

        safeClasses.add("sun.util.calendar.ZoneInfo");
        registerSafeClass(java.util.TimeZone.class);

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
        registerSafeClass(StackTraceElement.class);

        safeClasses.add("com.kubling.client.metadata.MetadataResult");
        safeClasses.add("com.kubling.client.metadata.ParameterInfo");
        safeClasses.add("com.kubling.client.metadata.ResultsMetadataConstants");

        safeClasses.add("com.kubling.client.xa.XidImpl");
        safeClasses.add("com.kubling.client.xa.XATransactionException");

        safeClasses.add("com.kubling.client.lob.LobChunk");

        DataTypeManager.getAllDataTypeClasses()
                .forEach(ObjectInputStreamWithClassloader::registerSafeClass);

        registerSafeClass(com.kubling.core.types.ClobType.class);
        registerSafeClass(com.kubling.core.types.BaseClobType.class);
        registerSafeClass(com.kubling.core.types.ClobImpl.class);
        registerSafeClass(com.kubling.core.types.BlobType.class);
        registerSafeClass(com.kubling.core.types.BlobImpl.class);
        registerSafeClass(com.kubling.core.types.BaseLob.class);
        registerSafeClass(com.kubling.core.types.XMLType.class);
        registerSafeClass(com.kubling.core.types.SQLXMLImpl.class);
        registerSafeClass(com.kubling.core.types.Streamable.class);
        registerSafeClass(com.kubling.core.types.ArrayImpl.class);
        registerSafeClass(com.kubling.core.types.BinaryType.class);
        registerSafeClass(com.kubling.core.types.NullType.class);
        registerSafeClass(com.kubling.core.types.GeometryType.class);
        registerSafeClass(com.kubling.core.types.GeographyType.class);
        registerSafeClass(com.kubling.core.types.AbstractGeospatialType.class);

        safeClasses.add("com.kubling.client.util.ExceptionHolder");
        registerSafeClass(ExecutionDiagnosticException.class);
        registerSafeClass(KublingRuntimeException.class);
        registerSafeClass(KublingComponentException.class);
        registerSafeClass(KublingException.class);
        registerSafeClass(KublingProcessingException.class);
        registerSafeClass(ComponentNotFoundException.class);

    }

    public ObjectInputStreamWithClassloader(
            InputStream in,
            ClassLoader cl) throws IOException {
        super(in);
        this.cl = cl;
        ObjectInputFilter configuredFilter = getObjectInputFilter();
        setObjectInputFilter(configuredFilter == null
                ? safeClassFilter
                : ObjectInputFilter.merge(configuredFilter, safeClassFilter));
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

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces)
            throws IOException, ClassNotFoundException {
        for (String interfaceName : interfaces) {
            checkClass(interfaceName);
        }
        return super.resolveProxyClass(interfaces);
    }

    /**
     * Returns the filter used by protocol object streams. The filter consults
     * the same exact-class registry as {@link #checkClass(String)} on every
     * invocation, including classes registered after the filter is installed.
     *
     * @return the shared safe-class filter
     */
    public static ObjectInputFilter safeClassFilter() {
        return safeClassFilter;
    }

    /**
     * Registers a class and its serializable superclass hierarchy as safe for
     * deserialization. Array types are represented by their component type.
     * Only class names are retained, so registration does not retain class
     * loaders.
     *
     * @param type the class to register
     * @throws NullPointerException if {@code type} is null
     */
    public static void registerSafeClass(Class<?> type) {
        Objects.requireNonNull(type, "type");
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            return;
        }
        safeClasses.add(type.getName());
        for (Class<?> supertype = type.getSuperclass();
             supertype != null && Serializable.class.isAssignableFrom(supertype);
             supertype = supertype.getSuperclass()) {
            safeClasses.add(supertype.getName());
        }
    }

    public static void checkClass(String name) throws ClassNotFoundException {

        if (name.startsWith("[")) {
            String elementType = name;
            while (elementType.startsWith("[")) {
                elementType = elementType.substring(1);
            }
            if (elementType.length() == 1 && "BCDFIJSZ".indexOf(elementType.charAt(0)) >= 0) {
                return; // byte[], char[][], double[][][], etc.
            }
            if (elementType.startsWith("L") && elementType.endsWith(";")) {
                elementType = elementType.substring(1, elementType.length() - 1);
                checkClass(elementType);
                return;
            }
            throw new ClassNotFoundException("Blocked class: " + name);
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
