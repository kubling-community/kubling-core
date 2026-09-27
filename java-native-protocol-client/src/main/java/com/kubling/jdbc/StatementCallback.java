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

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * A callback for non-blocking statement result processing.
 * {@link Statement#close()} must still be called to release
 * statement resources.
 * <p>
 * Statement methods, such as cancel, are perfectly valid
 * even when using a callback.
 */
public interface StatementCallback {

    /**
     * Process the current row of the {@link ResultSet}.
     * Any call that retrieves non-lob values from the current row
     * will be performed without blocking on more data from sources.
     * Calls outside the current row, such as next(), may block.
     */
    void onRow(Statement s, ResultSet rs) throws Exception;

    /**
     * Called when an exception occurs.  No further rows will
     * be processed by this callback.
     */
    void onException(Statement s, Exception e) throws Exception;

    /**
     * Called when processing has completed normally.
     */
    void onComplete(Statement s) throws Exception;

}
