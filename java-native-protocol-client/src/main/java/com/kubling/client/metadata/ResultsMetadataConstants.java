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

package com.kubling.client.metadata;

/**
 * Constants in this class indicate the column positions of different
 * metadata columns in queries against Runtime metadata. These constants are
 * used in ResultsMetadataImpl object and in the JDBC driver to obtain column specific
 * metadata information.
 */

public interface ResultsMetadataConstants {

    // constant indicating the position of catalog or Virtual database name.
    Integer VIRTUAL_DATABASE_NAME = 0;
    // constant indicating the position of schema or Virtual database version.
    Integer VIRTUAL_DATABASE_VERSION = 1;
    // constant indicating the position of table or group name.
    Integer GROUP_NAME = 2;
    // constant indicating the position of column or element name.
    Integer ELEMENT_NAME = 3;
    // constant indicating the position of column label used for display purposes.
    Integer ELEMENT_LABEL = 4;
    // constant indicating the position of datatype of the column.
    Integer DATA_TYPE = 6;
    // constant indicating the position of precision of the column.
    Integer PRECISION = 7;
    // constant indicating the position of radix of a column.
    Integer RADIX = 8;
    // constant indicating scale of the column.
    Integer SCALE = 9;
    // constant indicating the position of auto-incrementable property of a column.
    Integer AUTO_INCREMENTING = 10;
    // constant indicating the position of columns case sensitivity.
    Integer CASE_SENSITIVE = 11;
    // constant indicating the position of nullable property of a column.
    Integer NULLABLE = 12;
    // constant indicating the position of searchable property of a column.
    Integer SEARCHABLE = 13;
    // constant indicating the position of signed property of a column.
    Integer SIGNED = 14;
    // constant indicating the position of updatable property of a column.
    Integer WRITABLE = 15;
    // constant indicating if a column is a currency value
    Integer CURRENCY = 16;
    // constant indicating the display size for a column
    Integer DISPLAY_SIZE = 17;

    /**
     * These types are associated with a DataType or an Element needing the indication of null types.
     */
    final class NULL_TYPES {
        public static final Integer NOT_NULL = 1;
        public static final Integer NULLABLE = 2;
        public static final Integer UNKNOWN = 3;
    }

    /**
     * These types are associated with the Element having valid search types.
     */
    final class SEARCH_TYPES {
        public static final Integer SEARCHABLE = 1;
        public static final Integer ALLEXCEPTLIKE = 2;
        public static final Integer LIKE_ONLY = 3;
        public static final Integer UNSEARCHABLE = 4;
    }

}
