/*
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

package com.kubling.core;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class TestExecutionDiagnosticException {

    @Test
    void testSerializableClientSafeFields() throws Exception {
        ExecutionDiagnosticException original = new ExecutionDiagnosticException(
                "SOURCE_UNAVAILABLE", "one logical target is unavailable", "target-1", true);
        assertEquals(0, original.getStackTrace().length);
        assertNull(original.getCause());
        assertThrows(IllegalStateException.class, () -> original.initCause(new Exception("internal")));
        assertEquals(1L, ObjectStreamClass.lookup(ExecutionDiagnosticException.class).getSerialVersionUID());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        ExecutionDiagnosticException copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (ExecutionDiagnosticException) in.readObject();
        }

        assertEquals("SOURCE_UNAVAILABLE", copy.getCode());
        assertEquals("one logical target is unavailable", copy.getMessage());
        assertEquals("target-1", copy.getTarget());
        assertTrue(copy.isRetryable());
        assertEquals(0, copy.getStackTrace().length);
        assertNull(copy.getCause());
    }

    @Test
    void testOptionalFieldsAreNotNormalized() {
        ExecutionDiagnosticException empty = new ExecutionDiagnosticException(
                "", "safe message", "", false);
        assertEquals("", empty.getCode());
        assertEquals("", empty.getTarget());
        assertFalse(empty.isRetryable());

        ExecutionDiagnosticException absent = new ExecutionDiagnosticException(
                null, "safe message", null, false);
        assertNull(absent.getCode());
        assertNull(absent.getTarget());
        assertThrows(NullPointerException.class,
                () -> new ExecutionDiagnosticException(null, null, null, false));
    }
}
