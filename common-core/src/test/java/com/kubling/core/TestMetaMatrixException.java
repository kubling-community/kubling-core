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

package com.kubling.core;

import org.junit.jupiter.api.Test;

import java.io.Serial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the children Iterator of the MetaMatrixException.
 */
public class TestMetaMatrixException {

    @Test
    public void testMetaMatrixExceptionWithNullThrowable() {
        final KublingException err = new KublingException((Throwable) null);
        assertNull(err.getCode());
        assertNull(err.getMessage());

    }

    @Test
    public void testMetaMatrixExceptionWithMessage() {
        final KublingException err = new KublingException("Test");
        assertNull(err.getCode());
        assertEquals("Test", err.getMessage());

    }

    public enum Event implements BundleUtil.Event {
        Code,
        propertyValuePhrase,
    }

    @Test
    public void testMetaMatrixExceptionWithCodeAndMessage() {
        final KublingException err = new KublingException(Event.Code, "Test");
        assertEquals("Code", err.getCode());
        assertEquals("Code Test", err.getMessage());
    }


    @Test
    public void testMetaMatrixExceptionWithExceptionAndMessage() {
        final KublingException child = new KublingException(Event.propertyValuePhrase, "Child");
        final KublingException err = new KublingException(child, "Test");
        assertEquals("propertyValuePhrase", err.getCode());
        assertEquals("propertyValuePhrase Test", err.getMessage());

    }

    @Test
    public void testMetaMatrixExceptionWithExceptionAndCodeAndMessage() {
        final KublingException child = new KublingException(Event.propertyValuePhrase, "Child");
        final KublingException err = new KublingException(Event.Code, child, "Test");
        assertEquals("Code", err.getCode());
        assertEquals("Code Test", err.getMessage());

    }

    @Test
    public void testConstructorDoesNotInvokeOverridableSetter() {
        TrackingException err = new TrackingException(Event.Code, "Test");

        assertFalse(err.setterInvoked);
        assertEquals("Code", err.getCode());

        err.setCode("Other");
        assertTrue(err.setterInvoked);
        assertEquals("Other", err.getCode());
    }

    private static final class TrackingException extends KublingException {

        @Serial
        private static final long serialVersionUID = 1L;

        private boolean setterInvoked;

        private TrackingException(BundleUtil.Event code, String message) {
            super(code, message);
        }

        @Override
        public void setCode(String code) {
            setterInvoked = true;
            super.setCode(code);
        }
    }
}
