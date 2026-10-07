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

package com.kubling.client.util;

import com.kubling.client.SourceWarning;
import com.kubling.core.ExecutionDiagnosticException;
import com.kubling.core.KublingException;
import com.kubling.core.KublingRuntimeException;
import com.kubling.jdbc.JDBCPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestExceptionUtil {

    @Test
    public void testSanitizeDiagnosticSourceWarnings() {
        for (boolean partial : new boolean[]{false, true}) {
            ExecutionDiagnosticException diagnostic = new ExecutionDiagnosticException(
                    "SOURCE_UNAVAILABLE", "one target is unavailable", "target-1", true);
            SourceWarning original = new SourceWarning("model-1", "connector-1", diagnostic, partial);

            SourceWarning copy = assertInstanceOf(SourceWarning.class, ExceptionUtil.sanitize(original, false));
            assertNotSame(original, copy);
            assertEquals("model-1", copy.getModelName());
            assertEquals("connector-1", copy.getConnectorBindingName());
            assertEquals(partial, copy.isPartialResultsError());
            assertEquals(0, copy.getStackTrace().length);

            ExecutionDiagnosticException cause =
                    assertInstanceOf(ExecutionDiagnosticException.class, copy.getCause());
            assertNotSame(diagnostic, cause);
            assertEquals("SOURCE_UNAVAILABLE", cause.getCode());
            assertEquals("one target is unavailable", cause.getMessage());
            assertEquals("target-1", cause.getTarget());
            assertTrue(cause.isRetryable());
            assertNull(cause.getCause());
            assertEquals(0, cause.getStackTrace().length);
        }
    }

    @Test
    public void testSanitizeOrdinarySourceWarningKeepsLegacyCauseSanitization() {
        SourceWarning original = new SourceWarning("model-2", "connector-2",
                new Exception("internal detail"), true);

        SourceWarning copy = assertInstanceOf(SourceWarning.class, ExceptionUtil.sanitize(original, false));
        assertEquals("model-2", copy.getModelName());
        assertEquals("connector-2", copy.getConnectorBindingName());
        assertTrue(copy.isPartialResultsError());
        KublingException legacyCause = assertInstanceOf(KublingException.class, copy.getCause());
        assertNull(legacyCause.getCode());
        assertNull(legacyCause.getMessage());
        assertEquals("java.lang.Exception", legacyCause.getCause().getMessage());
        assertEquals(0, legacyCause.getStackTrace().length);
    }

    @Test
    public void testSanitizeRuntimeExceptionPreservesCode() {
        KublingRuntimeException original = new KublingRuntimeException(
                JDBCPlugin.Event.KBL20000, "internal detail");

        KublingRuntimeException copy = assertInstanceOf(
                KublingRuntimeException.class, ExceptionUtil.sanitize(original, false));
        assertNotSame(original, copy);
        assertEquals("KBL20000", copy.getCode());
        assertEquals("KBL20000", copy.getMessage());
        assertEquals(0, copy.getStackTrace().length);
    }

    @Test
    public void testSanitize() {
        KublingException te = new KublingException(JDBCPlugin.Event.KBL20000, "you don't want to see this");
        te.initCause(new Exception("or this"));

        Throwable t = ExceptionUtil.sanitize(te, true);
        assertTrue(t.getStackTrace().length != 0);
        assertNotNull(t.getCause());
        assertEquals("KBL20000", t.getMessage());
        assertEquals("java.lang.Exception", t.getCause().getMessage());

        t = ExceptionUtil.sanitize(te, false);
        assertEquals(0, t.getStackTrace().length);
        assertEquals("KBL20000", t.getMessage());
    }

}
