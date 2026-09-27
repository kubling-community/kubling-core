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

import com.kubling.core.util.ObjectInputStreamWithClassloader;
import com.kubling.jdbc.JDBCPlugin;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 * @version $Rev: 381 $, $Date: 2008-10-01 06:06:18 -0500 (Wed, 01 Oct 2008) $
 */
public class CompactObjectInputStream extends ObjectInputStream {

    private final ClassLoader classLoader;

    CompactObjectInputStream(InputStream in) throws IOException {
        this(in, null);
    }

    public CompactObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
        super(in);
        this.classLoader = classLoader;
    }

    @Override
    protected void readStreamHeader() throws IOException {
        int version = readByte() & 0xFF;
        if (version != STREAM_VERSION) {
            throw new StreamCorruptedException(
                    "Unsupported version: " + version);
        }
    }

    @Override
    protected ObjectStreamClass readClassDescriptor()
            throws IOException, ClassNotFoundException {
        int type = read();
        if (type < 0) {
            throw new EOFException();
        }
        Class<?> clazz;
        switch (type) {
            case CompactObjectOutputStream.TYPE_PRIMITIVE:
                return super.readClassDescriptor();
            case CompactObjectOutputStream.TYPE_NON_PRIMITIVE:
                String className = readUTF();
                if (classLoader == null) {
                    clazz = Class.forName(
                            className, true,
                            CompactObjectInputStream.class.getClassLoader());
                } else {
                    clazz = Class.forName(className, true, classLoader);
                }
                return ObjectStreamClass.lookupAny(clazz);
            default:
                clazz = CompactObjectOutputStream.KNOWN_CODES.get(type);
                if (clazz != null) {
                    return ObjectStreamClass.lookupAny(clazz);
                }
                throw new StreamCorruptedException(
                        "Unexpected class descriptor type: " + type);
        }
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();

        try {
            ObjectInputStreamWithClassloader.checkClass(name);
        } catch (ClassNotFoundException e) {
            Logger.getLogger("com.kubling").log(Level.SEVERE, JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20037, name));
            throw e;
        }

        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException ex) {
            return super.resolveClass(desc);
        }
    }

}
