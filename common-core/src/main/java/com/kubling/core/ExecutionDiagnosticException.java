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

import java.io.Serial;
import java.util.Objects;

/**
 * A client-safe diagnostic produced while executing a query. Its code is a
 * logical identifier, not a SQLState or a Kubling error code. The API used to
 * report this exception determines whether it is a general warning or a cause
 * of partial results.
 */
public final class ExecutionDiagnosticException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String target;
    private final boolean retryable;

    /**
     * Creates a diagnostic containing only information safe for a client.
     *
     * @param code      optional logical diagnostic identifier
     * @param message   client-safe explanation
     * @param target    optional client-safe logical target identifier
     * @param retryable whether retrying the affected target may succeed
     */
    public ExecutionDiagnosticException(String code, String message, String target, boolean retryable) {
        super(Objects.requireNonNull(message, "message"), null, false, false);
        this.code = code;
        this.target = target;
        this.retryable = retryable;
    }

    /**
     * @return the logical diagnostic identifier, if supplied
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the client-safe logical target identifier, if supplied
     */
    public String getTarget() {
        return target;
    }

    /**
     * @return whether retrying the affected target may succeed
     */
    public boolean isRetryable() {
        return retryable;
    }
}
