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

/* <p> This class contains constants indicating names of the columns in the
 *  result sets returned by methods on DatabaseMetaData. Each inner class represents
 *  a particular method and the class attributes give the names of the columns on
 *  methods ResultSet.
 */

interface JDBCColumnNames {

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getCatalogs method on DatabaseMetaData. These constant values
     * are be used for the column names used in constructing the ResultSet obj.
     */
    interface CATALOGS {
        //  name of the column containing catalog or Virtual database name.
        String TABLE_CAT = "TABLE_CAT";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getColumns method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in construction the ResultSet obj.
     */
    interface COLUMNS {

        //  name of the column containing catalog or Virtual database name.
        String TABLE_CAT = "TABLE_CAT";

        //  name of the column containing schema or Virtual database version.
        String TABLE_SCHEM = "TABLE_SCHEM";

        //  name of the column containing table or group name.
        String TABLE_NAME = "TABLE_NAME";

        //  name of the column containing column or element name.
        String COLUMN_NAME = "COLUMN_NAME";

        /**
         * name of column that contains SQL type from java.sql.Types for column's data type.
         */
        String DATA_TYPE = "DATA_TYPE";

        /**
         * name of column that contains local type name used by the data source.
         */
        String TYPE_NAME = "TYPE_NAME";

        //  name of the column containing column size.
        String COLUMN_SIZE = "COLUMN_SIZE";

        /**
         * name of column that is not used will contain nulls
         */
        String BUFFER_LENGTH = "BUFFER_LENGTH";

        //  name of the column containing number of digits to right of decimal
        String DECIMAL_DIGITS = "DECIMAL_DIGITS";

        //  name of the column containing column's Radix.
        String NUM_PREC_RADIX = "NUM_PREC_RADIX";

        /**
         * name of column that has a String value indicating nullablity
         */
        String NULLABLE = "NULLABLE";

        /**
         * name of column containing explanatory notes.
         */
        String REMARKS = "REMARKS";

        /**
         * name of column which contains default value for the column.
         */
        String COLUMN_DEF = "COLUMN_DEF";

        /**
         * name of column that not used will contain nulls
         */
        String SQL_DATA_TYPE = "SQL_DATA_TYPE";

        /**
         * name of column that not used will contain nulls
         */
        String SQL_DATETIME_SUB = "SQL_DATETIME_SUB";

        /**
         * name of column that stores the max number of bytes in the column
         */
        String CHAR_OCTET_LENGTH = "CHAR_OCTET_LENGTH";

        /**
         * name of column that stores the index of a column in the table
         */
        String ORDINAL_POSITION = "ORDINAL_POSITION";

        /**
         * name of column that has a String value indicating nullablity
         */
        String IS_NULLABLE = "IS_NULLABLE";

        /**
         * name of column that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
         */
        String SCOPE_CATLOG = "SCOPE_CATLOG";

        /**
         * name of column that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
         */
        String SCOPE_SCHEMA = "SCOPE_SCHEMA";

        /**
         * name of column that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
         */
        String SCOPE_TABLE = "SCOPE_TABLE";

        /**
         * name of column that is source type of distinct type or user-generated Ref type, SQL type
         * from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
         */
        String SOURCE_DATA_TYPE = "SOURCE_DATA_TYPE";

        /**
         * name of column that has a String value indicating format
         */
        String FORMAT = "FORMAT";

        /**
         * name of column that has an String value indicating minimum range
         */
        String MIN_RANGE = "MIN_RANGE";

        /**
         * name of column that has an String value indicating maximum range
         */
        String MAX_RANGE = "MAX_RANGE";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getColumnPrivileges and getTablePrivileges methods on
     * DatabaseMetaData. These constant values are be used to hardcode the column
     * names used in constructing the ResultSet obj.
     */
    interface PRIVILEGES {

        //  name of the column containing catalog or Virtual database name.
        String TABLE_CAT = "TABLE_CAT";

        //  name of the column containing schema or Virtual database version.
        String TABLE_SCHEM = "TABLE_SCHEM";

        //  name of the column containing table or group name.
        String TABLE_NAME = "TABLE_NAME";

        //  name of the column containing column or element name.
        String COLUMN_NAME = "COLUMN_NAME";

        //  name of the column containing Grantor name
        String GRANTOR = "GRANTOR";

        //  name of the column containing Grantee name
        String GRANTEE = "GRANTEE";

        //  name of the column containing privilege name
        String PRIVILEGE = "PRIVILEGE";

        //  name of the column containing privilege grantable info
        String IS_GRANTABLE = "IS_GRANTABLE";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getCrossReference, getExportedKeys, and getImportedKeys methods
     * on DatabaseMetaData. These constant values are be used to hardcode the
     * column names used in constructing the ResultSet obj.
     */
    interface REFERENCE_KEYS {

        //  name of the column containing catalog or Virtual database name for primary key's table.
        String PKTABLE_CAT = "PKTABLE_CAT";

        //  name of the column containing schema or Virtual database version for primary key's table.
        String PKTABLE_SCHEM = "PKTABLE_SCHEM";

        //  name of the column containing table or group name for primary key's table.
        String PKTABLE_NAME = "PKTABLE_NAME";

        // name of the column containing column or element name of the primary key.
        String PKCOLUMN_NAME = "PKCOLUMN_NAME";

        // name of the column containing catalog or Virtual database name for foreign key's table.
        String FKTABLE_CAT = "FKTABLE_CAT";

        // name of the column containing schema or Virtual database version for foreign key's table.
        String FKTABLE_SCHEM = "FKTABLE_SCHEM";

        // name of the column containing table or group name for foreign key's table.
        String FKTABLE_NAME = "FKTABLE_NAME";

        // name of the column containing column or element name of the foreign key.
        String FKCOLUMN_NAME = "FKCOLUMN_NAME";

        // name of the column containing sequence number within the foreign key
        String KEY_SEQ = "KEY_SEQ";

        // name of the column containing effect on foreign key when PK is updated.
        String UPDATE_RULE = "UPDATE_RULE";

        // name of the column containing effect on foreign key when PK is deleted.
        String DELETE_RULE = "DELETE_RULE";

        // name of the column containing name of the foreign key.
        String FK_NAME = "FK_NAME";

        // name of the column containing name of the primary key.
        String PK_NAME = "PK_NAME";

        // name of the column containing deferability of foreign key constraStrings.
        String DEFERRABILITY = "DEFERRABILITY";
        String FKPOSITION = "FKPOSITION";
    }


    /**
     * This class contains constants representing column names on ResultSet
     * returned by getPrimaryKeys method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructin the ResultSet obj.
     */
    interface PRIMARY_KEYS {

        // name of the column containing catalog or Virtual database name.
        String TABLE_CAT = "TABLE_CAT";

        // name of the column containing schema or Virtual database version.
        String TABLE_SCHEM = "TABLE_SCHEM";

        // name of the column containing table or group name.
        String TABLE_NAME = "TABLE_NAME";

        // name of the column containing column or element name.
        String COLUMN_NAME = "COLUMN_NAME";

        // name of the column containing sequence number within the primary key
        String KEY_SEQ = "KEY_SEQ";

        // name of the column containing name of the primary key.
        String PK_NAME = "PK_NAME";
        String POSITION = "POSITION";
    }


    /**
     * This class contains constants representing column names on ResultSet
     * returned by getProcedureColumns method on DatabaseMetaData. These constant
     * values are be used to hardcode the column names used in constructin the
     * ResultSet obj.
     */
    interface PROCEDURE_COLUMNS {

        // name of the column containing procedure catalog or Virtual database name.
        String PROCEDURE_CAT = "PROCEDURE_CAT";

        // name of the column containing schema or Virtual database version.
        String PROCEDURE_SCHEM = "PROCEDURE_SCHEM";

        // name of the column containing table or group name.
        String PROCEDURE_NAME = "PROCEDURE_NAME";

        // name of the column containing column or element name.
        String COLUMN_NAME = "COLUMN_NAME";

        // name of the column containing column or element type.
        String COLUMN_TYPE = "COLUMN_TYPE";

        /**
         * name of column that contains SQL type from java.sql.Types for column's data type.
         */
        String DATA_TYPE = "DATA_TYPE";

        /**
         * name of column that contains local type name used by the data source.
         */
        String TYPE_NAME = "TYPE_NAME";

        // name of the column containing number of digits to right of decimal
        String PRECISION = "PRECISION";

        /**
         * name of column that that contains length of data in bytes
         */
        String LENGTH = "LENGTH";

        // constant indicating column's Radix.
        String SCALE = "SCALE";

        // constant indicating column's Radix.
        String RADIX = "RADIX";

        /**
         * name of column that has a String value indicating nullablity
         */
        String NULLABLE = "NULLABLE";

        /**
         * name of column containing explanatory notes.
         */
        String REMARKS = "REMARKS";
        String POSITION = "POSITION";
    }


    /**
     * This class contains constants representing column names on ResultSet
     * returned by getProcedures method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */
    interface PROCEDURES {

        // name of the column containing procedure catalog or Virtual database name.
        String PROCEDURE_CAT = "PROCEDURE_CAT";

        // name of the column containing schema or Virtual database version.
        String PROCEDURE_SCHEM = "PROCEDURE_SCHEM";

        // name of the column containing table or group name.
        String PROCEDURE_NAME = "PROCEDURE_NAME";

        // name of the column containing name of the column which is reserved
        //String RESERVED = "RESERVED";

        /**
         * name of column containing explanatory notes.
         */
        String REMARKS = "REMARKS";

        /**
         * name of column indicating kind of the procedure.
         */
        String PROCEDURE_TYPE = "PROCEDURE_TYPE";

        String RESERVED_1 = "RESERVED_1";
        String RESERVED_2 = "RESERVED_2";
        String RESERVED_3 = "RESERVED_3";
    }


    /**
     * This class contains constants representing column names on ResultSet
     * returned by getSchemas method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */
    interface SCHEMAS {

        // name of the column containing procedure catalog or Virtual database name.
        String TABLE_SCHEM = "TABLE_SCHEM";

        // name of the column containing schema or Virtual database version.
        String TABLE_CATALOG = "TABLE_CATALOG";

    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getTables and getTableTypes methods on DatabaseMetaData. These
     * constant values are be used to hardcode the column names used in construction
     * the ResultSet obj.
     */
    interface TABLES {

        // name of the column containing catalog or Virtual database name.
        String TABLE_CAT = "TABLE_CAT";

        // name of the column containing schema or Virtual database version.
        String TABLE_SCHEM = "TABLE_SCHEM";

        // name of the column containing table or group name.
        String TABLE_NAME = "TABLE_NAME";

        // name of the column containing table or group type.
        String TABLE_TYPE = "TABLE_TYPE";

        /**
         * name of column containing explanatory notes.
         */
        String REMARKS = "REMARKS";
        String TYPE_CAT = "TYPE_CAT";
        String TYPE_SCHEM = "TYPE_SCHEM";
        String TYPE_NAME = "TYPE_NAME";
        String SELF_REFERENCING_COL_NAME = "SELF_REFERENCING_COL_NAME";
        String REF_GENERATION = "REF_GENERATION";
        String ISPHYSICAL = "ISPHYSICAL";

    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getTables and getTableTypes methods on DatabaseMetaData. These
     * constant values are be used to hardcode the column names used in construction
     * the ResultSet obj.
     */
    interface TABLE_TYPES {

        // name of the column containing table or group type.
        String TABLE_TYPE = "TABLE_TYPE";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getTypeInfo method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */
    interface TYPE_INFO {

        /**
         * name of column that contains local type name used by the data source.
         */
        String TYPE_NAME = "TYPE_NAME";

        /**
         * name of column that contains SQL type from java.sql.Types for column's data type.
         */
        String DATA_TYPE = "DATA_TYPE";

        // name of the column containing number of digits to right of decimal
        String PRECISION = "PRECISION";

        // name of the column containing prefix used to quote a literal
        String LITERAL_PREFIX = "LITERAL_PREFIX";

        // name of the column containing suffix used to quote a literal
        String LITERAL_SUFFIX = "LITERAL_SUFFIX";

        // name of the column containing params used in creating the type
        String CREATE_PARAMS = "CREATE_PARAMS";

        /**
         * name of column that has a String value indicating nullablity
         */
        String NULLABLE = "NULLABLE";

        /**
         * name of column that has a String value indicating case sensitivity
         */
        String CASE_SENSITIVE = "CASE_SENSITIVE";

        /**
         * name of column that has a String value indicating searchability
         */
        String SEARCHABLE = "SEARCHABLE";

        /**
         * name of column that has a String value indicating searchability
         */
        String UNSIGNED_ATTRIBUTE = "UNSIGNED_ATTRIBUTE";

        /**
         * name of column that contains info if the column is a currency value
         */
        String FIXED_PREC_SCALE = "FIXED_PREC_SCALE";

        /**
         * name of column that contains info whether the column is autoincrementable
         */
        String AUTOINCREMENT = "AUTO_INCREMENT";

        /**
         * name of column that localised version of type name
         */
        String LOCAL_TYPE_NAME = "LOCAL_TYPE_NAME";

        /**
         * name of column that gives the min scale supported
         */
        String MINIMUM_SCALE = "MINIMUM_SCALE";

        /**
         * name of column that gives the max scale supported
         */
        String MAXIMUM_SCALE = "MAXIMUM_SCALE";

        /**
         * name of column that not used will contain nulls
         */
        String SQL_DATA_TYPE = "SQL_DATA_TYPE";

        /**
         * name of column that not used will contain nulls
         */
        String SQL_DATETIME_SUB = "SQL_DATETIME_SUB";

        // constant indicating column's Radix.
        String NUM_PREC_RADIX = "NUM_PREC_RADIX";
    }


    /**
     * This class contains constants representing column names on ResultSet
     * returned by getUDTS method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */
    interface UDTS {

        // name of the column containing table or Groups name in which UDTS are present.
        String TABLE_NAME = "UDTS";

        // name of the column containing catalog or Virtual database name.
        String TYPE_CAT = "TYPE_CAT";

        // name of the column containing schema or Virtual database version.
        String TYPE_SCHEM = "TYPE_SCHEM";

        // name of the column containing name of type name column.
        String TYPE_NAME = "TYPE_NAME";

        // name of the column containing class name column.
        String CLASS_NAME = "CLASS_NAME";

        // name of the column containing name of sql datatype code column
        String DATA_TYPE = "DATA_TYPE";

        // name of the column containing comments column
        String REMARKS = "REMARKS";
        String BASE_TYPE = "BASE_TYPE";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getIndexInfo method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */
    interface INDEX_INFO {

        // name of the column containing tables catalog name on which the index is present
        String TABLE_CAT = "TABLE_CAT";

        // name of the column containing tables schema name on which the index is present
        String TABLE_SCHEM = "TABLE_SCHEM";

        // name of the column containing table or group name.
        String TABLE_NAME = "TABLE_NAME";

        // name of the column containing name of column showing if an index in non-unique
        String NON_UNIQUE = "NON_UNIQUE";

        // name of the column containing name of column containing index_qualifier string
        String INDEX_QUALIFIER = "INDEX_QUALIFIER";

        // name of the column containing name of column containing index names
        String INDEX_NAME = "INDEX_NAME";

        // name of the column containing name of column containing index types
        String TYPE = "TYPE";

        // name of the column containing name of the column containing column position.
        String ORDINAL_POSITION = "ORDINAL_POSITION";

        // name of the column containing name of the column containing column names.
        String COLUMN_NAME = "COLUMN_NAME";

        // name of the column containing name of column containing info if the index is asc or desc.
        String ASC_OR_DESC = "ASC_OR_DESC";

        // name of the column containing name of the column containing number of unique values in index.
        String CARDINALITY = "CARDINALITY";

        // name of the column containing name of the column giving number od pages used for the current index.
        String PAGES = "PAGES";

        // name of the column containing name of the column giving filter condition.
        String FILTER_CONDITION = "FILTER_CONDITION";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getBestRowIdentifier method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */
    interface BEST_ROW {

        // name of the column containing SCOPE of the identifier
        String SCOPE = "SCOPE";

        // name of the column containing column name
        String COLUMN_NAME = "COLUMN_NAME";

        // name of the column containing data type code
        String DATA_TYPE = "DATA_TYPE";

        // name of the column containing data type name
        String TYPE_NAME = "TYPE_NAME";

        // name of the column containing size of the column
        String COLUMN_SIZE = "COLUMN_SIZE";

        // name of the column containing buffer length
        String BUFFER_LENGTH = "BUFFER_LENGTH";

        // name of the column containing decimal digits/ scale
        String DECIMAL_DIGITS = "DECIMAL_DIGITS";

        // name of the column containing name of the column containing column position.
        String ORDINAL_POSITION = "ORDINAL_POSITION";

        // name of the column containing pseudo column
        String PSEUDO_COLUMN = "PSEUDO_COLUMN";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getSuperTables method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */
    interface SUPER_TABLES {
        // name of the column containing catalog or Virtual database name.
        String TABLE_CAT = "TABLE_CAT";
        // name of the column containing schema or Virtual database version.
        String TABLE_SCHEM = "TABLE_SCHEM";
        // name of the column containing table or Groups name .
        String TABLE_NAME = "TABLE_NAME";
        // name of the column containing super table.
        String SUPERTABLE_NAME = "SUPERTABLE_NAME";
    }

    /**
     * This class contains constants representing column names on ResultSet
     * returned by getSuperTypes method on DatabaseMetaData. These constant values
     * are be used to hardcode the column names used in constructing the ResultSet obj.
     */

    interface SUPER_TYPES {

        // name of the column containing catalog or Virtual database name.
        String TYPE_CAT = "TYPE_CAT";
        // name of the column containing schema or Virtual database version.
        String TYPE_SCHEM = "TYPE_SCHEM";
        // name of the column containing name of type name column.
        String TYPE_NAME = "TYPE_NAME";
        //  name of the column containing super catalog or Virtual database name.
        String SUPERTYPE_CAT = "SUPERTYPE_CAT";
        // name of the column containing super schema or Virtual database version.
        String SUPERTYPE_SCHEM = "SUPERTYPE_SCHEM";
        // name of the column containing name of super type name column.
        String SUPERTYPE_NAME = "SUPERTYPE_NAME";

    }

}
