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
import com.kubling.client.xa.XATransactionException;
import com.kubling.core.KublingComponentException;
import com.kubling.core.KublingException;
import com.kubling.core.KublingRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public class ExceptionUtil {

    public static <T extends Throwable> T getExceptionOfType(Throwable ex, Class<T> cls) {
        while (ex != null) {
            if (cls.isAssignableFrom(ex.getClass())) {
                return cls.cast(ex);
            }
            if (ex.getCause() == ex) {
                break;
            }
            ex = ex.getCause();
        }
        return null;
    }

    public static Throwable convertException(Method method, Throwable exception) {
        boolean canThrowXATransactionException = false;
        boolean canThrowComponentException = false;
        Class<?>[] exceptionClasses = method.getExceptionTypes();
        for (Class<?> exceptionClass : exceptionClasses) {
            if (exceptionClass.isAssignableFrom(exception.getClass())) {
                return exception;
            }
            if (!canThrowComponentException) {
                canThrowComponentException = KublingComponentException.class.isAssignableFrom(exceptionClass);
            }
            if (!canThrowXATransactionException) {
                canThrowXATransactionException = XATransactionException.class.isAssignableFrom(exceptionClass);
            }
        }
        if (canThrowComponentException) {
            return new KublingComponentException(exception);
        }
        if (canThrowXATransactionException) {
            return new XATransactionException(exception);
        }
        if (RuntimeException.class.isAssignableFrom(exception.getClass())) {
            return exception;
        }
        return new KublingRuntimeException(exception);
    }

    /**
     * Strip out the message and optionally the stacktrace
     *
     * @param t
     * @return
     */
    public static Throwable sanitize(Throwable t, boolean preserveStack) {
        String code;
        if (t instanceof KublingException) {
            code = ((KublingException) t).getCode();
        } else if (t instanceof KublingRuntimeException) {
            code = ((KublingRuntimeException) t).getCode();
        } else {
            code = t.getClass().getName();
        }
        Throwable child = null;
        if (t.getCause() != null && t.getCause() != t) {
            child = sanitize(t.getCause(), preserveStack);
        }
        Class<?> clazz = t.getClass();
        Throwable result = null;
        while (clazz != null) {
            if (clazz == Throwable.class || clazz == Exception.class) {
                break;
            }
            Constructor<?> ctor;
            try {
                ctor = clazz.getDeclaredConstructor(String.class);
                result = (Throwable) ctor.newInstance(code);
                break;
            } catch (Exception e) {
                // Nothing to do here
            }
            clazz = clazz.getSuperclass();
        }
        if (result == null) {
            result = new KublingException(code);
        }
        if (result instanceof KublingException) {
            ((KublingException) result).setCode(code);
        } else if (result instanceof KublingRuntimeException) {
            ((KublingException) result).setCode(code);
        }
        if (child != null) {
            result.initCause(child);
        }
        if (preserveStack) {
            result.setStackTrace(t.getStackTrace());
        } else {
            result.setStackTrace(SourceWarning.EMPTY_STACK_TRACE);
        }
        return result;
    }
}
