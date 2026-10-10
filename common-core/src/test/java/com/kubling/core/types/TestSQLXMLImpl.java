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

import com.kubling.core.util.ObjectConverterUtil;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.Reader;
import java.io.Serial;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestSQLXMLImpl {

    String testStr = "<foo>test</foo>";

    @Test
    public void testGetSource() throws Exception {
        SQLXMLImpl xml = new SQLXMLImpl(testStr);
        assertInstanceOf(StreamSource.class, xml.getSource(null));

        StreamSource ss = xml.getSource(null);
        assertEquals(testStr, new String(ObjectConverterUtil.convertToByteArray(ss.getInputStream()), Streamable.ENCODING));
    }

    @Test
    public void testGetCharacterStream() throws Exception {
        SQLXMLImpl xml = new SQLXMLImpl(testStr);
        assertEquals(testStr, ObjectConverterUtil.convertToString(xml.getCharacterStream()));
    }

    @Test
    public void testGetBinaryStream() throws Exception {
        SQLXMLImpl xml = new SQLXMLImpl(testStr);
        assertEquals(testStr, new String(ObjectConverterUtil.convertToByteArray(xml.getBinaryStream()), Streamable.ENCODING));
    }

    @Test
    public void testGetString() throws Exception {
        SQLXMLImpl xml = new SQLXMLImpl(testStr);
        assertEquals(testStr, xml.getString());
    }

    @Test
    public void testSetBinaryStream() {
        SQLXMLImpl xml = new SQLXMLImpl(testStr);
        assertThrows(SQLException.class, xml::setBinaryStream);
    }

    @Test
    public void testSetCharacterStream() {
        SQLXMLImpl xml = new SQLXMLImpl(testStr);
        assertThrows(SQLException.class, xml::setCharacterStream);
    }

    @Test
    public void testSetString() {
        SQLXMLImpl xml = new SQLXMLImpl(testStr);
        assertThrows(SQLException.class, () -> xml.setString(testStr));
    }

    @Test
    public void testGetString1() throws Exception {
        SQLXMLImpl clob = new SQLXMLImpl() {
            public Reader getCharacterStream() {
                return new Reader() {

                    int pos = 0;

                    @Override
                    public void close() {

                    }

                    @Override
                    public int read(char[] cbuf, int off, int len) {
                        if (pos < 5) {
                            cbuf[off] = 'a';
                            pos++;
                            return 1;
                        }
                        return -1;
                    }
                };
            }
        };
        assertEquals("aaaaa", clob.getString());
    }

    @Test
    public void testConstructorDoesNotInvokeOverridableEncodingSetter() {
        TrackingSQLXMLImpl xml = new TrackingSQLXMLImpl(testStr);

        assertFalse(xml.setterInvoked);
        assertEquals(Streamable.CHARSET, xml.getCharset());

        xml.setEncoding(Streamable.ENCODING);
        assertTrue(xml.setterInvoked);
    }

    private static final class TrackingSQLXMLImpl extends SQLXMLImpl {

        @Serial
        private static final long serialVersionUID = 1L;

        private boolean setterInvoked;

        public TrackingSQLXMLImpl() {
        }

        private TrackingSQLXMLImpl(String value) {
            super(value);
        }

        @Override
        public void setEncoding(String encoding) {
            setterInvoked = true;
            super.setEncoding(encoding);
        }
    }

}
