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
import java.sql.SQLException;


/**
 * Exception which occurs if an error occurs within the server that is not
 * business-related.  For instance, if a service or bean is not available
 * or communication fails.
 */
public class KublingException extends Exception {

    @Serial
    private static final long serialVersionUID = -3033427629587497938L;
    protected String code;
    private transient String originalType;

    public KublingException() {
    }

    public KublingException(String message) {
        super(message);
    }

    public KublingException(BundleUtil.Event code, final String message) {
        super(message);
        this.code = code.toString();
    }

    public KublingException(BundleUtil.Event code, Throwable t, final String message) {
        super(message, t);
        if (message != null && t != null && message.equals(t.getMessage())) {
            this.code = resolveCode(code, t);
        } else {
            this.code = code.toString();
        }
    }

    public KublingException(BundleUtil.Event code, Throwable t) {
        super(t);
        this.code = resolveCode(code, t);
    }

    private static String resolveCode(BundleUtil.Event code, Throwable t) {
        String codeStr = code.toString();
        if (t instanceof KublingException te) {
            if (te.getCode() != null) {
                codeStr = te.getCode();
            }
        }
        return codeStr;
    }

    public KublingException(Throwable e) {
        this(e, e != null ? e.getMessage() : null);
    }

    public KublingException(Throwable e, String message) {
        super(message, e);
        this.code = getCode(e);
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOriginalType() {
        return originalType;
    }

    public void setOriginalType(String originalType) {
        this.originalType = originalType;
    }

    static String getCode(Throwable e) {
        if (e instanceof KublingException) {
            return (((KublingException) e).getCode());
        } else if (e instanceof KublingRuntimeException) {
            return ((KublingRuntimeException) e).getCode();
        } else if (e instanceof SQLException) {
            return ((SQLException) e).getSQLState();
        }
        return null;
    }

    public String getMessage() {
        String message = super.getMessage();
        if (message == null) {
            return code;
        }
        if (code == null || code.isEmpty() || message.startsWith(code)) {
            return message;
        }
        return code + " " + message;
    }

}
