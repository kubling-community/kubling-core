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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * This interface provides methods in
 * addition to the standard JDBC methods.
 */
public interface KublingPreparedStatement extends PreparedStatement {

    /**
     * Execute the given statement using a non-blocking callback.
     * This method is only valid for use with embedded connections.
     * <p>
     * Note that a single Statement may only have 1 async query executing at a time.
     */
    void submitExecute(StatementCallback callback, RequestOptions options) throws SQLException;

}
