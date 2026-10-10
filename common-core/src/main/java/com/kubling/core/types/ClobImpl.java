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
import com.kubling.core.util.ObjectConverterUtil;
import com.kubling.core.util.ReaderInputStream;
import com.kubling.core.util.SqlUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLException;


/**
 * This object holds a chunk of char data and implements the JDBC Clob interface.
 * This object presents a streaming interface and provides a way to access partial
 * of the Clob data. Connectors can use this object when dealing with large
 * objects.
 */
public class ClobImpl extends BaseLob implements Clob {

    @Serial
    private static final long serialVersionUID = -4868633917511897143L;

    private static final class StringInputStreamFactory extends
            InputStreamFactory {
        String str;

        private StringInputStreamFactory(String str) {
            this.str = str;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(str.getBytes(Streamable.CHARSET));
        }

        @Override
        public Reader getCharacterStream() {
            return new StringReader(str);
        }

        @Override
        public StorageMode getStorageMode() {
            return StorageMode.MEMORY;
        }
    }

    final static class ClobStreamProvider implements
            LobSearchUtil.StreamProvider {
        private final Clob searchstr;

        private ClobStreamProvider(Clob searchstr) {
            this.searchstr = searchstr;
        }

        public InputStream getBinaryStream() throws SQLException {
            final Reader reader = searchstr.getCharacterStream();

            return new InputStream() {
                int currentChar = -1;

                @Override
                public int read() throws IOException {
                    if (currentChar == -1) {
                        currentChar = reader.read();
                        if (currentChar == -1) {
                            return -1;
                        }
                        return currentChar & 0xff;
                    }
                    int result = currentChar & 0xff00;
                    currentChar = -1;
                    return result;
                }
            };
        }
    }

    private long len = -1;

    public ClobImpl() {

    }

    /**
     * Creates a new ClobImpl.  Note that the length is not taken from the {@link InputStreamFactory} since
     * it refers to bytes and not chars.
     */
    public ClobImpl(InputStreamFactory streamFactory, long length) {
        super(streamFactory);
        this.len = length;
    }

    public ClobImpl(final char[] chars) {
        this(new StringInputStreamFactory(new String(chars)), chars.length);
    }

    public ClobImpl(String str) {
        this(new StringInputStreamFactory(str), str.length());
    }

    public InputStream getAsciiStream() throws SQLException {
        return new ReaderInputStream(getCharacterStream(), StandardCharsets.US_ASCII);
    }

    public String getSubString(long pos, int length) throws SQLException {
        if (pos < 1) {
            throw new SQLException(CorePlugin.Util.getString("MMClob_MMBlob.2", pos));
        } else if (pos > length()) {
            return null;
        }
        pos = pos - 1;
        if (length < 0) {
            throw new SQLException(CorePlugin.Util.getString("MMClob_MMBlob.1", length));
        } else if ((pos + length) > length()) {
            length = (int) (length() - pos);
        }
        Reader in = getCharacterStream();
        try {
            try {
                long skipped;
                while (pos > 0) {
                    skipped = in.skip(pos);
                    pos -= skipped;
                }
                return ObjectConverterUtil.convertToString(in, length);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Returns the number of characters in the <code>CLOB</code> value
     * designated by this <code>Clob</code> object.
     *
     * @return length of the <code>CLOB</code> in characters
     */
    public long length() throws SQLException {
        if (len == -1) {
            long length = 0;
            Reader r = new BufferedReader(getCharacterStream());
            try {
                while (r.read() != -1) {
                    length++;
                }
            } catch (IOException e) {
                throw new SQLException(e);
            } finally {
                try {
                    r.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
            this.len = length;
        }
        return len;
    }

    /**
     * Determines the character position at which the specified
     * <code>Clob</code> object <code>searchstr</code> appears in this
     * <code>Clob</code> object.  The search begins at position
     * <code>start</code>.
     *
     * @param searchstr the <code>Clob</code> object for which to search
     * @param start     the position at which to begin searching; the first
     *                  position is 1
     * @return the position at which the <code>Clob</code> object appears,
     * else -1; the first position is 1
     */
    public long position(final Clob searchstr, long start) throws SQLException {
        if (searchstr == null) {
            return -1;
        }

        return LobSearchUtil.position(new ClobStreamProvider(searchstr), searchstr.length(), new ClobStreamProvider(this), this.length(), start, 2);
    }

    /**
     * Determines the character position at which the specified substring
     * <code>searchstr</code> appears in the SQL <code>CLOB</code> value
     * represented by this <code>Clob</code> object.  The search
     * begins at position <code>start</code>.
     *
     * @param searchstr the substring for which to search
     * @param start     the position at which to begin searching; the first position
     *                  is 1
     * @return the position at which the substring appears, else -1; the first
     * position is 1
     * @throws SQLException if there is an error accessing the
     *                      <code>CLOB</code> value
     */
    public long position(String searchstr, long start) throws SQLException {
        if (searchstr == null) {
            return -1;
        }
        return position(new ClobImpl(searchstr), start);
    }

    public Reader getCharacterStream(long arg0, long arg1) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public OutputStream setAsciiStream(long arg0) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public Writer setCharacterStream(long arg0) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public int setString(long arg0, String arg1) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public int setString(long arg0, String arg1, int arg2, int arg3)
            throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public void truncate(long arg0) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public static Clob createClob(char[] chars) {
        return new ClobImpl(chars);
    }

    @Override
    public String toString() {
        try {
            if (this.len == 0) return null;

            InputStream in = getAsciiStream();
            byte[] data = in.readAllBytes();

            if (data.length == 0) {
                throw new IOException("Ascii stream returned zero bytes; possibly reused, closed, or pointing to invalid storage");
            }

            return new String(data, StandardCharsets.US_ASCII);

        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to read Clob content", e);
        }
    }


}
