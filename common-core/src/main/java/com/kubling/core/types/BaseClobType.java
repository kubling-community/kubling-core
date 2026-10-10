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

package com.kubling.core.types;

import com.kubling.core.CorePlugin;
import com.kubling.core.KublingRuntimeException;
import com.kubling.core.util.EquivalenceUtil;
import com.kubling.core.util.HashCodeUtil;
import com.kubling.core.util.ObjectConverterUtil;

import java.io.*;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;


/**
 * This is wrapper on top of a "clob" object, which implements the "java.sql.Clob"
 * interface. This class also implements the Streamable interface
 */
public class BaseClobType extends Streamable<Clob> implements NClob, Sequencable, Comparable<BaseClobType> {

    @Serial
    private static final long serialVersionUID = 2753412502127824104L;

    private int hash;

    public BaseClobType() {
    }

    public BaseClobType(Clob clob) {
        super(clob);
    }

    /**
     * @see Clob#getAsciiStream()
     */
    public InputStream getAsciiStream() throws SQLException {
        return this.reference.getAsciiStream();
    }

    /**
     * @see Clob#getCharacterStream()
     */
    public Reader getCharacterStream() throws SQLException {
        return this.reference.getCharacterStream();
    }

    /**
     * @see Clob#getSubString(long, int)
     */
    public String getSubString(long pos, int len) throws SQLException {
        return this.reference.getSubString(pos, len);
    }

    @Override
    long computeLength() throws SQLException {
        return this.reference.length();
    }

    /**
     * @see Clob#position(Clob, long)
     */
    public long position(Clob searchstr, long start) throws SQLException {
        return this.reference.position(searchstr, start);
    }

    /**
     * @see Clob#position(String, long)
     */
    public long position(String searchstr, long start) throws SQLException {
        return this.reference.position(searchstr, start);
    }

    /**
     * @see Clob#setAsciiStream(long)
     */
    public OutputStream setAsciiStream(long pos) throws SQLException {
        return this.reference.setAsciiStream(pos);
    }

    /**
     * @see Clob#setCharacterStream(long)
     */
    public Writer setCharacterStream(long pos) throws SQLException {
        return this.reference.setCharacterStream(pos);
    }

    /**
     * @see Clob#setString(long, String, int, int)
     */
    public int setString(long pos,
                         String str,
                         int offset,
                         int len) throws SQLException {
        return this.reference.setString(pos, str, offset, len);
    }

    /**
     * @see Clob#setString(long, String)
     */
    public int setString(long pos, String str) throws SQLException {
        return this.reference.setString(pos, str);
    }

    /**
     * @see Clob#truncate(long)
     */
    public void truncate(long len) throws SQLException {
        this.reference.truncate(len);
    }

    /**
     * Utility method to convert to String
     *
     * @return string form of the clob passed.
     */
    public static String getString(Clob clob) throws SQLException, IOException {

        try (Reader reader = clob.getCharacterStream()) {
            StringWriter writer = new StringWriter();
            int c = reader.read();
            while (c != -1) {
                writer.write((char) c);
                c = reader.read();
            }
            String data = writer.toString();
            writer.close();
            return data;
        }
    }

    private final static int CHAR_SEQUENCE_BUFFER_SIZE = 1 << 12;

    public CharSequence getCharSequence() {
        return new CharSequence() {

            private char[] buffer = new char[CHAR_SEQUENCE_BUFFER_SIZE];
            private int bufLength;
            private Reader reader;
            private int beginPosition;

            public int length() {
                long result;
                try {
                    result = BaseClobType.this.length();
                } catch (SQLException err) {
                    throw new KublingRuntimeException(CorePlugin.Event.KBL10051, err);
                }
                if (((int) result) != result) {
                    throw new KublingRuntimeException(CorePlugin.Event.KBL10052, CorePlugin.Util.gs(CorePlugin.Event.KBL10052));
                }
                return (int) result;
            }

            public char charAt(int index) {
                try {
                    if ((reader == null || index < beginPosition) && reader != null) {
                        reader.close();
                        reader = null;
                    }
                    if (buffer == null || index < beginPosition || index >= beginPosition + bufLength) {
                        if (reference instanceof ClobImpl) {
                            if (reader == null) {
                                reader = getCharacterStream();
                            }
                            bufLength = reader.read(buffer, 0, buffer.length);
                        } else {
                            String stringBuffer = BaseClobType.this.getSubString(index + 1, CHAR_SEQUENCE_BUFFER_SIZE);
                            bufLength = stringBuffer.length();
                            buffer = stringBuffer.toCharArray();
                        }
                        beginPosition = index;
                    }
                    return buffer[index - beginPosition];
                } catch (IOException | SQLException err) {
                    throw new KublingRuntimeException(CorePlugin.Event.KBL10053, err);
                }
            }

            public CharSequence subSequence(int start,
                                            int end) {
                try {
                    return BaseClobType.this.getSubString(start + 1, end - start);
                } catch (SQLException err) {
                    throw new KublingRuntimeException(CorePlugin.Event.KBL10054, err);
                }
            }

        };
    }

    public void free() throws SQLException {
        this.reference.free();
    }

    public Reader getCharacterStream(long pos, long len) throws SQLException {
        return this.reference.getCharacterStream(pos, len);
    }

    @Override
    protected void readReference(ObjectInput in) throws IOException {
        char[] chars = new char[(int) length];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = in.readChar();
        }
        this.reference = ClobImpl.createClob(chars);
    }

    /**
     * Since we have the length in chars we'll just write out in double byte format.
     * These clobs should be small, so the wasted space should be minimal.
     */
    @Override
    protected void writeReference(final DataOutput out) throws IOException {
        Writer w = new Writer() {

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                for (int i = off; i < len; i++) {
                    out.writeChar(cbuf[i]);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Reader r;
        try {
            r = getCharacterStream();
        } catch (SQLException e) {
            throw new IOException(e);
        }
        try {
            int chars = ObjectConverterUtil.write(w, r, (int) length, false);
            if (length != chars) {
                throw new IOException("Expected length " + length + " but was " + chars + " for " + this.reference);
            }
        } finally {
            r.close();
        }
    }

    @Override
    public int compareTo(BaseClobType o) {
        try {
            Reader cs1 = this.getCharacterStream();
            Reader cs2 = o.getCharacterStream();
            long len1 = this.length();
            long len2 = o.length();
            long n = Math.min(len1, len2);
            for (long i = 0; i < n; i++) {
                int c1 = cs1.read();
                int c2 = cs2.read();
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
            return Long.signum(len1 - len2);
        } catch (SQLException e) {
            throw new KublingRuntimeException(CorePlugin.Event.KBL10056, e);
        } catch (IOException e) {
            throw new KublingRuntimeException(CorePlugin.Event.KBL10057, e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BaseClobType other)) {
            return false;
        }
        if (EquivalenceUtil.areEqual(reference, other.reference)) {
            return true;
        }
        try {
            if (length() != other.length()) {
                return false;
            }
            return this.compareTo(other) == 0;
        } catch (SQLException | KublingRuntimeException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            if (hash == 0) {
                hash = HashCodeUtil.expHashCode(this.getCharSequence());
            }
            return hash;
        } catch (KublingRuntimeException e) {
            return 0;
        }
    }

}
