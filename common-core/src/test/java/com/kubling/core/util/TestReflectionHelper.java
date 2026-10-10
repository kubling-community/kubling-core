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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestReflectionHelper
 */
public class TestReflectionHelper {

    // =========================================================================
    //                      H E L P E R   M E T H O D S
    // =========================================================================

    /**
     * Verify the Class<?>[] arrays compare equal element for element.
     *
     * @param msg             msg to display
     * @param signatureSought
     * @param signatureFound
     * @since 4.4
     */
    private void helpAssertSameMethodSignature(String msg,
                                               Class<?>[] signatureSought,
                                               Class<?>[] signatureFound) {
        assertEquals(signatureSought.length, signatureFound.length, msg + ": sizes differ.");
        for (int i = 0; i < signatureSought.length; ++i) {
            assertEquals(signatureSought[i], signatureFound[i], msg + " for argument # " + (i + 1));
        }
    }

    // =========================================================================
    //                         T E S T     C A S E S
    // =========================================================================

    @Test
    public void testConstructorWithNullTargetClass() {
        try {
            new ReflectionHelper(null);
            fail("Should have caught null target class passed to constructor");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConstructorWithValidTargetClass() {
        final ReflectionHelper helper = new ReflectionHelper(String.class);
        assertNotNull(helper);
    }

    //  ===============================================================================================
    //   Test overloaded methods
    //  ===============================================================================================
    @Test
    public void testFindBestMethodWithSignature_String() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{String.class};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_Serializable() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Serializable.class};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Serializable.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_Object() {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{NullPointerException.class};
        try {
            helper.findBestMethodWithSignature("method", signatureSought);
            fail("exception expected");
        } catch (NoSuchMethodException e) {
            assertEquals("method Args: [class java.lang.NullPointerException] has multiple possible signatures.", e.getMessage());
        }
    }

    @Test
    public void testFindBestMethodWithSignature_StringArray() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{String[].class};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_Integer() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Integer.class};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_long() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Long.TYPE};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName(), "Wrong class");
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_2ArgSerializableAndNumber() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Integer.class, Integer.class};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Serializable.class, Number.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    //  ===============================================================================================
    //   Test explicit method names
    //  ===============================================================================================
    @Test
    public void testFindBestMethodWithSignature_StringAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{String.class};
        Method theMethod = helper.findBestMethodWithSignature("methodString", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_ObjectAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{NullPointerException.class};
        Method theMethod = helper.findBestMethodWithSignature("methodObject", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Object.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_SerializableAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{NullPointerException.class};
        Method theMethod = helper.findBestMethodWithSignature("methodSerializable", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Serializable.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_ObjectSerializableAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{NullPointerException.class};
        Method theMethod = helper.findBestMethodWithSignature("methodSerializable", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Serializable.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_IntegerSerializableAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Integer.class};
        Method theMethod = helper.findBestMethodWithSignature("methodSerializable", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Serializable.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_StringArrayAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{String[].class};
        Method theMethod = helper.findBestMethodWithSignature("methodStringArray", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_ListAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{ArrayList.class};
        Method theMethod = helper.findBestMethodWithSignature("methodList", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{List.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_IntegerAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Integer.class};
        Method theMethod = helper.findBestMethodWithSignature("methodInteger", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_IntegerObjectAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Integer.class};
        Method theMethod = helper.findBestMethodWithSignature("methodObject", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Object.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_LongObjectAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Long.class};
        Method theMethod = helper.findBestMethodWithSignature("methodObject", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Object.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_longAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Long.TYPE};
        Method theMethod = helper.findBestMethodWithSignature("method_long", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    //  ===============================================================================================
    //   Test 2-arg methods
    //  ===============================================================================================
    @Test
    public void testFindBestMethodWithSignature_2ArgIntegerObjectAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Integer.class, Integer.class};
        Method theMethod = helper.findBestMethodWithSignature("twoArgMethod_Object_Object", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Object.class, Object.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_2ArgLongObjectAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Long.class, Long.class};
        Method theMethod = helper.findBestMethodWithSignature("twoArgMethod_Object_Object", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Object.class, Object.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_2ArgSerializableNumberAndMethodName() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Long.class, Long.class};
        Method theMethod = helper.findBestMethodWithSignature("twoArgMethod_Serializable_Number", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Serializable.class, Number.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    //  ===============================================================================================
    //   Test overridden methods
    //  ===============================================================================================
    @Test
    public void testFindBestMethodWithSignature_SubInterface_2ArgSerializableAndNumber() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeSubInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Serializable.class, Number.class};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        assertEquals(theMethod.getDeclaringClass().getName(), FakeSubInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureSought, signatureFound);
    }

    @Test
    public void testFindBestMethodWithSignature_SubInterface_2ArgSerializableAndLong() throws Exception {
        ReflectionHelper helper = new ReflectionHelper(FakeSubInterface.class);
        Class<?>[] signatureSought = new Class<?>[]{Serializable.class, Long.class};
        Method theMethod = helper.findBestMethodWithSignature("method", signatureSought);
        assertNotNull(theMethod, "Failed to find method for args: " + Arrays.toString(signatureSought));

        // make sure we got the method we expected
        Class<?>[] signatureFound = theMethod.getParameterTypes();
        Class<?>[] signatureExpected = new Class<?>[]{Serializable.class, Long.class};
        assertEquals(theMethod.getDeclaringClass().getName(), FakeSubInterface.class.getName());
        helpAssertSameMethodSignature("Found wrong method signature", signatureExpected, signatureFound);
    }

    @Test
    public void testCreate() throws Exception {
        ReflectionHelper.create(SomeClass.class.getName(), Arrays.asList(true), null);
    }

    /**
     * Test base interface
     */
    public interface FakeInterface {

        void method(String arg);

        void method(Serializable arg);

        void method(Object arg);

        void method(String[] arg);

        void method(List<?> arg);

        void method(Integer arg);

        void method(long arg);

        void method(Serializable arg1, Number arg2);

        void methodString(String arg);

        void methodSerializable(Serializable arg);

        void methodObject(Object arg);

        void methodStringArray(String[] arg);

        void methodList(List<?> arg);

        void methodInteger(Integer arg);

        void method_long(long arg);

        void twoArgMethod_Object_Object(Object arg1, Object arg2);

        void twoArgMethod_Serializable_Number(Serializable arg1, Number arg2);
    }

    /**
     * Test sub interface
     */
    public interface FakeSubInterface extends FakeInterface {

        void method(Number arg1, Long arg2);

        void method(Serializable arg1, Number arg2);

        void method(Serializable arg1, Long arg2);
    }

    public static class SomeClass {
        public SomeClass(boolean primArg) {
        }
    }
}
