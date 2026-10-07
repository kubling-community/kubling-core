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


import java.io.Serial;

/**
 * A generic runtime exception which contains a reference to another exception
 * and which represents a condition that should never occur during runtime.  This
 * class can be used to maintain a linked list of exceptions. <p>
 * <p>
 * Subclasses of this exception typically only need to implement whatever
 * constructors they need. <p>
 */
public class KublingRuntimeException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -4035276728007979320L;

    /**
     * @deprecated This value is unused and retained only for binary compatibility.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    public static final String CAUSED_BY_STRING = CorePlugin.Util.getString("RuntimeException.Caused_by");

    //############################################################################################################################
    //# Variables                                                                                                                #
    //############################################################################################################################

    /**
     * An error code.
     */
    private String code;

    //############################################################################################################################
    //# Constructors                                                                                                             #
    //############################################################################################################################

    /**
     * Construct a default instance of this class.
     */
    public KublingRuntimeException() {
    }

    /**
     * Construct an instance with the specified error message.  If the message is actually a key, the actual message will be
     * retrieved from a resource bundle using the key, the specified parameters will be substituted for placeholders within the
     * message, and the code will be set to the key.
     *
     * @param message The error message or a resource bundle key
     */
    public KublingRuntimeException(final String message) {
        super(message);
    }

    KublingRuntimeException(final String code, final String message) {
        super(message);
        // The following setCode call should be executed after setting the message
        setCode(code);
    }

    public KublingRuntimeException(BundleUtil.Event code, final String message) {
        super(message);
        // The following setCode call should be executed after setting the message
        setCode(code.toString());
    }

    public KublingRuntimeException(BundleUtil.Event code, final Throwable t) {
        super(t);
        // The following setCode call should be executed after setting the message
        setCode(code.toString());
    }

    /**
     * Construct an instance with a linked exception specified.  If the exception is a {@link KublingException},
     * then the code will be set to the exception's code.
     *
     * @param e An exception to chain to this exception
     */
    public KublingRuntimeException(final Throwable e) {
        super((e instanceof java.lang.reflect.InvocationTargetException)
                ? ((java.lang.reflect.InvocationTargetException) e).getTargetException().getMessage()
                : (e == null ? null : e.getMessage()), e);
        setCode(KublingException.getCode(e));
    }

    /**
     * Construct an instance with the linked exception, error code, and error message specified. If the specified
     * exception is a {@link KublingException} or a MetaMatrixRuntimeException, the code will
     * be set to the exception's code.
     *
     * @param e       The exception to chain to this exception
     * @param event   The error code
     * @param message The error message
     */
    public KublingRuntimeException(BundleUtil.Event event, final Throwable e, final String message) {
        super(message, e);
        // Overwrite code set in other ctor from exception.
        setCode(event.toString());
    }


    //############################################################################################################################
    //# Methods                                                                                                                  #
    //############################################################################################################################

    /**
     * Get the error code.
     *
     * @return The error code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Set the error code.
     *
     * @param code the error code
     */
    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        String message = super.getMessage();
        if (code == null || code.isEmpty() || message.startsWith(code)) {
            return message;
        }
        return code + " " + message;
    }

}
