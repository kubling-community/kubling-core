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

package com.kubling.jdbc;

import com.kubling.client.SourceWarning;
import com.kubling.core.KublingException;

import java.sql.SQLWarning;
import java.util.List;


/**
 * Utilities for creating SQLWarnings.
 */
class WarningUtil {

    private WarningUtil() {
    }

    /**
     * Used to wrap warnings/exceptions into SQLWarning.
     * The chain of warnings is translated into a chain of SQLWarnings.
     *
     * @param ex Throwable object which needs to be wrapped.
     */
    static SQLWarning createWarning(Throwable ex) {
        String sourceName = null;
        String modelName = null;
        if (ex instanceof SourceWarning exception) {
            if (exception.isPartialResultsError()) {
                PartialResultsWarning warning = new PartialResultsWarning(JDBCPlugin.Util.getString("WarningUtil.Failures_occurred"));
                warning.addConnectorFailure(exception.getConnectorBindingName(), KublingSQLException.create(exception));
                return warning;
            }
            ex = exception.getCause();
            sourceName = exception.getConnectorBindingName();
            modelName = exception.getModelName();
        }
        String code = null;
        if (ex instanceof KublingException) {
            code = ((KublingException) ex).getCode();
        }
        return new KublingSQLWarning(ex.getMessage(), code, ex, sourceName, modelName);
    }

    /**
     * Convert a List of warnings from the server into a single SQLWarning chain.
     *
     * @param exceptions List of exceptions from server
     * @return Chain of SQLWarning corresponding to list of exceptions
     */
    static SQLWarning convertWarnings(List<Throwable> exceptions) {
        if (exceptions == null || exceptions.isEmpty()) {
            return null;
        }
        SQLWarning root = createWarning(exceptions.getFirst());
        SQLWarning current = root;
        for (int i = 1; i < exceptions.size(); i++) {
            SQLWarning newWarning = createWarning(exceptions.get(i));
            current.setNextWarning(newWarning);
            current = newWarning;
        }
        return root;
    }
}
