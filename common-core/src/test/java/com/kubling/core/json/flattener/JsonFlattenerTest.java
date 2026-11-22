/*
 *
 * Copyright 2015 Wei-Ming Wu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.core.json.flattener;

import com.github.wnameless.json.base.Jackson3JsonCore;
import com.github.wnameless.json.base.Jackson3JsonValue;
import com.kubling.core.json.unflattener.JsonUnflattener;
import com.kubling.query.unittest.ResourcesUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JsonFlattenerTest {

    ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    public void testConstructorException() {
        assertThrows(RuntimeException.class, () -> new JsonFlattener("abc[123]}"));
    }

    @Test
    public void testFlatten() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test2.json");

        assertEquals("{\"a.b\":1,\"a.c\":null,\"a.d[0]\":false,\"a.d[1]\":true,\"e\":\"f\",\"g\":2.3}",
                JsonFlattener.flatten(json));

        assertEquals("{\"[0].a\":1,\"[1]\":2,\"[2].c[0]\":3,\"[2].c[1]\":4}",
                JsonFlattener.flatten("[{\"a\":1},2,{\"c\":[3,4]}]"));
    }

    @Test
    public void testFlattenWithExactFloat() {
        assertEquals("{\"[0].a\":1,\"[1]\":2.0,\"[2].c[0]\":3,\"[2].c[1]\":4}",
                JsonFlattener.flatten("[{\"a\":1},2.00,{\"c\":[3,4]}]"));

        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();
        JsonFlattener jsonFlattener =
                new JsonFlattener(new Jackson3JsonCore(mapper), "[{\"a\":1},2.00,{\"c\":[3,4]}]");
        assertEquals("{\"[0].a\":1,\"[1]\":2.00,\"[2].c[0]\":3,\"[2].c[1]\":4}",
                jsonFlattener.flatten());
    }

    @Test
    public void testFlattenAsMap() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test2.json");

        assertEquals("{\"a.b\":1,\"a.c\":null,\"a.d[0]\":false,\"a.d[1]\":true,\"e\":\"f\",\"g\":2.3}",
                JsonFlattener.flattenAsMap(json).toString());
    }

    @Test
    public void testFlattenAsMapWithExactFloat() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test2.json");

        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();
        JsonFlattener jsonFlattener = new JsonFlattener(new Jackson3JsonCore(mapper), json);
        assertEquals("{\"a.b\":1,\"a.c\":null,\"a.d[0]\":false,\"a.d[1]\":true,\"e\":\"f\",\"g\":2.30}",
                jsonFlattener.flattenAsMap().toString());
    }

    @Test
    public void testFlattenWithJsonValueBase() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test2.json");

        JsonNode jsonVal = JsonMapper.builder().build().readTree(json);
        assertEquals("{\"a.b\":1,\"a.c\":null,\"a.d[0]\":false,\"a.d[1]\":true,\"e\":\"f\",\"g\":2.3}",
                JsonFlattener.flatten(new Jackson3JsonValue(jsonVal)));

        assertEquals("{\"[0].a\":1,\"[1]\":2,\"[2].c[0]\":3,\"[2].c[1]\":4}",
                JsonFlattener.flatten("[{\"a\":1},2,{\"c\":[3,4]}]"));
    }

    @Test
    public void testFlattenAsMapWithJsonValueBase() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test2.json");

        JsonNode jsonVal = JsonMapper.builder().build().readTree(json);
        assertEquals("{\"a.b\":1,\"a.c\":null,\"a.d[0]\":false,\"a.d[1]\":true,\"e\":\"f\",\"g\":2.3}",
                JsonFlattener.flattenAsMap(new Jackson3JsonValue(jsonVal)).toString());

        assertEquals("{\"[0].a\":1,\"[1]\":2,\"[2].c[0]\":3,\"[2].c[1]\":4}",
                JsonFlattener.flattenAsMap("[{\"a\":1},2,{\"c\":[3,4]}]").toString());
    }

    @Test
    public void testFlattenWithKeyContainsDotAndSquareBracket() {
        assertEquals("{\"[0][\\\"a.a.[\\\"]\":1,\"[1]\":2,\"[2].c[0]\":3,\"[2].c[1]\":4}",
                JsonFlattener.flatten("[{\"a.a.[\":1},2,{\"c\":[3,4]}]"));
    }

    @Test
    public void testHashCode() throws IOException {
        String json1 = ResourcesUtil.getClassPathResource("test.json");
        String json2 = ResourcesUtil.getClassPathResource("test2.json");

        JsonFlattener flattener = new JsonFlattener(json1);
        assertEquals(flattener.hashCode(), flattener.hashCode());
        assertEquals(flattener.hashCode(), new JsonFlattener(json1).hashCode());
        assertNotEquals(flattener.hashCode(), new JsonFlattener(json2).hashCode());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals() throws IOException {
        String json1 = ResourcesUtil.getClassPathResource("test.json");
        String json2 = ResourcesUtil.getClassPathResource("test2.json");

        JsonFlattener flattener = new JsonFlattener(json1);
        assertEquals(flattener, new JsonFlattener(json1));
        assertNotEquals(flattener, new JsonFlattener(json2));
    }

    @Test
    public void testToString() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test2.json");

        assertEquals(
                "JsonFlattener{source={\"a\":{\"b\":1,\"c\":null,\"d\":[false,true]},\"e\":\"f\",\"g\":2.3}}",
                new JsonFlattener(json).toString());
    }

    @Test
    public void testWithNoPrecisionDouble() {
        String json = "{\"39473331\":{\"mega\":6.0,\"goals\":1.0}}";
        assertEquals("{\"39473331.mega\":6.0,\"39473331.goals\":1.0}",
                new JsonFlattener(json).flatten());
    }

    @Test
    public void testWithEmptyJsonObject() {
        String json = "{}";
        assertEquals("{}", new JsonFlattener(json).flatten());
        assertEquals(json, JsonUnflattener.unflatten(new JsonFlattener(json).flatten()));
        assertEquals(new HashMap<>(), new JsonFlattener(json).flattenAsMap());
    }

    @Test
    public void testWithEmptyJsonArray() {
        String json = "[]";
        assertEquals("[]", new JsonFlattener(json).flatten());
        assertEquals(Map.of("root", new ArrayList<>()), new JsonFlattener(json).flattenAsMap());
        assertEquals(json, JsonUnflattener.unflatten(new JsonFlattener(json).flatten()));
        assertEquals("[]", new JsonFlattener(json).withFlattenMode(FlattenMode.KEEP_ARRAYS).flatten());
        assertEquals(Map.of("root", new ArrayList<>()),
                new JsonFlattener(json).withFlattenMode(FlattenMode.KEEP_ARRAYS).flattenAsMap());
        assertEquals(json, JsonUnflattener
                .unflatten(new JsonFlattener(json).withFlattenMode(FlattenMode.KEEP_ARRAYS).flatten()));
    }

    @Test
    public void testWithEmptyArray() {
        String json = "{\"no\":\"1\",\"name\":\"riya\",\"marks\":[]}";
        assertEquals("{\"no\":\"1\",\"name\":\"riya\",\"marks\":[]}",
                new JsonFlattener(json).flatten());
        assertEquals(json, JsonUnflattener.unflatten(new JsonFlattener(json).flatten()));
    }

    @Test
    public void testWithEmptyObject() {
        String json = "{\"no\":\"1\",\"name\":\"riya\",\"marks\":[{}]}";
        assertEquals("{\"no\":\"1\",\"name\":\"riya\",\"marks[0]\":{}}",
                new JsonFlattener(json).flatten());
        assertEquals(json, JsonUnflattener.unflatten(new JsonFlattener(json).flatten()));
    }

    @Test
    public void testWithArray() {
        String json = "[{\"abc\":123},456,[null]]";
        assertEquals("{\"[0].abc\":123,\"[1]\":456,\"[2][0]\":null}",
                new JsonFlattener(json).flatten());
        assertEquals(json, JsonUnflattener.unflatten(new JsonFlattener(json).flatten()));
    }

    @Test
    public void testWithSpecialCharacters() {
        String json = "[{\"abc\\t\":\" \\\" \\r \\t \1234 \"}]";
        assertEquals("{\"[0].abc\\t\":\" \\\" \\r \\t \1234 \"}", new JsonFlattener(json).flatten());
        json = "{\" \":[123,\"abc\"]}";
        assertEquals("{\" [0]\":123,\" [1]\":\"abc\"}", new JsonFlattener(json).flatten());
    }

    @Test
    public void testWithUnicodeCharacters() {
        String json = "[{\"姓名\":123}]";
        assertEquals("{\"[0].姓名\":123}", new JsonFlattener(json).flatten());
    }

    @Test
    public void testWithFlattenMode() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test4.json");
        assertEquals(
                "{\"a.b\":1,\"a.c\":null,\"a.d\":[false,{\"i.j\":[false,true,\"xy\"]}],\"e\":\"f\",\"g\":2.3,\"z\":[]}",
                new JsonFlattener(json).withFlattenMode(FlattenMode.KEEP_ARRAYS).flatten());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWithStringEscapePolicyALL_UNICODES() {
        String json = "{\"abc\":{\"def\":\"太極\"}}";
        assertEquals("{\"abc.def\":\"\\u592A\\u6975\"}",
                new JsonFlattener(json).withStringEscapePolicy(StringEscapePolicy.ALL_UNICODES).flatten());
    }

    @Test
    public void testWithStringEscapePolicyALL() {
        String json = "{\"abc\":{\"def\":\"太極/兩儀\"}}";
        assertEquals("{\"abc.def\":\"\\u592A\\u6975\\/\\u5169\\u5100\"}",
                new JsonFlattener(json).withStringEscapePolicy(StringEscapePolicy.ALL).flatten());
    }

    @Test
    public void testWithStringEscapePolicyALL_BUT_SLASH() {
        String json = "{\"abc\":{\"def\":\"太極/兩儀\"}}";
        assertEquals("{\"abc.def\":\"\\u592A\\u6975/\\u5169\\u5100\"}",
                new JsonFlattener(json).withStringEscapePolicy(StringEscapePolicy.ALL_BUT_SLASH).flatten());
    }

    @Test
    public void testWithStringEscapePolicyALL_BUT_UNICODE() {
        String json = "{\"abc\":{\"def\":\"太極/兩儀\"}}";
        assertEquals("{\"abc.def\":\"太極\\/兩儀\"}", new JsonFlattener(json)
                .withStringEscapePolicy(StringEscapePolicy.ALL_BUT_UNICODE).flatten());
    }

    @Test
    public void testWithStringEscapePolicyALL_BUT_SLASH_AND_UNICODE() {
        String json = "{\"abc\":{\"def\":\"太極/兩儀\"}}";
        assertEquals("{\"abc.def\":\"太極/兩儀\"}", new JsonFlattener(json)
                .withStringEscapePolicy(StringEscapePolicy.ALL_BUT_SLASH_AND_UNICODE).flatten());
    }

    @Test
    public void testWithSeparator() {
        String json = "{\"abc\":{\"def\":123}}";
        assertEquals("{\"abc*def\":123}", new JsonFlattener(json).withSeparator("*").flatten());
    }

    @Test
    public void testWithSeparatorException() {
        String json = "{\"abc\":{\"def\":123}}";
        try {
            new JsonFlattener(json).withSeparator("\"");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Separator contains illegal character(\")", e.getMessage());
        }
        try {
            new JsonFlattener(json).withSeparator(StringUtils.SPACE);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Separator contains illegal character( )", e.getMessage());
        }
        try {
            new JsonFlattener(json).withSeparator("[");
//      fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Separator([) is already used in brackets", e.getMessage());
        }
        try {
            new JsonFlattener(json).withSeparator("]");
//      fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Separator(]) is already used in brackets", e.getMessage());
        }
    }

    @Test
    public void testWithLeftAndRightBracket() {
        String json = "{\"abc\":{\"A.\":[123,\"def\"]}}";
        assertEquals("{\"abc{\\\"A.\\\"}{0}\":123,\"abc{\\\"A.\\\"}{1}\":\"def\"}",
                new JsonFlattener(json).withLeftAndRightBrackets('{', '}').flatten());
    }

    @Test
    public void testWithLeftAndRightBracketsException() {
        String json = "{\"abc\":{\"def\":123}}";
        try {
            new JsonFlattener(json).withLeftAndRightBrackets('#', '#');
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Both brackets cannot be the same", e.getMessage());
        }
        try {
            new JsonFlattener(json).withLeftAndRightBrackets('"', ']');
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Left bracket contains illegal character(\")", e.getMessage());
        }
        try {
            new JsonFlattener(json).withLeftAndRightBrackets(' ', ']');
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Left bracket contains illegal character( )", e.getMessage());
        }
        try {
            new JsonFlattener(json).withLeftAndRightBrackets('.', ']');
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Left bracket contains illegal character(.)", e.getMessage());
        }
        try {
            new JsonFlattener(json).withLeftAndRightBrackets('[', '"');
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Right bracket contains illegal character(\")", e.getMessage());
        }
        try {
            new JsonFlattener(json).withLeftAndRightBrackets('[', ' ');
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Right bracket contains illegal character( )", e.getMessage());
        }
        try {
            new JsonFlattener(json).withLeftAndRightBrackets('[', '.');
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Right bracket contains illegal character(.)", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRootInMap() {
        assertEquals("null", JsonFlattener.flatten("null"));
        assertNull(JsonFlattener.flattenAsMap("null").get("root"));
        assertEquals("123", JsonFlattener.flatten("123"));
        assertEquals(123, JsonFlattener.flattenAsMap("123").get("root"));
        assertEquals("\"abc\"", JsonFlattener.flatten("\"abc\""));
        assertEquals("abc", JsonFlattener.flattenAsMap("\"abc\"").get("root"));
        assertEquals("true", JsonFlattener.flatten("true"));
        assertEquals(true, JsonFlattener.flattenAsMap("true").get("root"));
        assertEquals("[]", JsonFlattener.flatten("[]"));
        assertEquals(Collections.emptyList(), JsonFlattener.flattenAsMap("[]").get("root"));
        assertEquals("[[{\"abc.def\":123}]]", new JsonFlattener("[[{\"abc\":{\"def\":123}}]]")
                .withFlattenMode(FlattenMode.KEEP_ARRAYS).flatten());
        List<List<Map<String, Object>>> root =
                (List<List<Map<String, Object>>>) new JsonFlattener("[[{\"abc\":{\"def\":123}}]]")
                        .withFlattenMode(FlattenMode.KEEP_ARRAYS).flattenAsMap().get("root");
        assertEquals(Map.of("abc.def", 123), root.getFirst().getFirst());
    }

    @Test
    public void testPrintMode() throws IOException {
        String src = ResourcesUtil.getClassPathResource("test.json");

        String json = new JsonFlattener(src).withPrintMode(PrintMode.MINIMAL).flatten();
        assertEquals(mapper.readTree(json).toString(), json);

        json = new JsonFlattener(src).withPrintMode(PrintMode.PRETTY).flatten();
        assertEquals(mapper.readTree(json).toPrettyString(), json);

        src = "[[123]]";
        json = new JsonFlattener(src).withFlattenMode(FlattenMode.KEEP_ARRAYS)
                .withPrintMode(PrintMode.MINIMAL).flatten();
        assertEquals(mapper.readTree(json).toString(), json);

        json = new JsonFlattener(src).withFlattenMode(FlattenMode.KEEP_ARRAYS)
                .withPrintMode(PrintMode.PRETTY).flatten();
        assertEquals(mapper.readTree(json).toPrettyString(), json);
    }

    @Test
    public void testPrintModeWithEscapedDoubleQoutesAndBackslash() throws IOException {
        String input = ResourcesUtil.getClassPathResource("test_print_mode_unflatten_minimal.json");
        JsonFlattener jf = new JsonFlattener(input);
        jf.withPrintMode(PrintMode.MINIMAL);
        String flattendJsonWithDotKey = jf.flatten();

        String output = ResourcesUtil.getClassPathResource("test_print_mode_flatten_minimal.json");
        assertEquals(output, flattendJsonWithDotKey);

        jf.withPrintMode(PrintMode.PRETTY);
        flattendJsonWithDotKey = jf.flatten();

        output = ResourcesUtil.getClassPathResource("test_print_mode_flatten_pretty.json");
        assertEquals(output, flattendJsonWithDotKey);

        JsonUnflattener ju = new JsonUnflattener(flattendJsonWithDotKey);
        ju.withPrintMode(PrintMode.MINIMAL);
        String nestedJsonWithDotKey = ju.unflatten();

        assertEquals(input, nestedJsonWithDotKey);

        ju.withPrintMode(PrintMode.PRETTY);
        nestedJsonWithDotKey = ju.unflatten();

        output = ResourcesUtil.getClassPathResource("test_print_mode_unflatten_pretty.json");
        assertEquals(output, nestedJsonWithDotKey);
    }

    @Test
    public void testNoCache() {
        JsonFlattener jf = new JsonFlattener("{\"abc\":{\"def\":123}}");
        assertSame(jf.flattenAsMap(), jf.flattenAsMap());
        assertNotSame(jf.flatten(), jf.flatten());
        assertEquals("{\"abc*def\":123}", jf.withSeparator("*").flatten());
        assertEquals(jf.flatten(), jf.withPrintMode(PrintMode.MINIMAL).flatten());
    }

    @Test
    public void testNullPointerException() {
        try {
            new JsonFlattener("{\"abc\":{\"def\":123}}").withFlattenMode(null);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            new JsonFlattener("{\"abc\":{\"def\":123}}").withStringEscapePolicy(null);
            fail();
        } catch (NullPointerException ignored) {
        }
        try {
            new JsonFlattener("{\"abc\":{\"def\":123}}").withPrintMode(null);
            fail();
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testFlattenWithNestedEmptyJsonObjectAndKeepArraysMode() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test5.json");

        assertEquals(
                "{\"a.b\":1,\"a.c\":null,\"a.d\":[false,{\"i.j\":[false,true]}],\"e\":\"f\",\"g\":2.3,\"z\":{}}",
                new JsonFlattener(json).withFlattenMode(FlattenMode.KEEP_ARRAYS).flatten());
    }

    @Test
    public void testWithSeparatorAndNestedObject() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test5.json");
        assertEquals(
                "{\"a_b\":1,\"a_c\":null,\"a_d\":[false,{\"i_j\":[false,true]}],\"e\":\"f\",\"g\":2.3,\"z\":{}}",
                new JsonFlattener(json).withFlattenMode(FlattenMode.KEEP_ARRAYS).withSeparator("_")
                        .flatten());
    }

    @Test
    public void testWithRootKeyInSourceObject() {
        String json = "{\"" + JsonFlattener.ROOT + "\":null, \"ss\":[123]}";
        assertEquals("{\"" + JsonFlattener.ROOT + "\":null,\"ss[0]\":123}",
                JsonFlattener.flatten(json));
    }

    @Test
    public void testInitByReader() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test.json");

        JsonFlattener jf = new JsonFlattener(new InputStreamReader(IOUtils.toInputStream(json, StandardCharsets.UTF_8)));
        assertEquals(jf, new JsonFlattener(json));
    }

    @Test
    public void testFlattenModeMongodb() throws IOException {
        String src = ResourcesUtil.getClassPathResource("test_mongo.json");

        String expectedJson = ResourcesUtil.getClassPathResource("test_mongo_flattened.json");

        String flattened = new JsonFlattener(src).withFlattenMode(FlattenMode.MONGODB)
                .withPrintMode(PrintMode.PRETTY).flatten();

        assertEquals(expectedJson, flattened);
    }

    @Test
    public void testFlattenModeMongodbException() {
        String json = "{\"abc\":{\"de.f\":123}}";
        JsonFlattener jf = new JsonFlattener(json).withFlattenMode(FlattenMode.MONGODB);
        try {
            jf.flatten();
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Key cannot contain separator(.) in FlattenMode.MONGODB", e.getMessage());
        }
    }

    @Test
    public void testWithKeyTransformer() {
        String json = "{\"abc\":{\"de.f\":123}}";
        JsonFlattener jf = new JsonFlattener(json).withFlattenMode(FlattenMode.MONGODB)
                .withKeyTransformer(new KeyTransformer() {

                    @Override
                    public String transform(String key) {
                        return key.replace('.', '_');
                    }

                });
        assertEquals("{\"abc.de_f\":123}", jf.flatten());
    }

    @Test
    public void testWithFlattenModeKeepBottomArrays() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test_keep_primitive_arrays.json");

        String expectedJson = ResourcesUtil.getClassPathResource("test_keep_primitive_arrays_flattened.json");

        JsonFlattener jf = new JsonFlattener(json).withFlattenMode(FlattenMode.KEEP_PRIMITIVE_ARRAYS)
                .withPrintMode(PrintMode.PRETTY);
        String flattened = jf.flatten();

        assertEquals(expectedJson, flattened);
    }

    @Test
    public void testWithJsonCore() throws IOException {
        String json = ResourcesUtil.getClassPathResource("test_long_decimal.json");

        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .build();

        JsonFlattener jf = new JsonFlattener(new Jackson3JsonCore(mapper), json);
        jf.withPrintMode(PrintMode.PRETTY);
        assertEquals(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json)),
                jf.flatten());

        jf = new JsonFlattener(new Jackson3JsonCore(mapper), new StringReader(json));
        jf.withPrintMode(PrintMode.PRETTY);
        assertEquals(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json)),
                jf.flatten());

        jf = new JsonFlattener(new Jackson3JsonCore(mapper), new Jackson3JsonCore(mapper).parse(json));
        jf.withPrintMode(PrintMode.PRETTY);
        assertEquals(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json)),
                jf.flatten());
    }

    @Test
    public void testWithIgnoreReservedCharacters() {
        String json = "{\"matrix\":{\"agent.smith\":\"1999\"}}";

        assertEquals("{\"matrix[\\\"agent.smith\\\"]\":\"1999\"}", JsonFlattener.flatten(json));
        assertEquals("{\"matrix.agent.smith\":\"1999\"}",
                new JsonFlattener(json).ignoreReservedCharacters().flatten());

        assertThrows(IllegalArgumentException.class, () -> new JsonFlattener(json).withFlattenMode(FlattenMode.MONGODB).flatten());
        assertEquals("{\"matrix.agent.smith\":\"1999\"}", new JsonFlattener(json)
                .withFlattenMode(FlattenMode.MONGODB).ignoreReservedCharacters().flatten());

        String jsonArray = "[{\"matrix\":\"reloaded\",\"agent\":{\"smith_no\":\"1\"}},"
                + "{\"matrix\":\"reloaded\",\"agent\":{\"smith_no\":\"2\"}}]";

        assertEquals(
                "{\"_0_matrix\":\"reloaded\",\"_0_agent_smith_no\":\"1\",\"_1_matrix\":\"reloaded\",\"_1_agent_smith_no\":\"2\"}",
                new JsonFlattener(jsonArray).withFlattenMode(FlattenMode.MONGODB).withSeparator("_")
                        .ignoreReservedCharacters().flatten());
        assertEquals(
                "[{\"matrix\":\"reloaded\",\"agent_smith_no\":\"1\"},{\"matrix\":\"reloaded\",\"agent_smith_no\":\"2\"}]",
                new JsonFlattener(jsonArray).withFlattenMode(FlattenMode.KEEP_ARRAYS).withSeparator("_")
                        .ignoreReservedCharacters().flatten());
    }

}
