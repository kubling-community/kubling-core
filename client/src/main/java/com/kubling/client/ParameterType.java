/*
 * Copyright Kubling and/or its affiliates
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

package com.kubling.client;

import java.util.Objects;

/**
 * Describes the JDBC and logical type explicitly assigned to a prepared
 * statement parameter. The parameter value is transported separately.
 */
public final class ParameterType {

    private final int jdbcType;
    private final String typeName;

    public ParameterType(int jdbcType, String typeName) {
        this.jdbcType = jdbcType;
        this.typeName = typeName;
    }

    public int getJdbcType() {
        return jdbcType;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ParameterType other)) {
            return false;
        }
        return jdbcType == other.jdbcType && Objects.equals(typeName, other.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jdbcType, typeName);
    }

    @Override
    public String toString() {
        return "ParameterType[jdbcType=" + jdbcType + ", typeName=" + typeName + ']';
    }
}
