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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestStringUtil {

    public void helpTestJoin(List<String> input, String delimiter, String expectedResult) {
        String result = StringUtil.join(input, delimiter);
        assertEquals(expectedResult, result);
    }

    public void helpTestReplace(String source, String search, String replace, String expectedResult) {
        String result = StringUtil.replace(source, search, replace);
        assertEquals(expectedResult, result);
    }

    public void helpTestReplaceAll(String source, String search, String replace, String expectedResult) {
        String result = StringUtil.replaceAll(source, search, replace);
        assertEquals(expectedResult, result, "Unexpected ReplaceAll result");
    }

    @Test
    public void testJoin1() {
        List<String> input = new ArrayList<>();
        input.add("One");
        input.add("Two");
        helpTestJoin(input, null, null);
    }

    @Test
    public void testJoin2() {
        helpTestJoin(null, "/", null);
    }

    @Test
    public void testJoin3() {
        List<String> input = new ArrayList<>();
        input.add("One");
        input.add("Two");
        helpTestJoin(input, "/", "One/Two");
    }

    @Test
    public void testReplace1() {
        helpTestReplace("12225", null, "234", "12225");
    }

    @Test
    public void testReplace2() {
        helpTestReplace("12225", "222", null, "12225");
    }

    @Test
    public void testReplace3() {
        helpTestReplace("12225", "222", "234", "12345");
    }

    @Test
    public void testReplaceAll() {
        helpTestReplaceAll("1121121112", "2", "1", "1111111111");
    }

    @Test
    public void testGetStackTrace() {
        final String expectedStackTrace = "java.lang.RuntimeException: Test";
        final Throwable t = new RuntimeException("Test");
        final String trace = StringUtil.getStackTrace(t);
        if (!trace.startsWith(expectedStackTrace)) {
            fail("Stack trace: \n" + trace + "\n did not match expected stack trace: \n" + expectedStackTrace);
        }
    }

    @Test
    public void testToString() {
        final String[] input = new String[]{"string1", "string2", "string3"};
        final String output = StringUtil.toString(input);
        assertEquals("[string1,string2,string3]", output);
    }

    @Test
    public void testGetTokens() {
        final String input = "string with; tokens ; delimited by ; ; semicolons; there;; are 7 tokens.";
        final List<String> tokens = StringUtil.getTokens(input, ";");
        assertEquals(7, tokens.size());
        assertEquals("string with", tokens.get(0));
        assertEquals(" tokens ", tokens.get(1));
        assertEquals(" delimited by ", tokens.get(2));
        assertEquals(" ", tokens.get(3));
        assertEquals(" semicolons", tokens.get(4));
        assertEquals(" there", tokens.get(5));
        assertEquals(" are 7 tokens.", tokens.get(6));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        String text = "test";
        assertEquals(-1, StringUtil.indexOfIgnoreCase(null, text));
        assertEquals(-1, StringUtil.indexOfIgnoreCase("", text));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(text, null));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(text, ""));
        assertEquals(-1, StringUtil.indexOfIgnoreCase(text, "testing"));

        assertEquals(1, StringUtil.indexOfIgnoreCase(text, "es"));
        assertEquals(1, StringUtil.indexOfIgnoreCase(text, "Es"));
        assertEquals(1, StringUtil.indexOfIgnoreCase(text, "eS"));
        assertEquals(2, StringUtil.indexOfIgnoreCase(text, "ST"));
    }

    @Test
    public void testStartsWithIgnoreCase() {
        String text = "test";
        assertFalse(StringUtil.startsWithIgnoreCase(null, text));
        assertFalse(StringUtil.startsWithIgnoreCase("", text));
        assertFalse(StringUtil.startsWithIgnoreCase(text, null));
        assertTrue(StringUtil.startsWithIgnoreCase(text, ""));
        assertFalse(StringUtil.startsWithIgnoreCase(text, "testing"));

        assertFalse(StringUtil.startsWithIgnoreCase(text, "es"));
        assertTrue(StringUtil.startsWithIgnoreCase(text, "te"));
        assertTrue(StringUtil.startsWithIgnoreCase(text, "Te"));
        assertTrue(StringUtil.startsWithIgnoreCase(text, "tE"));
        assertTrue(StringUtil.startsWithIgnoreCase(text, "TE"));
    }

    @Test
    public void testEndsWithIgnoreCase() {
        String text = "test";
        assertFalse(StringUtil.endsWithIgnoreCase(null, text));
        assertFalse(StringUtil.endsWithIgnoreCase("", text));
        assertFalse(StringUtil.endsWithIgnoreCase(text, null));
        assertTrue(StringUtil.endsWithIgnoreCase(text, ""));
        assertFalse(StringUtil.endsWithIgnoreCase(text, "testing"));

        assertFalse(StringUtil.endsWithIgnoreCase(text, "es"));
        assertTrue(StringUtil.endsWithIgnoreCase(text, "st"));
        assertTrue(StringUtil.endsWithIgnoreCase(text, "St"));
        assertTrue(StringUtil.endsWithIgnoreCase(text, "sT"));
        assertTrue(StringUtil.endsWithIgnoreCase(text, "ST"));
    }

    @Test
    public void testIsLetter() {
        assertTrue(StringUtil.isLetter('a'));
        assertTrue(StringUtil.isLetter('A'));
        assertFalse(StringUtil.isLetter('5'));
        assertFalse(StringUtil.isLetter('_'));
        assertTrue(StringUtil.isLetter('\u00cf')); // Latin-1 letter
        assertFalse(StringUtil.isLetter('\u0967')); // Devanagiri number
        assertTrue(StringUtil.isLetter('\u0905')); // Devanagiri letter
    }

    @Test
    public void testIsDigit() {
        assertFalse(StringUtil.isDigit('a'));
        assertFalse(StringUtil.isDigit('A'));
        assertTrue(StringUtil.isDigit('5'));
        assertFalse(StringUtil.isDigit('_'));
        assertFalse(StringUtil.isDigit('\u00cf')); // Latin-1 letter
        assertTrue(StringUtil.isDigit('\u0967')); // Devanagiri number
        assertFalse(StringUtil.isDigit('\u0905')); // Devanagiri letter
    }

    @Test
    public void testIsLetterOrDigit() {
        assertTrue(StringUtil.isLetterOrDigit('a'));
        assertTrue(StringUtil.isLetterOrDigit('A'));
        assertTrue(StringUtil.isLetterOrDigit('5'));
        assertFalse(StringUtil.isLetterOrDigit('_'));
        assertTrue(StringUtil.isLetterOrDigit('\u00cf')); // Latin-1 letter
        assertTrue(StringUtil.isLetterOrDigit('\u0967')); // Devanagiri number
        assertTrue(StringUtil.isLetterOrDigit('\u0905')); // Devanagiri letter
    }

    @Test
    public void testGetFirstToken() {
        assertEquals("/foo/bar", StringUtil.getFirstToken("/foo/bar.vdb", "."));
        assertEquals("", StringUtil.getFirstToken("/foo/bar.vdb", "/"));
        assertEquals("/foo", StringUtil.getFirstToken("/foo./bar.vdb", "."));
        assertEquals("bar", StringUtil.getFirstToken(StringUtil.getLastToken("/foo/bar.vdb", "/"), "."));
        assertEquals("vdb", StringUtil.getLastToken("/foo/bar.vdb", "."));
    }

    public enum EnumTest {
        HELLO,
        WORLD
    }

    @Test
    public void testValueOf() throws Exception {
        assertEquals(Integer.valueOf(21), StringUtil.valueOf("21", Integer.class));
        assertEquals(Boolean.TRUE, StringUtil.valueOf("true", Boolean.class));
        assertEquals("Foo", StringUtil.valueOf("Foo", String.class));
        assertEquals(Float.valueOf(10.12f), StringUtil.valueOf("10.12", Float.class));
        assertEquals(Double.valueOf(121.123), StringUtil.valueOf("121.123", Double.class));
        assertEquals(Long.valueOf(12334567L), StringUtil.valueOf("12334567", Long.class));
        assertEquals(Short.valueOf((short) 21), StringUtil.valueOf("21", Short.class));

        List<?> list = StringUtil.valueOf("foo,bar,x,y,z", List.class);
        assertEquals(5, list.size());
        assertTrue(list.contains("foo"));
        assertTrue(list.contains("x"));

        int[] values = StringUtil.valueOf("1,2,3,4,5", int[].class);
        assertEquals(5, values.length);
        assertEquals(5, values[4]);

        Map<?, ?> m = StringUtil.valueOf("foo=bar,x=,y=z", Map.class);
        assertEquals(3, m.size());
        assertEquals(m.get("foo"), "bar");
        assertEquals(m.get("x"), "");
        assertEquals(EnumTest.HELLO, StringUtil.valueOf("HELLO", EnumTest.class));

        assertEquals(URI.create("https://kubling.com").toURL(),
                StringUtil.valueOf("https://kubling.com", URL.class));
        assertThrows(IllegalArgumentException.class,
                () -> StringUtil.valueOf("not-a-url", URL.class));
    }
}
