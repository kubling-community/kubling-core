/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.core.types;

import com.kubling.core.crypto.CryptoException;
import com.kubling.core.json.flattener.JsonifyArrayList;
import com.kubling.core.json.flattener.JsonifyLinkedHashMap;
import com.kubling.core.util.ApplicationInfo;
import com.kubling.core.util.LRUCache;
import com.kubling.core.util.UnitTestUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TestSerializationCompatibility {

    @Test
    public void testExistingSerialVersionUids() {
        assertSerialVersionUid(-7112228759017805524L, BinaryType.class);
        assertSerialVersionUid(6864882087853041423L, GeometryType.class);
        assertSerialVersionUid(-1163661573729970809L, ArrayImpl.NullException.class);
        assertSerialVersionUid(1725951583381521004L, SQLXMLImpl.class);
        assertSerialVersionUid(5328359053675439976L, BlobImpl.class);
        assertSerialVersionUid(-4868633917511897143L, ClobImpl.class);
        assertSerialVersionUid(-4723658681834168137L, ApplicationInfo.class);
        assertSerialVersionUid(-8487873564252233036L, LRUCache.class);
        assertSerialVersionUid(-4596425569745339598L, CryptoException.class);
    }

    @Test
    public void testJsonCollectionTranslatorsAfterSerialization() throws Exception {
        JsonifyArrayList<String> list = new JsonifyArrayList<>();
        list.add("a\"b");
        assertEquals(list.toString(), UnitTestUtil.helpSerialize(list).toString());

        JsonifyLinkedHashMap<String, String> map = new JsonifyLinkedHashMap<>();
        map.put("a\"b", "c\nd");
        assertEquals(map.toString(), UnitTestUtil.helpSerialize(map).toString());
    }

    @Test
    public void testExternalizableFieldsRemainOnTheWire() throws Exception {
        ArrayImpl array = new ArrayImpl((Object[]) new String[]{"a", "b"});
        array.setZeroBased(true);
        ArrayImpl arrayCopy = UnitTestUtil.helpSerialize(array);
        assertArrayEquals(array.getValues(), arrayCopy.getValues());
        assertTrue(arrayCopy.isZeroBased());

        byte[] bytes = {1, 2, 3};
        BlobImpl blobCopy = UnitTestUtil.helpSerialize(
                new BlobImpl(new SerializableInputStreamFactory(bytes)));
        assertArrayEquals(bytes, blobCopy.getBytes(1, bytes.length));

        String text = "serialized clob";
        ClobImpl clobCopy = UnitTestUtil.helpSerialize(
                new ClobImpl(new SerializableInputStreamFactory(text), text.length()));
        assertEquals(text, clobCopy.getSubString(1, text.length()));

        String xml = "<value>serialized</value>";
        SQLXMLImpl xmlCopy = UnitTestUtil.helpSerialize(
                new SQLXMLImpl(new SerializableInputStreamFactory(xml)));
        assertEquals(xml, xmlCopy.getString());
    }

    @Test
    public void testGeospatialCacheIsNotSerialized() throws Exception {
        byte[] bytes = {4, 5, 6};
        GeometryType geometry = new GeometryType(bytes, 4326);
        geometry.setGeoCache(new Object());
        geometry.setReferenceStreamId(null);

        GeometryType copy = UnitTestUtil.helpSerialize(geometry);

        assertEquals(4326, copy.getSrid());
        assertArrayEquals(bytes, copy.getBytes(1, bytes.length));
        assertNull(copy.getGeoCache());
    }

    @Test
    public void testFileFactoryConstructorDoesNotInvokeOverridableSetter() {
        File file = new File("input.txt");
        TrackingFileInputStreamFactory factory = new TrackingFileInputStreamFactory(file);

        assertFalse(factory.setterInvoked);
        assertEquals(file.toURI().toASCIIString(), factory.getSystemId());

        factory.setSystemId("updated");
        assertTrue(factory.setterInvoked);
        assertEquals("updated", factory.getSystemId());
    }

    @Test
    public void testArrayComparisonAfterComparableCleanup() {
        assertEquals(0, new ArrayImpl(1, "a").compareTo(new ArrayImpl(1, "a")));
        assertTrue(new ArrayImpl(1, "a").compareTo(new ArrayImpl(1, "b")) < 0);
        assertTrue(new ArrayImpl(1).compareTo(new ArrayImpl(1, 2)) < 0);
        assertFalse(new ArrayImpl(1).equals(new ArrayImpl("1")));
    }

    private static void assertSerialVersionUid(long expected, Class<?> type) {
        assertEquals(expected, ObjectStreamClass.lookup(type).getSerialVersionUID(), type.getName());
    }

    private static final class SerializableInputStreamFactory extends InputStreamFactory
            implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final byte[] bytes;

        private SerializableInputStreamFactory(byte[] bytes) {
            this.bytes = bytes;
        }

        private SerializableInputStreamFactory(String value) {
            this(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public Reader getCharacterStream() {
            return new StringReader(new String(bytes, StandardCharsets.UTF_8));
        }

        @Override
        public long getLength() {
            return bytes.length;
        }

        @Override
        public StorageMode getStorageMode() {
            return StorageMode.MEMORY;
        }
    }

    private static final class TrackingFileInputStreamFactory
            extends InputStreamFactory.FileInputStreamFactory {

        private boolean setterInvoked;

        private TrackingFileInputStreamFactory(File file) {
            super(file);
        }

        @Override
        public void setSystemId(String systemId) {
            setterInvoked = true;
            super.setSystemId(systemId);
        }
    }
}
