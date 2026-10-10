package com.kubling.core.types;

import com.kubling.core.KublingRuntimeException;
import com.kubling.core.util.UnitTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestJsonType {

    @Test
    public void testJsonEquals() {
        String json1 = "{ \"a\": 1, \"b\": 2 }";
        String json2 = "{ \"b\": 2, \"a\": 1 }";

        JsonType jt1 = new JsonType(new ClobImpl(json1));
        JsonType jt2 = new JsonType(new ClobImpl(json2));

        assertEquals(jt1, jt2);
    }

    @Test
    public void testJsonCompare() {
        String json1 = "{ \"a\": 1, \"b\": 2 }";
        String json2 = "{ \"b\": 2, \"a\": 1 }";

        JsonType jt1 = new JsonType(new ClobImpl(json1));
        JsonType jt2 = new JsonType(new ClobImpl(json2));

        assertEquals(0, jt1.compareTo(jt2));
    }

    @Test
    public void testJsonHashCode() {
        String json1 = "{ \"a\": 1, \"b\": 2 }";
        String json2 = "{ \"b\": 2, \"a\": 1 }";

        JsonType jt1 = new JsonType(new ClobImpl(json1));
        JsonType jt2 = new JsonType(new ClobImpl(json2));

        assertEquals(jt1.hashCode(), jt2.hashCode());
    }

    @Test
    public void testJsonTypeSerialization() throws Exception {
        String json = "{\"z\":9,\"a\":1}";
        JsonType jt = new JsonType(new ClobImpl(json));

        jt.setReferenceStreamId(null);
        assertEquals("{\"a\":1,\"z\":9}", jt.toString());

        JsonType read = UnitTestUtil.helpSerialize(jt);

        assertEquals(json.length(), read.length());
        assertEquals(json, read.getSubString(1, json.length()));

        assertEquals(jt, read);
        assertEquals(0, jt.compareTo(read));
        assertEquals(jt.hashCode(), read.hashCode());
    }

    @Test
    public void testInvalidJson() {
        JsonType jt = new JsonType(new ClobImpl("{ invalid json }"));

        assertThrows(KublingRuntimeException.class, jt::hashCode);
    }

    @Test
    public void testEqualsDifferentType() {
        JsonType jt = new JsonType(new ClobImpl("{\"a\": 1}"));
        assertNotEquals(new Object(), jt);
    }

    @Test
    public void testCompareWithNonJsonType() {
        JsonType jt = new JsonType(new ClobImpl("{\"a\": 1}"));
        ClobType ct = new ClobType(new ClobImpl("other text"));

        assertNotEquals(0, jt.compareTo(ct)); // falls back to default compare
    }

}
