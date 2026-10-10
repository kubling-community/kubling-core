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

import java.io.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains static methods that are routinely and commonly used in many test cases, and related to methods to test
 * common functionality or to perform common tests.
 * <p>
 * <em><b>This class should not be used by plug-in based tests<b><em>
 */
public class UnitTestUtil {

    public static final class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            final StringBuilder result = new StringBuilder();
            result.append(new Timestamp(record.getMillis()));
            result.append(" ");
            result.append(record.getLoggerName());
            result.append(" ");
            result.append(record.getLevel());
            result.append(" ");
            result.append(Thread.currentThread().getName());
            result.append(" ");
            result.append(record.getMessage());
            result.append('\n');
            if (record.getThrown() != null) {
                record.getThrown().printStackTrace(new PrintWriter(new Writer() {

                    @Override
                    public void close() {

                    }

                    @Override
                    public void flush() {

                    }

                    @Override
                    public void write(char[] cbuf, int off, int len) {
                        result.append(new String(cbuf, off, len));
                    }
                }));
                result.append('\n');
            }
            return result.toString();
        }
    }

    public static final String PATH_SEPARATOR = "/";

    private static final String DEFAULT_TESTDATA_PATH = "src/test/resources";
    //    private static final String DEFAULT_TEMP_DIR = "target/scratch";
    private static final String DEFAULT_TEMP_DIR = "scratch";

    //============================================================================================================================
    // Static Methods

    /**
     * <p>
     * This method attempts to check all of the requirements relating to equivalence.
     *
     * <p>
     * If <code>obj1</code> is not null, then the following tests are performed:
     * <li><code>obj1.equals(obj1) == true</code></li>
     * <li><code>obj1.equals(null) == false</code></li>
     *
     * <p>
     * If <code>obj2</code> is not null, then the following tests are performed:
     * <li><code>obj2.equals(obj2) == true</code></li>
     * <li><code>obj2.equals(null) == false</code></li>
     *
     * <p>
     * If both <code>obj1</code> and <code>obj2</code> are not null, then the following tests are performed:
     * <li><code>obj1.equals(obj2) == obj2.equals(obj1)</code></li>
     * <li><code>obj1.equals(obj2) == true</code> if <code>correctCompareToResult==0</code> or <code>obj1.equals(obj2) == false</code>
     * if <code>correctCompareToResult!=0</code></li>
     * <li><code>obj2.equals(obj1) == true</code> if <code>correctCompareToResult==0</code> or <code>obj2.equals(obj1) == false</code>
     * if <code>correctCompareToResult!=0</code></li>
     * <li><code>obj1.hashCode() == obj2.hashCode()</code> if <code>correctCompareToResult==0</code></li>
     *
     * <p>
     * Finally, if either <code>obj1</code> and <code>obj2</code> are instances of {@link Comparable}, then the following
     * tests are performed:
     * <li><code>obj1.compareTo(obj2) == correctCompareToResult</code>, or <code>obj1.compareTo(obj2) throws an IllegalArgumentException
     * if <code>obj2</code> is null</li>
     * <li><code>obj2.compareTo(obj1) == (-1 * correctCompareToResult)</code>, or
     * <code>obj2.compareTo(obj1) throws an IllegalArgumentException
     * if <code>obj2</code> is null</li></li>
     * <li><code>obj1.compareTo(obj1) == 0</code> if <code>obj1 != null</code></li>
     * <li><code>obj2.compareTo(obj2) == 0</code> if <code>obj2 != null</code></li>
     *
     * @param correctCompareToResult 0 if the two comparable objects should be
     *                               equivalent; &lt0 if <code>obj1<code> is to be considered less-than <code>obj2</code>;
     *                               >0 if <code>obj2<code> is to be considered less-than <code>obj1</code>.  If <code>obj1</code>
     *                               and <code>obj2</code> are not instances of {@link Comparable}, then it only matters that
     *                               this parameter is zero or non-zero (whether it's greater or less-than zero is not important).
     * @param obj1                   the reference to the first Object object; may be null, but no tests will be performed
     * @param obj2                   the reference to the second Object object; may be null
     */
    public static void helpTestEquivalence(final int correctCompareToResult,
                                           final Object obj1,
                                           final Object obj2) {
        if (obj1 != null) {

            if (obj2 != null) {
                // Test that equals is reflexive
                helpTestEquals(obj1);

                // Test that equals is symmetric ...
                helpTestEqualsTransitivity(obj1, obj2);
            }

            // If the objects are considered equal ...
            boolean equal = obj1.equals(obj2);
            if (equal) {
                if (correctCompareToResult != 0) {
                    fail("obj1.equals(obj2) returned true but was expected to return false");
                }

            } else {
                // They are not considered equal, so verify that this is what was expected
                if (correctCompareToResult == 0) {
                    fail("obj1.equals(obj2) returned false but was expected to return true");
                }
            }

            // Test the hashCode and whether it is compatible with equals.
            helpTestHashCode(equal, obj1, obj2);
        }

        if (obj2 != null) {

            // Test that equals is reflexive
            helpTestEquals(obj2);
        }

        // Finally (if both are comparable) then compareTo ...
        if (obj1 instanceof Comparable<?> || obj2 instanceof Comparable<?>) {
            Comparable<Object> comp1 = asComparable(obj1);
            Comparable<Object> comp2 = asComparable(obj2);
            helpTestCompareTo(correctCompareToResult, comp1, comp2);

            // Test that compareTo is reflexive
            helpTestReflexiveCompareTo(comp1); // doesn't do anything if obj1 == null
            helpTestReflexiveCompareTo(comp2); // doesn't do anything if obj2 == null
        }

    }

    //============================================================================================================================
    // Static Utility Methods

    /**
     * This method checks the 'compareTo' output of two Comparable objects, and uses an expected result to check the logic. This
     * test checks whether commutative calls to 'compareTo' are compatible. Note that either or both Comparable references may be
     * null; if only one of the references is null, this method checks that the 'compareTo' method throws an
     * IllegalArgumentException. If both are null, this method fails if the 'correctCompareToResult' is not 0.
     *
     * @param correctCompareToResult 0 if the two comparable objects should be equivalent; &lt0 if <code>obj1<code> is to be considered less-than <code>obj2</code>;
     *                               >0 if <code>obj2<code> is to be considered less-than <code>obj1</code>
     * @param obj1                   the reference to the first Comparable object; may be null
     * @param obj2                   the reference to the second Comparable object; may be null
     */
    protected static void helpTestCompareTo(final int correctCompareToResult,
                                            final Comparable<Object> obj1,
                                            final Comparable<Object> obj2) {
        if (obj1 != null) {
            if (obj2 != null) {
                // They are both non-null, so analyze the result of 'compareTo'
                int result1 = obj1.compareTo(obj2);
                int result2 = obj2.compareTo(obj1);

                // If the results are different ...
                if (result1 != result2) {
                    // Check that obj1.compareTo(obj2) returned the same sign as the expected result
                    if (result1 < 0) {
                        if (!(correctCompareToResult < 0)) {
                            fail("obj1.compareTo(obj2) returned <0 (actual=" + result1
                                    + ") and did not match the expected result (" + correctCompareToResult + ")");
                        }
                        if (!(result2 > 0)) {
                            fail("The compareTo call was not commutative: obj1.compareTo(obj2) returned <0 (actual=" + result1
                                    + ") but obj2.compareTo(obj2) did not return >0 (actual=" + result2 + ") ");
                        }
                    } else if (result1 > 0) {
                        if (!(correctCompareToResult > 0)) {
                            fail("obj1.compareTo(obj2) returned >0 (actual=" + result1
                                    + ") and did not match the expected result (" + correctCompareToResult + ")");
                        }
                        if (!(result2 < 0)) {
                            fail("The compareTo call was not commutative: obj1.compareTo(obj2) returned >0 (actual=" + result1
                                    + ") but obj2.compareTo(obj2) did not return <0 (actual=" + result2 + ") ");
                        }
                    } else { // result1 == 0
                        if (correctCompareToResult != 0) {
                            fail("obj1.compareTo(obj2) returned 0 but did not match the expected result ("
                                    + correctCompareToResult + ") and the second compareTo call didn't match the first");
                        }
                        fail("The compareTo call was not commutative: obj1.compareTo(obj2) returned 0 while obj2.compareTo(obj2) returned "
                                + result2);
                    }

                }
                // Otherwise the results are the same so correct 'compareTo' result should be 0
                else if (correctCompareToResult != 0) {
                    fail("obj1.compareTo(obj2) and obj2.compareTo(obj1) both returned " + result1
                            + " but did not match the expected result (" + correctCompareToResult + ")");
                }

            }
            // Otherwise, obj1 is NOT null but obj2 IS null
            else {
                int result = 0;
                try {
                    result = obj1.compareTo(obj2);
                    fail("The second comparable object was null but no IllegalArgumentException was thrown in "
                            + obj1.getClass().getName() + ".compareTo(Object)");
                } catch (IllegalArgumentException e) {
                    // Ignored
                }
                if (!(correctCompareToResult > 0)) {
                    fail("obj1.compareTo(null) returned " + result + " but did not match the expected result ("
                            + correctCompareToResult + ")");
                }
            }
        }
        // Else the obj1 IS null but obj2 is NOT null
        else if (obj2 != null) {
            int result = 0;
            try {
                result = obj2.compareTo(obj1);
                fail("The first comparable object was null but no IllegalArgumentException was thrown in "
                        + obj2.getClass().getName() + ".compareTo(Object)");
            } catch (IllegalArgumentException e) {
                // Ignored
            }
            if (!(correctCompareToResult < 0)) {
                fail("obj2.compareTo(null) returned " + result + " but did not match the expected result ("
                        + correctCompareToResult + ")");
            }
        }
        // Otherwise both are null
        else {
            // The correct result had better be 0
            if (correctCompareToResult != 0) {
                fail("The expected result was not 0 even though both references were null");
            }
        }
    }

    /**
     * Tests properties of the equals method implementation of the <code>obj</code> parameter. The equals method of <code>obj</code>
     * is tested for reflexivity, symmetry, and equality with <code>null</code>.
     *
     * @param obj Object whose equals method is tested
     * @throws org.opentest4j.AssertionFailedError if parameter is <code>null</code>, or if any test doesn't pass
     */
    protected static void helpTestEquals(final Object obj) {
        assertNotNull(obj);
        assertFalse(obj.equals(null), "The equals method of Object " + obj + " (Class " + obj.getClass().getName() +
                ") does not return false for null parameter");
        assertEquals(obj, obj, "The equals method of Object " + obj + " (Class " + obj.getClass().getName() + ") is not reflexive.");
    }

    /**
     * Tests transitivity property of the equals method implementation of the <code>test</code> parameter. The equals method of
     * <code>test</code> is tested for transitivity with the equals method of <code>control</code>. Note that <code>control</code>
     * can be either equal or not with <code>test</code>; what is tested is that their equals methods are consistent.
     *
     * @param test    Object whose equals method is tested
     * @param control object for testing transitivity of <code>test</code>'s equals method
     * @throws org.opentest4j.AssertionFailedError if either parameter is <code>null</code>, or if any test doesn't pass
     */
    protected static void helpTestEqualsTransitivity(final Object test,
                                                     final Object control) {
        assertNotNull(test);
        assertNotNull(control);
        assertEquals(test.equals(control), control.equals(test),
                "Equals methods of test Object and control Object are not symmetric: " + test + ", " + control);
    }

    /**
     * <p>
     * This method attempts to check that, if the expected result is that the objects are equal, the hashCode values of two
     * objects are also equal. Note that this method does not rely upon the 'equals' method to properly determine whether the two
     * objects are equal; rather, the first argument specifies whether the objects are expected to be equal.
     *
     * <p>
     * The hashCode of two objects should be the same if the two objects are considered equal using the
     * {@link Object#equals(Object) equals}method. If the objects are not equal, then it cannot be concluded
     * whether the two hash codes must be equivalent or different.
     *
     * @param shouldBeEqual true if the two comparable objects should be equivalent; or false otherwise
     * @param obj1          the reference to the first Object; may be null
     * @param obj2          the reference to the second Object; may be null
     */
    protected static void helpTestHashCode(final boolean shouldBeEqual,
                                           final Object obj1,
                                           final Object obj2) {
        // Run the test only if both are not null
        if (obj1 != null && obj2 != null) {

            // Check the hash codes ...
            int hash1 = obj1.hashCode();
            int hash2 = obj2.hashCode();
            if (shouldBeEqual) {
                assertEquals(hash1, hash2, "The two objects are supposed to be equal but do not have the same hash code value; obj1.hashCode()="
                        + hash1 + "; obj2.hashCode()=" + hash2);
            }
            // If they should not be equal, then it is NOT necessarily true
            // that the hash codes must be different. Therefore, nothing to test.
        }
    }

    /**
     * This method checks the reflexive nature of 'compareTo'; no check is performed if the input object is null.
     *
     * @param obj the reference to the Comparable object; may be null
     */
    protected static void helpTestReflexiveCompareTo(final Comparable<Object> obj) {
        if (obj != null && obj.compareTo(obj) != 0) {
            fail("The compareTo method is not reflexive; obj.compareTo(obj) does not equal 0");
        }
    }

    @SuppressWarnings("unchecked")
    private static Comparable<Object> asComparable(Object object) {
        return (Comparable<Object>) object;
    }

    //============================================================================================================================
    // Constructors

    /**
     * Can't construct - just utilities
     */
    protected UnitTestUtil() {
    }

    /**
     * Obtain a {@link File}for the file name in the test data directory (given by {@link #getTestDataPath()}).
     *
     * @param fileName A path and name relative to the test data directory; for example, "MyFile.txt" if the file is in the test
     *                 data directory, or "subfolder/MyFile.txt" if the file is in "subfolder".
     * @return The File referencing the file with the specified fileName within the test data directory
     */
    public static File getTestDataFile(String fileName) {
        return new File(UnitTestUtil.getTestDataPath(), fileName);
    }

    public static File getTargetTestDirDataFile(String fileName) {
        return new File("test-classes", fileName);
    }

    public static File getTestScratchFile(String fileName) {
        // Create the input stream ...
        String path = UnitTestUtil.getTestScratchPath();
        return new File(path, fileName);
    }

    public static String getTestDataPath() {
        return DEFAULT_TESTDATA_PATH;
    }

    /**
     * Obtain the file path to a scratch area where files may be created during testing.
     *
     * @return File path, never null
     */
    public static String getTestScratchPath() {
        String filePath = DEFAULT_TEMP_DIR;
        File scratch = new File(filePath);
        if (!scratch.exists() && !scratch.mkdirs()) {
            filePath = System.getProperty("java.io.tmpdir");

            if (filePath == null) {
                filePath = ".";
            }
        }
        File scratchDirectory = new File(filePath);
        if (!scratchDirectory.exists()) {
            scratchDirectory.mkdir();
        }
        return filePath;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T helpSerialize(T object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

        return (T) ois.readObject();
    }

    public static void enableTraceLogging(String loggerName) {
        enableLogging(Level.FINEST, loggerName);
    }

    static Map<String, Logger> loggers = new HashMap<>();

    public static void enableLogging(Level level, String loggerName) {
        Logger logger;
        synchronized (loggers) {
            logger = loggers.get(loggerName);
            if (logger == null) {
                logger = Logger.getLogger(loggerName);
                loggers.put(loggerName, logger);
            }
        }
        logger.setLevel(level);
        if (logger.getHandlers().length > 0) {
            for (Handler h : logger.getHandlers()) {
                h.setLevel(level);
            }
        } else {
            logger.setUseParentHandlers(false);
            ConsoleHandler ch = new ConsoleHandler();
            ch.setFormatter(new LogFormatter());
            ch.setLevel(level);
            logger.addHandler(ch);
        }
    }

}
