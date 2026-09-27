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

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

public class MetadataProvider {

    // Map of detail maps -- <columnIndex, Map<propertyName, metadataObject>>
    protected Map[] metadata;

    public MetadataProvider(Map[] metadata) {
        this.metadata = Objects.requireNonNullElseGet(metadata, () -> new Map[0]);
    }

    public Object getValue(int columnIndex, Integer metadataPropertyKey) throws SQLException {
        if (columnIndex < 0 || columnIndex >= metadata.length) {
            throw new SQLException(JDBCPlugin.Util.getString("StaticMetadataProvider.Invalid_column", columnIndex));
        }

        Map column = this.metadata[columnIndex];
        return column.get(metadataPropertyKey);
    }

    public int getColumnCount() {
        return metadata.length;
    }

    public String getStringValue(int columnIndex, Integer metadataPropertyKey) throws SQLException {
        return (String) getValue(columnIndex, metadataPropertyKey);
    }

    public int getIntValue(int columnIndex, Integer metadataPropertyKey) throws SQLException {
        return getIntValue(columnIndex, metadataPropertyKey, 0);
    }

    public int getIntValue(int columnIndex, Integer metadataPropertyKey, int defaultValue) throws SQLException {
        Integer val = (Integer) getValue(columnIndex, metadataPropertyKey);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    public boolean getBooleanValue(int columnIndex, Integer metadataPropertyKey) throws SQLException {
        return (Boolean) getValue(columnIndex, metadataPropertyKey);
    }

}
