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

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * A common utility class for string-related methods.
 * <p>
 * <strong>Note (Jun-25):</strong> The {@code @Deprecated} annotation was removed
 * because this class is widely used across the codebase. Rather than deprecating it,
 * we now treat it as a lightweight wrapper around Apache Commons Lang's string utilities.
 * This approach minimizes the amount of refactoring required while consolidating string logic in one place.
 */
public final class StringUtil {

    // Kept for compatibility.
    public interface Constants {
        String EMPTY_STRING = StringUtils.EMPTY;
        String SPACE = StringUtils.SPACE;
    }

    /**
     * Join string pieces and separate with a delimiter.  Similar to the perl function of
     * the same name.  If strings or delimiter are null, null is returned.  Otherwise, at
     * least an empty string will be returned.
     *
     * @param strings   String pieces to join
     * @param delimiter Delimiter to put between string pieces
     * @return One merged string
     * @see #split
     */
    public static String join(Collection<String> strings, String delimiter) {
        if (Objects.isNull(delimiter)) return null;
        return StringUtils.join(strings, delimiter);
    }

    /**
     * Return a stringified version of the array.
     *
     * @param array the array
     * @param delim the delimiter to use between array components
     * @return the string form of the array
     */
    public static String toString(final Object[] array, final String delim) {
        return toString(array, delim, true);
    }

    /**
     * Return a stringified version of the array.
     *
     * @param array the array
     * @param delim the delimiter to use between array components
     * @return the string form of the array
     */
    public static String toString(final Object[] array, final String delim, boolean includeBrackets) {
        if (array == null) {
            return StringUtils.EMPTY;
        }

        String joined = StringUtils.join(array, delim);
        return includeBrackets ? "[" + joined + "]" : joined;
    }


    /**
     * Return a stringified version of the array, using a ',' as a delimiter
     *
     * @param array the array
     * @return the string form of the array
     * @see #toString(Object[], String)
     */
    public static String toString(final Object[] array) {
        return toString(array, ",", true);
    }

    /**
     * Split a string into pieces based on delimiters.  Similar to the perl function of
     * the same name.  The delimiters are not included in the returned strings.
     *
     * @param str      Full string
     * @param splitter Characters to split on
     * @return List of String pieces from full string
     * @see #join
     */
    public static List<String> split(String str, String splitter) {
        StringTokenizer tokens = new StringTokenizer(str, splitter);
        ArrayList<String> l = new ArrayList<String>(tokens.countTokens());
        while (tokens.hasMoreTokens()) {
            l.add(tokens.nextToken());
        }
        return l;
    }

    /**
     * Replace a single occurrence of the search string with the replace string
     * in the source string. If any of the strings is null or the search string
     * is zero length, the source string is returned.
     *
     * @param source  the source string whose contents will be altered
     * @param search  the string to search for in source
     * @param replace the string to substitute for search if present
     * @return source string with the *first* occurrence of the search string
     * replaced with the replace string
     */
    public static String replace(String source, String search, String replace) {
        return Strings.CS.replace(source, search, replace);
    }

    /**
     * Replace all occurrences of the search string with the replace string
     * in the source string. If any of the strings is null or the search string
     * is zero length, the source string is returned.
     *
     * @param source  the source string whose contents will be altered
     * @param search  the string to search for in source
     * @param replace the string to substitute for search if present
     * @return source string with *all* occurrences of the search string
     * replaced with the replace string
     */
    public static String replaceAll(String source, String search, String replace) {
        return RegExUtils.replaceAll(source, search, replace);
    }

    /**
     * Return the tokens in a string in a list. This is particularly
     * helpful if the tokens need to be processed in reverse order. In that case,
     * a list iterator can be acquired from the list for reverse order traversal.
     *
     * @param str       String to be tokenized
     * @param delimiter Characters which are delimit tokens
     * @return List of string tokens contained in the tokenized string
     */
    public static List<String> getTokens(String str, String delimiter) {
        ArrayList<String> l = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(str, delimiter);
        while (tokens.hasMoreTokens()) {
            l.add(tokens.nextToken());
        }
        return l;
    }

    /**
     * Return the last token in the string.
     *
     * @param str       String to be tokenized
     * @param delimiter Characters which are delimit tokens
     * @return the last token contained in the tokenized string
     */
    public static String getLastToken(String str, String delimiter) {
        if (StringUtils.isEmpty(str)) {
            return StringUtils.EMPTY;
        }

        String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(str, delimiter);
        return parts.length > 0 ? parts[parts.length - 1] : StringUtils.EMPTY;
    }


    /**
     * Return the first token in the string.
     *
     * @param str       String to be tokenized
     * @param delimiter Characters which are delimit tokens
     * @return the first token contained in the tokenized string
     */
    public static String getFirstToken(String str, String delimiter) {
        if (StringUtils.isEmpty(str)) {
            return StringUtils.EMPTY;
        }

        String[] parts = StringUtils.splitByWholeSeparatorPreserveAllTokens(str, delimiter);
        return parts.length > 0 ? parts[0] : StringUtils.EMPTY;
    }

    public static String getStackTrace(final Throwable t) {
        return ExceptionUtils.getStackTrace(t);
    }

    /**
     * <p>
     * Returns whether the specified text is either empty or null.
     *
     * @param text The text to check; may be null;
     * @return True if the specified text is either empty or null.
     * @since 4.0
     */
    public static boolean isEmpty(final String text) {
        return StringUtils.isEmpty(text);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring. The integer returned is the smallest value
     * <i>k</i> such that:
     * <blockquote><pre>
     * this.startsWith(str, <i>k</i>)
     * </pre></blockquote>
     * is <code>true</code>.
     *
     * @param text any string.
     * @param str  any string.
     * @return if the str argument occurs as a substring within text,
     * then the index of the first character of the first
     * such substring is returned; if it does not occur as a
     * substring, <code>-1</code> is returned.  If the text or
     * str argument is null or empty then <code>-1</code> is returned.
     */
    public static int indexOfIgnoreCase(final String text, final String str) {
        if (isEmpty(text)) {
            return -1;
        }
        if (isEmpty(str)) {
            return -1;
        }
        int len = text.length() - str.length();
        for (int i = 0; i <= len; i++) {
            if (text.regionMatches(true, i, str, 0, str.length())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Tests if the string starts with the specified prefix.
     *
     * @param text   the string to test.
     * @param prefix the prefix.
     * @return <code>true</code> if the character sequence represented by the
     * argument is a prefix of the character sequence represented by
     * this string; <code>false</code> otherwise.
     * Note also that <code>true</code> will be returned if the
     * prefix is an empty string or is equal to the text
     * <code>String</code> object as determined by the
     * {@link #equals(Object)} method. If the text or
     * prefix argument is null <code>false</code> is returned.
     * @since JDK1. 0
     */
    public static boolean startsWithIgnoreCase(final String text, final String prefix) {
        if (text == null || prefix == null) {
            return false;
        }
        return Strings.CI.startsWith(text, prefix);
    }

    /**
     * Tests if the string ends with the specified suffix.
     */
    public static boolean endsWithIgnoreCase(final String text, final String suffix) {
        if (text == null || suffix == null) {
            return false;
        }
        return Strings.CI.endsWith(text, suffix);
    }

    /**
     * <p>
     * Prevents instantiation.
     *
     * @since 4.0
     */
    private StringUtil() {
    }

    public static boolean isLetter(char c) {
        return isBasicLatinLetter(c) || Character.isLetter(c);
    }

    public static boolean isDigit(char c) {
        return isBasicLatinDigit(c) || Character.isDigit(c);
    }

    public static boolean isLetterOrDigit(char c) {
        return isBasicLatinLetter(c) || isBasicLatinDigit(c) || Character.isLetterOrDigit(c);
    }

    private static boolean isBasicLatinLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isBasicLatinDigit(char c) {
        return c >= '0' && c <= '9';
    }

    @SuppressWarnings("unchecked" )
    public static <T> T valueOf(String value, Class<T> type) {
        if (value == null) {
            return null;
        }

        // Handle primitive wrappers and String
        if (type == String.class) return (T) value;
        if (type == Boolean.class || type == boolean.class) return (T) Boolean.valueOf(value);
        if (type == Integer.class || type == int.class) return (T) Integer.valueOf(value);
        if (type == Float.class || type == float.class) return (T) Float.valueOf(value);
        if (type == Double.class || type == double.class) return (T) Double.valueOf(value);
        if (type == Long.class || type == long.class) return (T) Long.valueOf(value);
        if (type == Short.class || type == short.class) return (T) Short.valueOf(value);
        if (type == Byte.class || type == byte.class) return (T) Byte.valueOf(value);

        // Enum
        if (type.isEnum()) {
            return (T) Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), value);
        }

        // URL
        if (type == URL.class) {
            try {
                return (T) URI.create(value).toURL();
            } catch (IllegalArgumentException | MalformedURLException e) {
                throw new IllegalArgumentException("Invalid URL: " + value, e);
            }
        }

        // List
        if (List.class.isAssignableFrom(type)) {
            return (T) Arrays.asList(value.split("," ));
        }

        // Set
        if (Set.class.isAssignableFrom(type)) {
            return (T) new HashSet<>(Arrays.asList(value.split("," )));
        }

        // Map
        if (Map.class.isAssignableFrom(type)) {
            Map<String, String> map = new HashMap<>();
            for (String pair : value.split("," )) {
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    map.put(pair.substring(0, idx), pair.substring(idx + 1));
                }
            }
            return (T) map;
        }

        // Array
        if (type.isArray()) {
            String[] values = value.split("," );
            Class<?> componentType = type.getComponentType();
            Object array = Array.newInstance(componentType, values.length);
            for (int i = 0; i < values.length; i++) {
                Array.set(array, i, valueOf(values[i].trim(), componentType));
            }
            return (T) array;
        }

        if (type == Void.class || type == void.class) {
            return null;
        }

        throw new IllegalArgumentException("Unsupported conversion from String to " + type.getName());
    }

    public static boolean equalsIgnoreCase(String s1, String s2) {
        return Strings.CI.equals(s1, s2);
    }

    public static <T extends Enum<T>> T caseInsensitiveValueOf(Class<T> enumType, String name) {
        try {
            return Enum.valueOf(enumType, name);
        } catch (IllegalArgumentException e) {
            T[] vals = enumType.getEnumConstants();
            for (T t : vals) {
                if (name.equalsIgnoreCase(t.name())) {
                    return t;
                }
            }
            throw e;
        }
    }

    public static List<String> tokenize(String str, char delim) {
        ArrayList<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == delim) {
                if (escaped) {
                    current.append(c);
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else {
                if (escaped && !current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                    escaped = false;
                }
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    /**
     * Unescape the given string
     */
    public static String unescape(CharSequence input, int quoteChar, boolean useAsciiEscapes, StringBuilder sb) {
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'b':
                        sb.append('\b');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 'u': {
                        int startIndex = i + 1;
                        int endIndex = startIndex + 4;
                        if (endIndex <= input.length()) {
                            String hex = input.subSequence(startIndex, endIndex).toString();
                            try {
                                int unicode = Integer.parseInt(hex, 16);
                                sb.append((char) unicode);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid Unicode escape: \\u" + hex);
                            }
                            i += 4;
                        } else {
                            throw new IllegalArgumentException("Incomplete Unicode escape sequence at position " + i);
                        }
                        break;
                    }
                    default:
                        if (c == quoteChar) {
                            sb.append((char) quoteChar);
                        } else if (useAsciiEscapes && Character.digit(c, 8) != -1) {
                            // Parse up to 3 octal digits
                            int octalValue = Character.digit(c, 8);
                            int j = 1;
                            while (j < 3 && (i + j) < input.length()) {
                                char nextChar = input.charAt(i + j);
                                int digit = Character.digit(nextChar, 8);
                                if (digit == -1) break;
                                octalValue = (octalValue << 3) + digit;
                                j++;
                            }
                            sb.append((char) octalValue);
                            i += (j - 1);
                        } else {
                            sb.append(c); // Unknown escape, keep as is
                        }
                }
                escaped = false;
            } else {
                if (c == '\\') {
                    escaped = true;
                } else if (c == quoteChar) {
                    break; // Stop at unescaped quote
                } else {
                    sb.append(c);
                }
            }
        }

        if (escaped) {
            throw new IllegalArgumentException("Incomplete escape sequence at end of input" );
        }

        return sb.toString();
    }


    private static int parseNumericValue(
            CharSequence string,
            StringBuilder sb,
            int i,
            int value,
            int possibleDigits,
            int radixExp) {

        for (int j = 0; j < possibleDigits; j++) {
            if (i + 1 == string.length()) {
                break;
            }
            char digit = string.charAt(i + 1);
            int val = Character.digit(digit, 1 << radixExp);
            if (val == -1) {
                break;
            }
            i++;
            value = (value << radixExp) + val;
        }
        sb.append((char) value);
        return i;
    }

}
