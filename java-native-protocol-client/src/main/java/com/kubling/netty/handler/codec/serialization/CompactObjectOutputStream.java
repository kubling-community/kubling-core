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

import com.kubling.client.DQP;
import com.kubling.client.RequestMessage;
import com.kubling.client.ResultsMessage;
import com.kubling.client.lob.LobChunk;
import com.kubling.client.metadata.MetadataResult;
import com.kubling.client.metadata.ParameterInfo;
import com.kubling.client.plan.Annotation;
import com.kubling.client.plan.PlanNode;
import com.kubling.client.security.*;
import com.kubling.client.util.ExceptionHolder;
import com.kubling.client.xa.XATransactionException;
import com.kubling.client.xa.XidImpl;
import com.kubling.core.*;
import com.kubling.core.types.*;
import com.kubling.core.types.InputStreamFactory.StreamFactoryReference;
import com.kubling.core.util.ReaderInputStream;
import com.kubling.jdbc.JDBCPlugin;
import com.kubling.net.socket.Handshake;
import com.kubling.net.socket.Message;
import com.kubling.net.socket.ServiceInvocationStruct;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 */
public class CompactObjectOutputStream extends ObjectOutputStream {

    static final int TYPE_PRIMITIVE = 0;
    static final int TYPE_NON_PRIMITIVE = 1;

    public static final Map<Class<?>, Integer> KNOWN_CLASSES = new HashMap<>();
    public static final Map<Integer, Class<?>> KNOWN_CODES = new HashMap<>();

    private final List<InputStream> streams = new LinkedList<>();
    private final List<StreamFactoryReference> references = new LinkedList<>();

    public static void addKnownClass(Class<?> clazz, byte code) {
        KNOWN_CLASSES.put(clazz, (int) code);
        if (KNOWN_CODES.put((int) code, clazz) != null) {
            throw new KublingRuntimeException(JDBCPlugin.Event.KBL20007, JDBCPlugin.Util.gs(JDBCPlugin.Event.KBL20007));
        }
    }

    static {
        addKnownClass(ServiceInvocationStruct.class, (byte) 2);
        addKnownClass(Handshake.class, (byte) 3);
        addKnownClass(Message.class, (byte) 4);
        addKnownClass(SerializableReader.class, (byte) 5);
        addKnownClass(SerializableInputStream.class, (byte) 6);

        addKnownClass(DQP.class, (byte) 10);
        addKnownClass(LobChunk.class, (byte) 11);
        addKnownClass(RequestMessage.class, (byte) 12);
        addKnownClass(ResultsMessage.class, (byte) 13);
        addKnownClass(PlanNode.class, (byte) 14);
        addKnownClass(PlanNode.Property.class, (byte) 15);
        addKnownClass(Annotation.class, (byte) 16);
        addKnownClass(MetadataResult.class, (byte) 17);
        addKnownClass(ParameterInfo.class, (byte) 18);
        addKnownClass(XidImpl.class, (byte) 19);
        addKnownClass(BlobImpl.class, (byte) 20);
        addKnownClass(ClobImpl.class, (byte) 21);
        addKnownClass(SQLXMLImpl.class, (byte) 22);
        addKnownClass(BlobType.class, (byte) 23);
        addKnownClass(ClobType.class, (byte) 24);
        addKnownClass(XMLType.class, (byte) 25);
        addKnownClass(XATransactionException.class, (byte) 26);

        addKnownClass(ILogon.class, (byte) 30);
        addKnownClass(LogonResult.class, (byte) 31);
        addKnownClass(SessionToken.class, (byte) 32);
        addKnownClass(LogonException.class, (byte) 33);
        addKnownClass(KublingSecurityException.class, (byte) 34);
        addKnownClass(InvalidSessionException.class, (byte) 35);

        addKnownClass(ExceptionHolder.class, (byte) 40);
        addKnownClass(KublingRuntimeException.class, (byte) 41);
        addKnownClass(KublingComponentException.class, (byte) 42);
        addKnownClass(KublingException.class, (byte) 43);
        addKnownClass(KublingProcessingException.class, (byte) 44);
        addKnownClass(ComponentNotFoundException.class, (byte) 45);
    }

    public CompactObjectOutputStream(OutputStream out) throws IOException {
        super(out);
        enableReplaceObject(true);
    }

    public List<InputStream> getStreams() {
        return streams;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        streams.clear();
        references.clear();
    }

    public List<StreamFactoryReference> getReferences() {
        return references;
    }

    @Override
    protected void writeStreamHeader() throws IOException {
        writeByte(STREAM_VERSION);
    }

    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        if (desc.forClass().isPrimitive()
                || !(Externalizable.class.isAssignableFrom(desc.forClass()))) {
            write(TYPE_PRIMITIVE);
            super.writeClassDescriptor(desc);
        } else {
            Integer b = KNOWN_CLASSES.get(desc.forClass());
            if (b != null) {
                write(b);
            } else {
                write(TYPE_NON_PRIMITIVE);
                writeUTF(desc.getName());
            }
        }
    }

    @Override
    protected Object replaceObject(Object obj) throws IOException {
        if (obj instanceof BaseLob) {
            try {
                switch (obj) {
                    case SQLXMLImpl sqlxml -> {
                        streams.add(sqlxml.getBinaryStream());
                        StreamFactoryReference sfr = new SQLXMLImpl();
                        references.add(sfr);
                        return sfr;
                    }
                    case ClobImpl clob -> {
                        streams.add(new ReaderInputStream(clob.getCharacterStream(),
                                Charset.forName(Streamable.ENCODING)));
                        StreamFactoryReference sfr = new ClobImpl();
                        references.add(sfr);
                        return sfr;
                    }
                    case BlobImpl ignored -> {
                        streams.add(((Blob) obj).getBinaryStream());
                        StreamFactoryReference sfr = new BlobImpl();
                        references.add(sfr);
                        return sfr;
                    }
                    default -> {
                    }
                }
            } catch (SQLException e) {
                throw new IOException(e);
            }
        } else if (obj instanceof Serializable) {
            return obj;
        } else {
            try {
                if (obj instanceof Reader) {
                    streams.add(new ReaderInputStream((Reader) obj, Charset.forName(Streamable.ENCODING)));
                    StreamFactoryReference sfr = new SerializableReader();
                    references.add(sfr);
                    return sfr;
                } else if (obj instanceof InputStream) {
                    streams.add((InputStream) obj);
                    StreamFactoryReference sfr = new SerializableInputStream();
                    references.add(sfr);
                    return sfr;
                } else if (obj instanceof SQLXML) {
                    streams.add(((SQLXML) obj).getBinaryStream());
                    StreamFactoryReference sfr = new SQLXMLImpl();
                    references.add(sfr);
                    return sfr;
                } else if (obj instanceof Clob) {
                    streams.add(new ReaderInputStream(((Clob) obj).getCharacterStream(), Charset.forName(Streamable.ENCODING)));
                    StreamFactoryReference sfr = new ClobImpl();
                    references.add(sfr);
                    return sfr;
                } else if (obj instanceof Blob) {
                    streams.add(((Blob) obj).getBinaryStream());
                    StreamFactoryReference sfr = new BlobImpl();
                    references.add(sfr);
                    return sfr;
                }
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
        return super.replaceObject(obj);
    }

    static class SerializableInputStream extends InputStream implements Externalizable, StreamFactoryReference {

        private InputStreamFactory isf;
        private InputStream is;

        public SerializableInputStream() {
        }

        public void setStreamFactory(InputStreamFactory streamFactory) {
            this.isf = streamFactory;
        }

        @Override
        public int read() throws IOException {
            if (is == null) {
                is = isf.getInputStream();
            }
            return is.read();
        }

        @Override
        public void close() throws IOException {
            isf.free();
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
        }
    }

    static class SerializableReader extends Reader implements Externalizable, StreamFactoryReference {

        private InputStreamFactory isf;
        private Reader r;

        public SerializableReader() {
        }

        public void setStreamFactory(InputStreamFactory streamFactory) {
            this.isf = streamFactory;
        }

        @Override
        public void close() throws IOException {
            isf.free();
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (r == null) {
                r = new InputStreamReader(isf.getInputStream(), Streamable.ENCODING);
            }
            return r.read(cbuf, off, len);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException,
                ClassNotFoundException {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
        }
    }

}
