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

import com.kubling.client.metadata.ResultsMetadataConstants;
import com.kubling.core.CoreConstants;
import com.kubling.core.types.DataTypeManager;
import com.kubling.core.types.JDBCSQLTypeInfo;
import com.kubling.core.util.PropertiesUtils;
import com.kubling.core.util.SqlUtil;
import com.kubling.core.util.StringUtil;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;


public class DatabaseMetaDataImpl extends WrapperImpl implements DatabaseMetaData {

    public static final String REPORT_AS_VIEWS = "reportAsViews";

    public static final String NULL_SORT = "nullsAreSorted";

    enum NullSort {
        High, Low, AtStart, AtEnd
    }

    private static final String IS_NULLABLE = "CASE NullType WHEN 'Nullable' THEN 'YES' WHEN 'No Nulls' THEN 'NO' ELSE '' END AS IS_NULLABLE";

    private static final String DATA_TYPES = "DataTypes";

    private static final Logger logger = Logger.getLogger("com.kubling.jdbc");

    /**
     * CONSTANTS
     */
    private static final String PERCENT = "%";
    // constant value indicating that there is not limit
    private final static int NO_LIMIT = 0;
    // constant value giving preferred name for a schema
    private final static String SCHEMA_TERM = "Schema";
    // constant value giving a string used to escape search strings
    private final static String ESCAPE_SEARCH_STRING = "\\";
    // constant value giving an identifier quoting char
    //private final static String SINGLE_QUOTE = "\'";
    // constant value giving an identifier quoting string
    private final static String DOUBLE_QUOTE = "\"";
    // constant value giving extra name characters used in Identifiers
    private final static String EXTRA_CHARS = ".@";
    // constant value giving the keywords not in SQL-92
    final static String KEY_WORDS = "OPTION, BIGDECIMAL" +
            ", BIGDECIMAL, BIGINTEGER, BREAK, BYTE, CRITERIA, ERROR, LIMIT, LONG, LOOP, MAKEDEP, MAKENOTDEP" +
            ", NOCACHE, STRING, VIRTUAL, WHILE";
    // constant value giving preferred name for a procedure
    private final static String PROCEDURE_TERM = "StoredProcedure";
    // constant value giving the names of numeric functions supported
    final static String NUMERIC_FUNCTIONS =
            "ABS, ACOS, ASIN, ATAN, ATAN2, BITAND, BITNOT, BITOR, BITXOR, CEILING"
                    + ", COS, COT, DEGREES, EXP, FLOOR, FORMATBIGDECIMAL, FORMATBIGINTEGER"
                    + ", FORMATDOUBLE, FORMATFLOAT, FORMATINTEGER, FORMATLONG, LOG, LOG10"
                    + ", MOD, PARSEBIGDECIMAL, PARSEBIGINTEGER, PARSEDOUBLE, PARSEFLOAT"
                    + ", PARSEINTEGER, PARSELONG, PI, POWER, RADIANS, RAND, ROUND, SIGN, SIN, SQRT, TAN";
    // constant value giving the names of string functions supported
    final static String STRING_FUNCTIONS =
            "ASCII, CHR, CHAR, CONCAT, CONCAT2, INITCAP, INSERT, LCASE, LEFT, LENGTH, LOCATE, LOWER, LPAD, LTRIM, " +
                    "REPEAT, REPLACE, RIGHT, RPAD, RTRIM, SUBSTRING, TRANSLATE, UCASE, UPPER";
    // constant value giving the names of date/time functions supported
    final static String DATE_FUNCTIONS =
            "CURDATE, CURTIME, NOW, DAYNAME, DAYOFMONTH, DAYOFWEEK, DAYOFYEAR, FORMATDATE, " +
                    "FORMATTIME, FORMATTIMESTAMP, FROM_UNIXTIME, HOUR, MINUTE, MONTH, MONTHNAME, PARSEDATE, PARSETIME, " +
                    "PARSETIMESTAMP, QUARTER, SECOND, TIMESTAMPADD, TIMESTAMPDIFF, WEEK, YEAR";
    // constant value giving the names of system functions supported
    final static String SYSTEM_FUNCTIONS =
            "CAST, COALESCE, CONVERT, DECODESTRING, DECODEINTEGER, IFNULL, NULLIF, NVL, LOOKUP, UUID, UNESCAPE, ARRAY_GET, ARRAY_LENGTH";
    // constant value giving max length of a catalog name
    private final static int MAX_CATALOG_NAME_LENGTH = 255;
    // constant value giving max length of a procedure name
    private final static int MAX_PROCEDURE_NAME_LENGTH = 255;
    // constant value giving max length of a table name
    private final static int MAX_TABLE_NAME_LENGTH = 255;
    // constant value giving max length of a column name
    private final static int MAX_COLUMN_NAME_LENGTH = 255;
    // constant value giving max length of a username
    private final static int MAX_USER_NAME_LENGTH = 255;
    // constant value giving min value of a columns scale
    //private final static short MIN_SCALE = 0;
    // constant value giving max value of a columns scale
    //private final static short MAX_SCALE = 256;

    private final static String LIKE_ESCAPE = " LIKE ? ESCAPE '" + ESCAPE_SEARCH_STRING + "' ";

    final private static class RUNTIME_MODEL {
        public final static String VIRTUAL_MODEL_NAME = CoreConstants.SYSTEM_MODEL;
    }

    private static final String NULLABILITY_MAPPING =
            "No Nulls, " + DatabaseMetaData.columnNoNulls +
                    ", Nullable, " + DatabaseMetaData.columnNullable +
                    ", Unknown, " + DatabaseMetaData.columnNullableUnknown;

    private static final String TYPE_NULLABILITY_MAPPING =
            "No Nulls, " + DatabaseMetaData.typeNoNulls +
                    ", Nullable, " + DatabaseMetaData.typeNullable +
                    ", Unknown, " + DatabaseMetaData.typeNullableUnknown;

    private static final String PROC_COLUMN_NULLABILITY_MAPPING =
            "No Nulls, " + DatabaseMetaData.procedureNoNulls +
                    ", Nullable, " + DatabaseMetaData.procedureNullable +
                    ", Unknown, " + DatabaseMetaData.procedureNullableUnknown;

    private static final String PARAM_DIRECTION_MAPPING =
            "In," + DatabaseMetaData.procedureColumnIn +
                    ", Out," + DatabaseMetaData.procedureColumnOut +
                    ", InOut," + DatabaseMetaData.procedureColumnInOut +
                    ", ReturnValue," + DatabaseMetaData.procedureColumnReturn +
                    ", ResultSet," + DatabaseMetaData.procedureColumnResult;

    // Queries
    private final static String QUERY_REFERENCE_KEYS =
            "SELECT PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_CAT, FKTABLE_SCHEM" +
                    ", FKTABLE_NAME, FKCOLUMN_NAME, KEY_SEQ, UPDATE_RULE, DELETE_RULE, FK_NAME, PK_NAME, DEFERRABILITY FROM " +
                    RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".ReferenceKeyColumns";

    private final static String QUERY_CROSS_REFERENCES = QUERY_REFERENCE_KEYS +
            " WHERE UCASE(PKTABLE_CAT)" + LIKE_ESCAPE + "AND UCASE(FKTABLE_CAT)" + LIKE_ESCAPE +
            " AND UCASE(PKTABLE_SCHEM)" + LIKE_ESCAPE + "AND UCASE(FKTABLE_SCHEM)" + LIKE_ESCAPE +
            " AND UCASE(PKTABLE_NAME)" + LIKE_ESCAPE + "AND UCASE(FKTABLE_NAME)" + LIKE_ESCAPE +
            "ORDER BY FKTABLE_NAME, KEY_SEQ";

    private final static String QUERY_EXPORTED_KEYS = QUERY_REFERENCE_KEYS +
            " WHERE UCASE(PKTABLE_CAT)" + LIKE_ESCAPE +
            " AND UCASE(PKTABLE_SCHEM)" + LIKE_ESCAPE +
            " AND UCASE(PKTABLE_NAME)" + LIKE_ESCAPE + "ORDER BY FKTABLE_NAME, KEY_SEQ";

    private final static String QUERY_IMPORTED_KEYS = QUERY_REFERENCE_KEYS +
            " WHERE UCASE(FKTABLE_CAT)" + LIKE_ESCAPE +
            " AND UCASE(FKTABLE_SCHEM)" + LIKE_ESCAPE +
            " AND UCASE(FKTABLE_NAME)" + LIKE_ESCAPE + "ORDER BY PKTABLE_NAME, KEY_SEQ";

    /* Note that we're retrieving length as DATA_TYPE.  Once retrieved when then correct this.
     * This allows us to reuse the ResultSetMetadata.
     */
    private final static String QUERY_COLUMNS_OLD = "SELECT VDBName AS TABLE_CAT" +
            ", SchemaName AS TABLE_SCHEM, TableName AS TABLE_NAME, Name AS COLUMN_NAME" +
            ", Length AS DATA_TYPE" +
            ", DataType AS TYPE_NAME" +
            ", e.Precision AS COLUMN_SIZE" +
            ", NULL AS BUFFER_LENGTH, Scale AS DECIMAL_DIGITS, Radix AS NUM_PREC_RADIX" +
            ", convert(decodeString(NullType, '" + NULLABILITY_MAPPING + "', ','), integer) AS NULLABLE" +
            ", Description AS REMARKS, DefaultValue AS COLUMN_DEF, NULL AS SQL_DATA_TYPE, NULL AS SQL_DATETIME_SUB" +
            ", CharOctetLength AS CHAR_OCTET_LENGTH, Position AS ORDINAL_POSITION" +
            ", " + IS_NULLABLE +
            ", NULL AS SCOPE_CATALOG, NULL AS SCOPE_SCHEMA, NULL AS SCOPE_TABLE, NULL AS SOURCE_DATA_TYPE, CASE WHEN e.IsAutoIncremented = 'true' THEN 'YES' ELSE 'NO' END AS IS_AUTOINCREMENT" +
            " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME +
            ".Columns e" +
            " WHERE UCASE(SchemaName)" + LIKE_ESCAPE +
            "AND UCASE(TableName)" + LIKE_ESCAPE +
            "AND UCASE(Name)" + LIKE_ESCAPE +
            "AND UCASE(VDBName)" + LIKE_ESCAPE +
            " ORDER BY TABLE_NAME, ORDINAL_POSITION";

    private final static String QUERY_COLUMNS = "SELECT VDBName AS TABLE_CAT" +
            ", SchemaName AS TABLE_SCHEM, TableName AS TABLE_NAME, Name AS COLUMN_NAME" +
            ", TypeCode AS DATA_TYPE" +
            ", TypeName AS TYPE_NAME" +
            ", ColumnSize AS COLUMN_SIZE" +
            ", NULL AS BUFFER_LENGTH, Scale AS DECIMAL_DIGITS, Radix AS NUM_PREC_RADIX" +
            ", convert(decodeString(NullType, '" + NULLABILITY_MAPPING + "', ','), integer) AS NULLABLE" +
            ", Description AS REMARKS, DefaultValue AS COLUMN_DEF, NULL AS SQL_DATA_TYPE, NULL AS SQL_DATETIME_SUB" +
            ", CharOctetLength AS CHAR_OCTET_LENGTH, Position AS ORDINAL_POSITION" +
            ", " + IS_NULLABLE +
            ", NULL AS SCOPE_CATALOG, NULL AS SCOPE_SCHEMA, NULL AS SCOPE_TABLE, NULL AS SOURCE_DATA_TYPE, CASE WHEN e.IsAutoIncremented = 'true' THEN 'YES' ELSE 'NO' END AS IS_AUTOINCREMENT, null AS IS_GENERATEDCOLUMN" +
            " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME +
            ".Columns e" +
            " WHERE UCASE(SchemaName)" + LIKE_ESCAPE +
            "AND UCASE(TableName)" + LIKE_ESCAPE +
            "AND UCASE(Name)" + LIKE_ESCAPE +
            "AND UCASE(VDBName)" + LIKE_ESCAPE +
            " ORDER BY TABLE_NAME, ORDINAL_POSITION";

    private static final String QUERY_INDEX_INFO =
            "SELECT VDBName AS TABLE_CAT, SchemaName AS TABLE_SCHEM, TableName AS TABLE_NAME" +
                    ", case when KeyType = 'Index' then TRUE else FALSE end AS NON_UNIQUE, NULL AS INDEX_QUALIFIER, KeyName AS INDEX_NAME" +
                    ", 3 AS TYPE, convert(Position, short) AS ORDINAL_POSITION, k.Name AS COLUMN_NAME" +
                    ", NULL AS ASC_OR_DESC, 0 AS CARDINALITY, 1 AS PAGES, NULL AS FILTER_CONDITION" +
                    " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".KeyColumns k" +
                    " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
                    " AND UCASE(SchemaName)" + LIKE_ESCAPE +
                    " AND UCASE(TableName)" + LIKE_ESCAPE +
                    " AND KeyType IN ('Unique', ?)";

    private static final String QUERY_INDEX_INFO_CARDINALITY =
            "SELECT VDBName AS TABLE_CAT, SchemaName AS TABLE_SCHEM, Name AS TABLE_NAME" +
                    ", FALSE AS NON_UNIQUE, NULL AS INDEX_QUALIFIER, null AS INDEX_NAME" +
                    ", 0 AS TYPE, cast(0 as short) AS ORDINAL_POSITION, null AS COLUMN_NAME" +
                    ", NULL AS ASC_OR_DESC, CARDINALITY, 1 AS PAGES, NULL AS FILTER_CONDITION" +
                    " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".Tables t" +
                    " WHERE Cardinality > -1" +
                    " AND UCASE(VDBName)" + LIKE_ESCAPE +
                    " AND UCASE(SchemaName)" + LIKE_ESCAPE +
                    " AND UCASE(Name)" + LIKE_ESCAPE;

    private static final String QUERY_PRIMARY_KEYS =
            "SELECT VDBName as TABLE_CAT, SchemaName AS TABLE_SCHEM, TableName AS TABLE_NAME" +
                    ", k.Name AS COLUMN_NAME, convert(Position, short) AS KEY_SEQ, KeyName AS PK_NAME" +
                    " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".KeyColumns k" +
                    " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
                    " AND UCASE(SchemaName)" + LIKE_ESCAPE +
                    " AND UCASE(TableName)" + LIKE_ESCAPE +
                    " AND KeyType LIKE 'Primary'" +
                    " ORDER BY COLUMN_NAME, KEY_SEQ";

    private final static String QUERY_PROCEDURES =
            "SELECT VDBName AS PROCEDURE_CAT, SchemaName AS PROCEDURE_SCHEM" +
                    ", p.Name AS PROCEDURE_NAME, convert(null, string) AS RESERVED_1" +
                    ", convert(null, string) AS RESERVED_2, convert(null, string) AS RESERVED_3, p.Description AS REMARKS" +
                    ", convert(decodeString(p.ReturnsResults, 'true, " + DatabaseMetaData.procedureReturnsResult +
                    ", false, " + DatabaseMetaData.procedureNoResult + "'), short) AS PROCEDURE_TYPE, p.Name AS SPECIFIC_NAME FROM " +
                    RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".Procedures as p" +
                    " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
                    " AND UCASE(SchemaName)" + LIKE_ESCAPE +
                    " AND UCASE(p.Name)" + LIKE_ESCAPE +
                    " ORDER BY PROCEDURE_SCHEM, PROCEDURE_NAME";

    private final static String QUERY_PROCEDURE_COLUMNS =
            "SELECT VDBName PROCEDURE_CAT, SchemaName AS PROCEDURE_SCHEM" +
                    ", ProcedureName AS PROCEDURE_NAME, p.Name AS COLUMN_NAME" +
                    ", convert(decodeString(TYPE, '" + PARAM_DIRECTION_MAPPING + "', ','), short) AS COLUMN_TYPE" +
                    ", TypeCode AS DATA_TYPE" +
                    ", TypeName AS TYPE_NAME, ColumnSize AS \"PRECISION\", TypeLength  AS LENGTH, convert(case when scale > 32767 then 32767 else Scale end, short) AS SCALE" +
                    ", Radix AS RADIX, convert(decodeString(NullType, '" +
                    PROC_COLUMN_NULLABILITY_MAPPING + "', ','), integer) AS NULLABLE" +
                    ", p.Description AS REMARKS, %s AS COLUMN_DEF" +
                    ", NULL AS SQL_DATA_TYPE, NULL AS SQL_DATETIME_SUB, NULL AS CHAR_OCTET_LENGTH, p.Position AS ORDINAL_POSITION" +
                    ", " + IS_NULLABLE + ", p.ProcedureName as SPECIFIC_NAME FROM " +
                    RUNTIME_MODEL.VIRTUAL_MODEL_NAME +
                    ".ProcedureParams as p" +
                    " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
                    " AND UCASE(SchemaName)" + LIKE_ESCAPE +
                    " AND UCASE(ProcedureName)" + LIKE_ESCAPE +
                    " AND UCASE(p.Name)" + LIKE_ESCAPE +
                    " ORDER BY PROCEDURE_SCHEM, PROCEDURE_NAME, case TYPE when 'ReturnValue' then 0 when 'ResultSet' then 2 else 1 end, POSITION";

    private final static String QUERY_PROCEDURE_COLUMNS_OLD =
            "SELECT VDBName PROCEDURE_CAT, SchemaName AS PROCEDURE_SCHEM" +
                    ", ProcedureName AS PROCEDURE_NAME, p.Name AS COLUMN_NAME" +
                    ", convert(decodeString(TYPE, '" + PARAM_DIRECTION_MAPPING + "', ','), short) AS COLUMN_TYPE" +
                    ", 1 AS DATA_TYPE" +
                    ", DataType AS TYPE_NAME, p.Precision AS \"PRECISION\", TypeLength  AS LENGTH, convert(case when scale > 32767 then 32767 else Scale end, short) AS SCALE" +
                    ", Radix AS RADIX, convert(decodeString(NullType, '" +
                    PROC_COLUMN_NULLABILITY_MAPPING + "', ','), integer) AS NULLABLE" +
                    ", p.Description AS REMARKS, NULL AS COLUMN_DEF" +
                    ", NULL AS SQL_DATA_TYPE, NULL AS SQL_DATETIME_SUB, NULL AS CHAR_OCTET_LENGTH, p.Position AS ORDINAL_POSITION" +
                    ", " + IS_NULLABLE + ", p.ProcedureName as SPECIFIC_NAME FROM " +
                    RUNTIME_MODEL.VIRTUAL_MODEL_NAME +
                    ".ProcedureParams as p" +
                    " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
                    " AND UCASE(SchemaName)" + LIKE_ESCAPE +
                    " AND UCASE(ProcedureName)" + LIKE_ESCAPE +
                    " AND UCASE(p.Name)" + LIKE_ESCAPE +
                    " ORDER BY PROCEDURE_SCHEM, PROCEDURE_NAME, case TYPE when 'ReturnValue' then 0 when 'ResultSet' then 2 else 1 end, POSITION";

    private static final String QUERY_SCHEMAS =
            "SELECT Name AS TABLE_SCHEM, VDBName AS TABLE_CATALOG" +
                    " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".Schemas" +
                    " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
                    " AND UCASE(Name)" + LIKE_ESCAPE +
                    " ORDER BY TABLE_SCHEM";

    private static final String QUERY_FUNCTIONS = "SELECT VDBName AS Function_CAT, SchemaName AS FUNCTION_SCHEM, "
            + "Name AS FUNCTION_NAME, Description as REMARKS, 1 as FUNCTION_TYPE, UID AS SPECIFIC_NAME" +
            " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".Functions" +
            " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
            " AND UCASE(SchemaName)" + LIKE_ESCAPE +
            " AND UCASE(Name)" + LIKE_ESCAPE +
            " ORDER BY FUNCTION_CAT, FUNCTION_SCHEM, FUNCTION_NAME, SPECIFIC_NAME";

    private static final String QUERY_FUNCTION_COLUMNS = "SELECT VDBName AS Function_CAT, SchemaName AS FUNCTION_SCHEM, " +
            "FunctionName AS FUNCTION_NAME, Name as COLUMN_NAME, CASE WHEN Type = 'ReturnValue' Then 4 WHEN Type = 'In' Then 1 ELSE 0 END AS COLUMN_TYPE" +
            ", TypeCode AS DATA_TYPE" +
            ", TypeName AS TYPE_NAME, ColumnSize AS \"PRECISION\", TypeLength  AS LENGTH, convert(case when scale > 32767 then 32767 else Scale end, short) AS SCALE" +
            ", Radix AS RADIX, convert(decodeString(NullType, '" +
            PROC_COLUMN_NULLABILITY_MAPPING + "', ','), integer) AS NULLABLE" +
            ", Description AS REMARKS, NULL AS CHAR_OCTET_LENGTH, Position AS ORDINAL_POSITION," +
            IS_NULLABLE + ", FunctionUID as SPECIFIC_NAME" +
            " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".FunctionParams" +
            " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
            " AND UCASE(SchemaName)" + LIKE_ESCAPE +
            " AND UCASE(FunctionName)" + LIKE_ESCAPE +
            " AND UCASE(Name)" + LIKE_ESCAPE +
            " ORDER BY FUNCTION_CAT, FUNCTION_SCHEM, FUNCTION_NAME, SPECIFIC_NAME, ORDINAL_POSITION";

    private static final String QUERY_FUNCTION_COLUMNS_OLD = "SELECT VDBName AS Function_CAT, SchemaName AS FUNCTION_SCHEM, " +
            "FunctionName AS FUNCTION_NAME, Name as COLUMN_NAME, CASE WHEN Type = 'ReturnValue' Then 4 WHEN Type = 'In' Then 1 ELSE 0 END AS COLUMN_TYPE" +
            ", 1 AS DATA_TYPE" +
            ", DataType AS TYPE_NAME, \"Precision\" AS \"PRECISION\", TypeLength  AS LENGTH, convert(case when scale > 32767 then 32767 else Scale end, short) AS SCALE" +
            ", Radix AS RADIX, convert(decodeString(NullType, '" +
            PROC_COLUMN_NULLABILITY_MAPPING + "', ','), integer) AS NULLABLE" +
            ", Description AS REMARKS, NULL AS CHAR_OCTET_LENGTH, Position AS ORDINAL_POSITION," +
            IS_NULLABLE + ", FunctionUID as SPECIFIC_NAME" +
            " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".FunctionParams" +
            " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
            " AND UCASE(SchemaName)" + LIKE_ESCAPE +
            " AND UCASE(FunctionName)" + LIKE_ESCAPE +
            " AND UCASE(Name)" + LIKE_ESCAPE +
            " ORDER BY FUNCTION_CAT, FUNCTION_SCHEM, FUNCTION_NAME, SPECIFIC_NAME, ORDINAL_POSITION";

    private static final String QUERY_TYPEINFO = "SELECT name as TYPE_NAME, typecode as DATA_TYPE, \"PRECISION\", LITERAL_PREFIX, LITERAL_SUFFIX, null as CREATE_PARAMS, "
            + "convert(decodeString(NullType, '" + TYPE_NULLABILITY_MAPPING + "', ','), short) AS NULLABLE, "
            + "IsCaseSensitive as CASE_SENSITIVE, "
            + "cast(case SearchType"
            + " when 'Like Only' then " + DatabaseMetaData.typePredChar
            + " when 'All Except Like' then " + DatabaseMetaData.typePredBasic
            + " when 'Searchable' then " + DatabaseMetaData.typeSearchable
            + " else " + DatabaseMetaData.typePredNone
            + " end as short) as SEARCHABLE, "
            + "not(IsSigned) as UNSIGNED_ATTRIBUTE, false as FIXED_PREC_SCALE, "
            + "IsAutoIncremented as AUTO_INCREMENT, null as LOCAL_TYPE_NAME, cast(0 as short) as MINIMUM_SCALE, "
            + "cast(32767 as short) as MAXIMUM_SCALE, cast(null as integer) AS SQL_DATA_TYPE, cast(null as integer) AS SQL_DATETIME_SUB, radix as NUM_PREC_RADIX "
            + "from SYS.datatypes where type in ('Domain', 'Basic')";

    private final String TABLE_TYPE;

    private final String QUERY_TABLES;

    /**
     * ATTRIBUTES
     */

    // driver's connection object used in constructing this object.
    private final ConnectionImpl driverConnection;

    private NullSort nullSort;

    /**
     * <p>Constructor which initializes with the connection object on which metadata
     * is sought
     *
     * @param connection driver's connection object.
     */
    DatabaseMetaDataImpl(ConnectionImpl connection) {
        this.driverConnection = connection;
        if (PropertiesUtils.getBooleanProperty(connection.getConnectionProps(), REPORT_AS_VIEWS, true)) {
            TABLE_TYPE = "CASE WHEN IsSystem = 'true' and UCASE(Type) = 'TABLE' THEN 'SYSTEM TABLE' WHEN IsPhysical <> 'true' AND UCASE(Type) = 'TABLE' THEN 'VIEW' ELSE UCASE(Type) END";
        } else {
            TABLE_TYPE = "CASE WHEN IsSystem = 'true' and UCASE(Type) = 'TABLE' THEN 'SYSTEM TABLE' ELSE UCASE(Type) END";
        }

        String nullSortProp = connection.getConnectionProps().getProperty(NULL_SORT);
        if (nullSortProp != null) {
            nullSort = StringUtil.caseInsensitiveValueOf(NullSort.class, nullSortProp);
        }

        QUERY_TABLES = "SELECT VDBName AS TABLE_CAT, SchemaName AS TABLE_SCHEM, Name AS TABLE_NAME" +
                ", " + TABLE_TYPE + " AS TABLE_TYPE, Description AS REMARKS, NULL AS TYPE_CAT, NULL AS TYPE_SCHEM" +
                ", NULL AS TYPE_NAME, NULL AS SELF_REFERENCING_COL_NAME, NULL AS REF_GENERATION, IsPhysical AS ISPHYSICAL" +
                " FROM " + RUNTIME_MODEL.VIRTUAL_MODEL_NAME + ".Tables g " +
                " WHERE UCASE(VDBName)" + LIKE_ESCAPE +
                " AND UCASE(SchemaName)" + LIKE_ESCAPE +
                " AND UCASE(Name)" + LIKE_ESCAPE;
    }

    @Override
    public boolean allProceduresAreCallable() {
        return true;
    }

    public boolean allTablesAreSelectable() {
        return true;
    }

    /**
     * <p>Checks whether a DDL statement within a transaction forces the transaction
     * to commit.
     *
     * @return if so return true else return false.
     */
    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    /**
     * <p>Checks whether a DDL statement within a transaction is ignored.
     *
     * @return if so return true, else false
     */
    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
    }

    @Override
    public boolean deletesAreDetected(int type) {
        return false;
    }

    /**
     * <p>Did getMaxRowSize() include LONGVARCHAR and LONGVARBINARY
     * blobs?
     *
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public boolean doesMaxRowSizeIncludeBlobs() {
        return false;
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
            throws SQLException {

        // here it always returns an empty result set, when this functionality
        // is being changed make sure that we check the catalog & schema here
        // to filter.

        // list containing records/rows in the ResultSet
        List records = new ArrayList(0);

        /* **********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         * **********************************************************************/
        Map[] metadataList = new Map[8];

        // HardCoding metadata details for SCOPE column
        metadataList[0] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.SCOPE,
                DataTypeManager.DefaultDataTypes.SHORT, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for COLUMN_NAME column
        metadataList[1] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.COLUMN_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for DATA_TYPE column
        metadataList[2] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.DATA_TYPE,
                DataTypeManager.DefaultDataTypes.SHORT, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for TYPE_NAME column
        metadataList[3] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.TYPE_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for COLUMN_SIZE column
        metadataList[4] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.COLUMN_SIZE,
                DataTypeManager.DefaultDataTypes.INTEGER, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for BUFFER_LENGTH column
        metadataList[5] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.BUFFER_LENGTH,
                DataTypeManager.DefaultDataTypes.INTEGER, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // HardCoding metadata details for DECIMAL_DIGITS column
        metadataList[6] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.DECIMAL_DIGITS,
                DataTypeManager.DefaultDataTypes.SHORT, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for PSEUDO_COLUMN column
        metadataList[7] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.BEST_ROW.PSEUDO_COLUMN,
                DataTypeManager.DefaultDataTypes.SHORT, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // logging
        String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.Best_row_sucess", table);
        logger.fine(logMsg);

        // construct results object from column values and their metadata
        return dummyStatement().createResultSet(records, metadataList);
    }

    public ResultSet getCatalogs() throws SQLException {
        // list containing records/rows in the ResultSet
        List<List<String>> records = new ArrayList<>(1);
        records.add(Arrays.asList(this.driverConnection.getCatalog()));

        /* **********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         * **********************************************************************/
        Map[] metadataList = new Map[1];

        // HardCoding metadata details for TABLE_CAT column
        metadataList[0] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.CATALOGS.TABLE_CAT,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // logging
        String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.Catalog_success");
        logger.fine(logMsg);

        // construct results object from column values and their metadata
        return dummyStatement().createResultSet(records, metadataList);
    }

    /**
     * <p>Gets the String object used to separate a catalog name and a table name
     *
     * @return String delimiter
     */
    public String getCatalogSeparator() {
        return ".";
    }

    public String getCatalogTerm() {
        return "VirtualDatabase";
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnName) throws SQLException {

        List records = new ArrayList(0);
        /* **********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         * **********************************************************************/
        Map[] metadataList = new Map[8];

        metadataList[0] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.TABLE_CAT,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);
        metadataList[1] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.TABLE_SCHEM,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);
        metadataList[2] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.TABLE_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);
        metadataList[3] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.COLUMN_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);
        metadataList[4] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.GRANTOR,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);
        metadataList[5] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.GRANTEE,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);
        metadataList[6] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.PRIVILEGE,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);
        metadataList[7] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.IS_GRANTABLE,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        return dummyStatement().createResultSet(records, metadataList);

    }

    @Override
    public ResultSet getColumns(String catalog, String schema, String tableNamePattern, String columnNamePattern) throws SQLException {

        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schema == null) {
            schema = PERCENT;
        }
        // Get columns in all the tables if tableNamePattern is null
        if (tableNamePattern == null) {
            tableNamePattern = PERCENT;
        }
        // Get all columns if columnNamePattern is null
        if (columnNamePattern == null) {
            columnNamePattern = PERCENT;
        }

        // list which represent records containing column info
        List records = new ArrayList();
        ResultSetMetaData rmetadata;
        ResultSetImpl results;
        PreparedStatement prepareQuery = null;

        boolean newMetadata = driverConnection.getServerConnection().getServerVersion().compareTo("09.03") >= 0;

        try {
            prepareQuery = driverConnection.prepareStatement(newMetadata ? QUERY_COLUMNS : QUERY_COLUMNS_OLD);
            prepareQuery.setObject(1, schema.toUpperCase());
            prepareQuery.setObject(2, tableNamePattern.toUpperCase());
            prepareQuery.setObject(3, columnNamePattern.toUpperCase());
            prepareQuery.setObject(4, catalog.toUpperCase());

            // make a query against runtimemetadata and get results
            results = (ResultSetImpl) prepareQuery.executeQuery();

            // build the list of records of column description
            while (results.next()) {
                // list represents a record on the Results object.
                List currentRow = new ArrayList(JDBCColumnPositions.COLUMNS.MAX_COLUMNS);

                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.
                for (int i = 0; i < results.getMetaData().getColumnCount(); i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }
                if (!newMetadata) {
                    String typeName = (String) currentRow.get(JDBCColumnPositions.COLUMNS.TYPE_NAME - 1);
                    Integer length = (Integer) currentRow.get(JDBCColumnPositions.COLUMNS.DATA_TYPE - 1);
                    Integer precision = (Integer) currentRow.get(JDBCColumnPositions.COLUMNS.COLUMN_SIZE - 1);
                    if (typeName != null) {
                        currentRow.set(JDBCColumnPositions.COLUMNS.DATA_TYPE - 1, JDBCSQLTypeInfo.getSQLType(typeName));
                        if (!Number.class.isAssignableFrom(DataTypeManager.getDataTypeClass(typeName))) {
                            if (length != null && length <= 0) {
                                currentRow.set(JDBCColumnPositions.COLUMNS.COLUMN_SIZE - 1, JDBCSQLTypeInfo.getDefaultPrecision(typeName));
                            } else {
                                currentRow.set(JDBCColumnPositions.COLUMNS.COLUMN_SIZE - 1, length);
                            }
                        } else if (precision != null && precision <= 0) {
                            currentRow.set(JDBCColumnPositions.COLUMNS.COLUMN_SIZE - 1, JDBCSQLTypeInfo.getDefaultPrecision(typeName));
                        }
                    } else {
                        currentRow.set(JDBCColumnPositions.COLUMNS.DATA_TYPE - 1, null);
                        currentRow.set(JDBCColumnPositions.COLUMNS.COLUMN_SIZE - 1, null);
                    }
                }
                records.add(currentRow);
            }// end of while

            // get the metadata for the results
            rmetadata = results.getMetaData();

            // logging
            String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.getCols_success", columnNamePattern, tableNamePattern);
            logger.fine(logMsg);

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            // logging
            String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.getCols_error", columnNamePattern, tableNamePattern, e.getMessage());
            throw KublingSQLException.create(e, logMsg);
        } finally {
            if (prepareQuery != null) {
                prepareQuery.close();
            }
        }

    }

    @Override
    public ResultSet getCrossReference(String primaryCatalog, String primarySchema, String primaryTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {

        if (primaryCatalog == null) {
            primaryCatalog = PERCENT;
        }

        if (foreignCatalog == null) {
            foreignCatalog = PERCENT;
        }

        if (primarySchema == null) {
            primarySchema = PERCENT;
        }

        if (foreignSchema == null) {
            foreignSchema = PERCENT;
        }

        if (primaryTable == null) {
            primaryTable = PERCENT;
        }

        if (foreignTable == null) {
            foreignTable = PERCENT;
        }

        ResultSet results;
        try (PreparedStatement prepareQuery = driverConnection.prepareStatement(QUERY_CROSS_REFERENCES)) {
            prepareQuery.setObject(1, primaryCatalog.toUpperCase());
            prepareQuery.setObject(2, foreignCatalog.toUpperCase());
            prepareQuery.setObject(3, primarySchema.toUpperCase());
            prepareQuery.setObject(4, foreignSchema.toUpperCase());
            prepareQuery.setObject(5, primaryTable.toUpperCase());
            prepareQuery.setObject(6, foreignTable.toUpperCase());

            // make a query against runtimemetadata and get results
            results = prepareQuery.executeQuery();

            ResultSet resultSet = getReferenceKeys(results);

            // logging
            String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.getCrossRef_success", primaryTable, foreignTable);
            logger.fine(logMsg);
            return resultSet;
        } catch (Exception e) {
            // logging
            String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.getCrossRef_error",
                    primaryTable, foreignTable, e.getMessage());
            throw KublingSQLException.create(e, logMsg);
        }
    }

    /**
     * Retrieves the minor version number of the underlying database.
     *
     * @return intValue of database's minor version
     * @throws SQLException if a database access error occurs.
     */
    public int getDatabaseMinorVersion() throws SQLException {
        return Integer.parseInt(driverConnection.getServerConnection().getServerVersion().split("[.]")[1]);
    }

    /**
     * Retrieves the major version number of the underlying database.
     *
     * @return intValue of database's minor version
     * @throws SQLException if a database access error occurs.
     */
    public int getDatabaseMajorVersion() throws SQLException {
        return Integer.parseInt(driverConnection.getServerConnection().getServerVersion().split("[.]")[0]);
    }

    /**
     * Retrieves the major JDBC version number for this driver.
     *
     * @return intValue JDBC major version number
     */
    public int getJDBCMajorVersion() {
        return 3;
    }

    /**
     * Retrieves the minor JDBC version number for this driver.
     *
     * @return intValue JDBC major version number
     */
    public int getJDBCMinorVersion() {
        return 0;
    }

    /**
     * <p>Gets the product name for this database
     *
     * @return String representing the product name
     */
    public String getDatabaseProductName() {
        return this.driverConnection.getDatabaseName();
    }

    public String getDatabaseProductVersion() {
        return KublingDriver.getInstance().getMajorVersion() + "." + KublingDriver.getInstance().getMinorVersion();
    }

    public int getDefaultTransactionIsolation() {
        return ConnectionImpl.DEFAULT_ISOLATION;
    }

    /**
     * <p>Gets this drivers major version number
     *
     * @return int representing the driver's major version
     */
    public int getDriverMajorVersion() {
        return KublingDriver.getInstance().getMajorVersion();
    }

    /**
     * <p>Gets this drivers minor version number
     *
     * @return int representing the driver's minor version
     */
    public int getDriverMinorVersion() {
        return KublingDriver.getInstance().getMinorVersion();
    }

    /**
     * <p>Get the name of this JDBC driver
     *
     * @return String representing the driver's name
     */
    public String getDriverName() {
        return KublingDriver.getInstance().getDriverName();
    }

    /**
     * <p>This method gets the version of this JDBC driver. It combines the major
     * and minor version numbers
     *
     * @return String representing the driver's version
     */
    public String getDriverVersion() {
        return getDriverMajorVersion() + "." + getDriverMinorVersion();
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schema == null) {
            schema = PERCENT;
        }

        if (table == null) {
            table = PERCENT;
        }

        ResultSet results;
        try (PreparedStatement prepareQuery = driverConnection.prepareStatement(QUERY_EXPORTED_KEYS)) {
            prepareQuery.setObject(1, catalog.toUpperCase());
            prepareQuery.setObject(2, schema.toUpperCase());
            prepareQuery.setObject(3, table.toUpperCase());

            // make a query against runtimemetadata and get results
            results = prepareQuery.executeQuery();
            ResultSet resultSet = getReferenceKeys(results);

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getExpKey_success", table));

            return resultSet;
        } catch (Exception e) {
            // logging
            String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.getExpKey_error", table, e.getMessage());
            throw KublingSQLException.create(e, logMsg);
        }
    }

    /**
     * <p>Gets the extra characters that can be used in unquoted identifier names
     * (those beyond a-z, 0-9, and _)
     *
     * @return String representing extra charachters that can be used in identifier names.
     */
    public String getExtraNameCharacters() {
        return EXTRA_CHARS;// ".@" is use in fully qualified identifier names
    }

    /**
     * <p>Return the string used to quote SQL identifiers. This returns a " " if identifier
     * quoting is not supported.
     *
     * @return string used to quote SQL identifiers.
     */
    public String getIdentifierQuoteString() {
        return DOUBLE_QUOTE;
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schema == null) {
            schema = PERCENT;
        }

        if (table == null) {
            table = PERCENT;
        }

        ResultSet results;
        try (PreparedStatement prepareQuery = driverConnection.prepareStatement(QUERY_IMPORTED_KEYS)) {
            prepareQuery.setObject(1, catalog.toUpperCase());
            prepareQuery.setObject(2, schema.toUpperCase());
            prepareQuery.setObject(3, table.toUpperCase());


            // make a query against runtime metadata and get results
            results = prepareQuery.executeQuery();

            ResultSet resultSet = getReferenceKeys(results);

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getImpKey_success", table));

            return resultSet;
        } catch (Exception e) {
            String logMsg = JDBCPlugin.Util.getString("MMDatabaseMetadata.getImpKey_error", table, e.getMessage());
            throw KublingSQLException.create(e, logMsg);
        }
    }

    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
            throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schema == null) {
            schema = PERCENT;
        }

        if (table == null) {
            table = PERCENT;
        }
        // list which represent records containing primary key info
        List records = new ArrayList();
        ResultSetMetaData rmetadata;
        ResultSetImpl results;
        PreparedStatement prepareQuery = null;

        try {
            String query = QUERY_INDEX_INFO;
            if (approximate) {
                query += " UNION ALL " + QUERY_INDEX_INFO_CARDINALITY;
            }
            query += " ORDER BY NON_UNIQUE, TYPE, INDEX_NAME, ORDINAL_POSITION";

            prepareQuery = driverConnection.prepareStatement(query);
            prepareQuery.setObject(1, catalog.toUpperCase());
            prepareQuery.setObject(2, schema.toUpperCase());
            prepareQuery.setObject(3, table.toUpperCase());
            prepareQuery.setObject(4, unique ? null : "Index");

            if (approximate) {
                prepareQuery.setObject(5, catalog.toUpperCase());
                prepareQuery.setObject(6, schema.toUpperCase());
                prepareQuery.setObject(7, table.toUpperCase());
            }

            // make a query against runtimemetadata and get results
            results = (ResultSetImpl) prepareQuery.executeQuery();

            // build the list of records from server's Results object.
            while (results.next()) {
                // list represents a record on the Results object.
                List currentRow = new ArrayList(13);
                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.
                for (int i = 0; i < JDBCColumnPositions.INDEX_INFO.MAX_COLUMNS; i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }

                // add the current row to the list of records.
                records.add(currentRow);
            }// end of while

            // get the metadata for the results
            rmetadata = results.getMetaData();
            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getIndex_success", table));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e, JDBCPlugin.Util.getString("MMDatabaseMetadata.getIndex_error", table, e.getMessage()));
        } finally {
            if (prepareQuery != null) {
                prepareQuery.close();
            }
        }
    }

    /**
     * <p>Gets the maximum number of hexadecimal characters allowed in an inline
     * binary literal
     *
     * @return int value giving maximum length of a binary literal
     */
    public int getMaxBinaryLiteralLength() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of characters allowed in a catalog name
     *
     * @return int value giving maximum length of a catalog name
     */
    public int getMaxCatalogNameLength() {
        return MAX_CATALOG_NAME_LENGTH;
    }

    /**
     * <p>Gets the maximum number of characters allowed in a character literal
     *
     * @return int value giving maximum length of a charachter literal
     */
    public int getMaxCharLiteralLength() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of characters allowed in a column name
     *
     * @return int value giving maximum length of the column name
     */
    public int getMaxColumnNameLength() {
        return MAX_COLUMN_NAME_LENGTH;
    }

    /**
     * <p>Gets the maximum number of columns allowed in a GROUP BY clause
     *
     * @return int values giving max columns in GROUP BY
     */
    public int getMaxColumnsInGroupBy() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of columns allowed in an index
     *
     * @return int gives maximum columns in an index.
     */
    public int getMaxColumnsInIndex() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of columns allowed in a ORDER BY clause
     *
     * @return int gives maximum columns in an order by.
     */
    public int getMaxColumnsInOrderBy() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of columns allowed in a SELECT clause
     *
     * @return int gives maximum columns in a select.
     */
    public int getMaxColumnsInSelect() {
        return NO_LIMIT;
    }

    public int getMaxColumnsInTable() {
        return NO_LIMIT;
    }

    public int getMaxConnections() {
        return NO_LIMIT;
    }

    public int getMaxCursorNameLength() {
        return NO_LIMIT;
    }

    public int getMaxIndexLength() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of characters allowed in a procedure name
     *
     * @return int gives maximum length of procedure name.
     */
    public int getMaxProcedureNameLength() {
        return MAX_PROCEDURE_NAME_LENGTH;
    }

    /**
     * <p>Gets the maximum number of bytes allowed in a single row
     *
     * @return int max row size in the result set.
     */
    public int getMaxRowSize() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of characters allowed in a schema name
     *
     * @return int maximum length of a schema.
     */
    public int getMaxSchemaNameLength() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of characters allowed in an SQL statement
     *
     * @return maximum length of a statement
     */
    public int getMaxStatementLength() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of active statements that may be open on one
     * connection at any time
     *
     * @return max number of open statements on a connection.
     */
    public int getMaxStatements() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of characters allowed in a table name
     *
     * @return max length of table name.
     */
    public int getMaxTableNameLength() {
        return MAX_TABLE_NAME_LENGTH;
    }

    /**
     * <p>Gets the maximum number of tables allowed in a SELECT clause
     *
     * @return max tables in a select.
     */
    public int getMaxTablesInSelect() {
        return NO_LIMIT;
    }

    /**
     * <p>Gets the maximum number of characters allowed in a username
     *
     * @return max length of username.
     */
    public int getMaxUserNameLength() {
        return MAX_USER_NAME_LENGTH;
    }

    public String getNumericFunctions() {
        return NUMERIC_FUNCTIONS;
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schema == null) {
            schema = PERCENT;
        }

        if (table == null) {
            table = PERCENT;
        }

        // list which represent records containing primary key info
        List records = new ArrayList();
        ResultSetMetaData rmetadata;
        ResultSetImpl results;
        PreparedStatement prepareQuery = null;
        try {
            prepareQuery = driverConnection.prepareStatement(QUERY_PRIMARY_KEYS);

            prepareQuery.setObject(1, catalog.toUpperCase());
            prepareQuery.setObject(2, schema.toUpperCase());
            prepareQuery.setObject(3, table.toUpperCase());

            // make a query against runtimemetadata and get results
            results = (ResultSetImpl) prepareQuery.executeQuery();

            // build the list of records from server's Results object.
            while (results.next()) {
                // list represents a record on the Results object.
                List currentRow = new ArrayList(7);
                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.
                for (int i = 0; i < JDBCColumnPositions.PRIMARY_KEYS.MAX_COLUMNS; i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }

                // add the current row to the list of records.
                records.add(currentRow);
            }// end of while

            // get the metadata for the results
            rmetadata = results.getMetaData();

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getPrimaryKey_success"));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e,
                    JDBCPlugin.Util.getString("MMDatabaseMetadata.getPrimaryKey_error", table, e.getMessage()));
        } finally {
            if (prepareQuery != null) {
                prepareQuery.close();
            }
        }
    }

    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
            throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }
        if (schemaPattern == null) {
            schemaPattern = PERCENT;
        }

        // Get all columns in all procedures if procedureNamePattern is null
        if (procedureNamePattern == null) {
            procedureNamePattern = PERCENT;
        }
        // Get all columns if columnNamePattern is null
        if (columnNamePattern == null) {
            columnNamePattern = PERCENT;
        }

        // list which represent records containing procedure column info
        List records = new ArrayList();

        ResultSetMetaData rmetadata;
        ResultSetImpl results;
        PreparedStatement prepareQuery = null;

        boolean newMetadata = driverConnection.getServerConnection().getServerVersion().compareTo("09.03") >= 0;

        try {
            String query = QUERY_PROCEDURE_COLUMNS_OLD;
            if (newMetadata) {
                query = QUERY_PROCEDURE_COLUMNS;
                if (driverConnection.getServerConnection().getServerVersion().compareTo("10.02") >= 0) {
                    query = String.format(query, "DefaultValue");
                } else {
                    query = String.format(query, "NULL");
                }
            }
            prepareQuery = driverConnection.prepareStatement(query);
            prepareQuery.setObject(1, catalog.toUpperCase());
            prepareQuery.setObject(2, schemaPattern.toUpperCase());
            prepareQuery.setObject(3, procedureNamePattern.toUpperCase());
            prepareQuery.setObject(4, columnNamePattern.toUpperCase());

            // make a query against runtimemetadata and get results
            results = (ResultSetImpl) prepareQuery.executeQuery();
            // build the list of records from server's Results object.
            while (results.next()) {
                // list represents a record on the Results object.
                List currentRow = new ArrayList(13);
                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.
                for (int i = 0; i < JDBCColumnPositions.PROCEDURE_COLUMNS.MAX_COLUMNS; i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }
                if (!newMetadata) {
                    String typeName = (String) currentRow.get(JDBCColumnPositions.PROCEDURE_COLUMNS.TYPE_NAME - 1);
                    Integer length = (Integer) currentRow.get(JDBCColumnPositions.PROCEDURE_COLUMNS.LENGTH - 1);
                    Integer precision = (Integer) currentRow.get(JDBCColumnPositions.PROCEDURE_COLUMNS.PRECISION - 1);
                    if (precision != null && precision <= 0) {
                        currentRow.set(JDBCColumnPositions.PROCEDURE_COLUMNS.PRECISION - 1, JDBCSQLTypeInfo.getDefaultPrecision(typeName));
                    }
                    if (length != null && length <= 0) {
                        currentRow.set(JDBCColumnPositions.PROCEDURE_COLUMNS.LENGTH - 1, JDBCSQLTypeInfo.getDefaultPrecision(typeName));
                    }
                    if (typeName != null) {
                        currentRow.set(JDBCColumnPositions.PROCEDURE_COLUMNS.DATA_TYPE - 1, JDBCSQLTypeInfo.getSQLType(typeName));
                    } else {
                        currentRow.set(JDBCColumnPositions.PROCEDURE_COLUMNS.DATA_TYPE - 1, null);
                    }
                }
                // add the current row to the list of records.
                records.add(currentRow);
            }// end of while
            rmetadata = results.getMetaData();

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getProcCol_success", columnNamePattern, procedureNamePattern));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e,
                    JDBCPlugin.Util.getString("MMDatabaseMetadata.getProcCol_error", columnNamePattern, e.getMessage()));
        } finally {
            if (prepareQuery != null) {
                prepareQuery.close();
            }
        }
    }

    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schemaPattern == null) {
            schemaPattern = PERCENT;
        }

        // Get all procedures if procedureNamePattern is null
        if (procedureNamePattern == null) {
            procedureNamePattern = PERCENT;
        }

        // list which represent records containing procedure info
        List records = new ArrayList();
        ResultSetMetaData rmetadata;
        ResultSetImpl results;

        try (PreparedStatement prepareQuery = driverConnection.prepareStatement(QUERY_PROCEDURES)) {
            prepareQuery.setObject(1, catalog.toUpperCase());
            prepareQuery.setObject(2, schemaPattern.toUpperCase());
            prepareQuery.setObject(3, procedureNamePattern.toUpperCase());

            // make a query against runtimemetadata and get results
            results = (ResultSetImpl) prepareQuery.executeQuery();

            // build the list of records from server's Results object.
            while (results.next()) {
                // list represents a record on the Results object.
                List currentRow = new ArrayList(JDBCColumnPositions.PROCEDURES.MAX_COLUMNS);
                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.
                // there are 3 columns are reserved for future use
                for (int i = 0; i < JDBCColumnPositions.PROCEDURES.MAX_COLUMNS; i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }

                // add the current row to the list of records.
                records.add(currentRow);

            }// end of while
            // get the metadata for the results
            rmetadata = results.getMetaData();

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getProc_success", procedureNamePattern));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e,
                    JDBCPlugin.Util.getString("MMDatabaseMetadata.getProc_error", procedureNamePattern, e.getMessage()));
        }
    }

    public String getProcedureTerm() {
        return PROCEDURE_TERM;
    }

    public ResultSet getSchemas() throws SQLException {
        return getSchemas(null, null);
    }

    public String getSchemaTerm() {
        return SCHEMA_TERM;
    }

    /**
     * <p>Gets the string that can be used to escape "_" or "%" wildcards in the
     * string search pattern used for search parameters
     *
     * @return String that is used for escaping wildcards
     */
    public String getSearchStringEscape() {
        return ESCAPE_SEARCH_STRING;
    }

    public String getSQLKeywords() {
        return KEY_WORDS;
    }

    /**
     * Indicates whether the SQLSTATE returned by SQLException.getSQLState is X/Open
     * (now known as Open Group) SQL CLI or SQL99.
     *
     * @return intValue, the type of SQLSTATE; one of: sqlStateXOpen or sqlStateSQL99
     */
    public int getSQLStateType() {
        //return sqlStateSQL99;
        return DatabaseMetaData.sqlStateSQL;
    }

    public String getStringFunctions() {
        return STRING_FUNCTIONS;
    }

    /**
     * Retrieves a description of the table hierarchies defined in a
     * particular schema in this database.
     *
     * @param catalog          A catalog name; "" retrieves those without a catalog;
     *                         null means drop catalog name from the selection criteria.
     * @param schemaPattern    A schema name pattern; "" retrieves those without a schema.
     * @param tableNamePattern A table name pattern; may be a fully-qualified name.
     * @throws SQLException since not supported
     */
    public ResultSet getSuperTables(String catalog, String schemaPattern,
                                    String tableNamePattern) throws SQLException {
        List records = new ArrayList(0);

        /* **********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         * **********************************************************************/
        Map[] metadataList = new Map[4];

        // HardCoding metadata details for TABLE_CAT column
        metadataList[0] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TABLES.TABLE_CAT,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // HardCoding metadata details for TABLE_SCHEM column
        metadataList[1] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TABLES.TABLE_SCHEM,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // HardCoding metadata details for TABLE_NAME column
        metadataList[2] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TABLES.TABLE_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for SUPERTABLE_NAME column
        metadataList[3] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TABLES.SUPERTABLE_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // construct results object from column values and their metadata
        return dummyStatement().createResultSet(records, metadataList);

    }

    /**
     * Retrieves a description of the user-defined type (UDT) hierarchies
     * defined in a particular schema in this database.
     *
     * @param catalog          A catalog name; "" retrieves those without a catalog;
     *                         null means drop catalog name from the selection criteria.
     * @param schemaPattern    A schema name pattern; "" retrieves those without a schema.
     * @param tableNamePattern A table name pattern; may be a fully-qualified name.
     */
    public ResultSet getSuperTypes(String catalog, String schemaPattern,
                                   String tableNamePattern) throws SQLException {
        List records = new ArrayList(0);

        /* **********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         * **********************************************************************/
        Map[] metadataList = new Map[6];

        // HardCoding metadata details for TYPE_CAT column
        metadataList[0] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TYPES.TYPE_CAT,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // HardCoding metadata details for TYPE_SCHEM column
        metadataList[1] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TYPES.TYPE_SCHEM,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // HardCoding metadata details for TYPE_NAME column
        metadataList[2] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TYPES.TYPE_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // HardCoding metadata details for SUPERTYPE_CAT column
        metadataList[3] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TYPES.SUPERTYPE_CAT,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // HardCoding metadata details for SUPERTYPE_SCHEM column
        metadataList[4] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TYPES.SUPERTYPE_SCHEM,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        // HardCoding metadata details for SUPERTYPE_NAME column
        metadataList[5] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.SUPER_TYPES.SUPERTYPE_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        // construct results object from column values and their metadata
        return dummyStatement().createResultSet(records, metadataList);
    }

    public String getSystemFunctions() {
        return SYSTEM_FUNCTIONS;
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableName) throws SQLException {
        List records = new ArrayList(0);
        /* **********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         * **********************************************************************/
        Map[] metadataList = new Map[7];

        metadataList[0] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.TABLE_CAT,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);
        metadataList[1] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.TABLE_SCHEM,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);
        metadataList[2] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.TABLE_NAME,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);
        metadataList[3] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.GRANTOR,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);
        metadataList[4] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.GRANTEE,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);
        metadataList[5] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.PRIVILEGE,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);
        metadataList[6] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.PRIVILEGES.IS_GRANTABLE,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NULLABLE, driverConnection);

        return dummyStatement().createResultSet(records, metadataList);

    }

    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
            throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schemaPattern == null) {
            schemaPattern = PERCENT;
        }

        // Get all tables if tableNamePattern is null
        if (tableNamePattern == null) {
            tableNamePattern = PERCENT;
        }

        // list which represent records containing tables info
        List records = new ArrayList();

        // query string to be submitted to get table metadata info
        StringBuilder sqlQuery = new StringBuilder(QUERY_TABLES);

        if (types != null) {
            StringBuilder typesString = new StringBuilder("("); // criteria string for different table types
            if (types.length == 0) {
                typesString.append("1 = 0");
            } else {
                // construct the criteria string
                for (int i = 0; i < types.length; i++) {
                    if (types[i] != null && !types[i].isEmpty()) {
                        if (i > 0) {
                            typesString.append(" OR ");
                        }
                        typesString.append(TABLE_TYPE).append(LIKE_ESCAPE);
                    }
                }
            }
            typesString.append(")");
            sqlQuery.append(" AND ").append(typesString);
        }

        sqlQuery.append(" ORDER BY TABLE_TYPE, TABLE_SCHEM, TABLE_NAME");


        ResultSetMetaData rmetadata;
        ResultSetImpl results;

        try (PreparedStatement prepareQuery = driverConnection.prepareStatement(sqlQuery.toString())) {
            int columnIndex = 0;
            prepareQuery.setObject(++columnIndex, catalog.toUpperCase());
            prepareQuery.setObject(++columnIndex, schemaPattern.toUpperCase());
            prepareQuery.setObject(++columnIndex, tableNamePattern.toUpperCase());

            if (types != null) {
                for (String type : types) {
                    if (type != null && !type.isEmpty()) {
                        prepareQuery.setObject(++columnIndex, type.toUpperCase());
                    }
                }
            }

            // make a query against runtimemetadata and get results
            results = (ResultSetImpl) prepareQuery.executeQuery();

            // build the list of records from server's Results object.
            while (results.next()) {
                // list represents a record on the Results object.
                List currentRow = new ArrayList(11);
                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.
                for (int i = 0; i < JDBCColumnPositions.TABLES.MAX_COLUMNS; i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }

                // add the current row to the list of records.
                records.add(currentRow);
            }// end of while

            // get the metadata for the results
            rmetadata = results.getMetaData();

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getTable_success", tableNamePattern));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e,
                    JDBCPlugin.Util.getString("MMDatabaseMetadata.getTable_error", tableNamePattern, e.getMessage()));
        }
    }

    public ResultSet getTableTypes() throws SQLException {

        // list which represent records containing Table Type info
        List records = new ArrayList(5);
        /* *******************************
         * HardCoding JDBC specific values
         * ********************************/

        records.add(Arrays.asList("DOCUMENT"));
        records.add(Arrays.asList("TABLE"));
        records.add(Arrays.asList("VIEW"));
        records.add(Arrays.asList("XMLSTAGINGTABLE"));
        records.add(Arrays.asList("SYSTEM TABLE"));

        /* **********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         * **********************************************************************/

        Map[] metadataList = new Map[1];

        metadataList[0] = StatementImpl.getColumnMetadata(null, JDBCColumnNames.TABLE_TYPES.TABLE_TYPE,
                DataTypeManager.DefaultDataTypes.STRING, ResultsMetadataConstants.NULL_TYPES.NOT_NULL, driverConnection);

        logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getTableType_success"));

        // construct results object from column values and their metadata
        return dummyStatement().createResultSet(records, metadataList);
    }

    public String getTimeDateFunctions() {
        return DATE_FUNCTIONS;
    }

    public ResultSet getTypeInfo() throws SQLException {
        if (driverConnection.getServerConnection().getServerVersion().compareTo("09.03") >= 0) {
            //use the system table
            ResultSetMetaData rmetadata;
            ResultSetImpl results;

            List<List<?>> records = new ArrayList<>();
            try (PreparedStatement prepareQuery = driverConnection.prepareStatement(QUERY_TYPEINFO)) {

                results = (ResultSetImpl) prepareQuery.executeQuery();

                while (results.next()) {
                    List<Object> currentRow = new ArrayList<>();
                    for (int i = 0; i < results.getMetaData().getColumnCount(); i++) {
                        currentRow.add(results.getObject(i + 1));
                    }

                    records.add(currentRow);
                }

                rmetadata = results.getMetaData();

                return dummyStatement().createResultSet(records, rmetadata);
            } catch (Exception e) {
                throw KublingSQLException.create(e,
                        JDBCPlugin.Util.getString("MMDatabaseMetadata.getTypeInfo_error", e.getMessage()));
            }
        }
        return getStaticTypeInfo();
    }

    private ResultSet getStaticTypeInfo() throws SQLException {
        // list which represent records containing data type info
        List<List<Object>> records = new ArrayList<>();

        records.add(Arrays.asList(
                createTypeInfoRow("boolean", "{b'", "}", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("byte", null, null, Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("tinyint", null, null, Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("long", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("bigint", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("char", "'", "'", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("bigdecimal", null, null, Boolean.FALSE, Boolean.TRUE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("decimal", null, null, Boolean.FALSE, Boolean.TRUE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("biginteger", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("integer", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("short", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("smallint", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("float", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("real", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("double", null, null, Boolean.FALSE, Boolean.FALSE, 10)));
        records.add(Arrays.asList(
                createTypeInfoRow("string", "'", "'", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("varchar", "'", "'", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("xml", null, null, Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("date", "{d'", "}", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("time", "{t'", "}", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("timestamp", "{ts'", "}", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("object", null, null, Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("blob", null, null, Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("varbinary", "X'", "'", Boolean.TRUE, Boolean.TRUE, 0)));
        records.add(Arrays.asList(
                createTypeInfoRow("clob", null, null, Boolean.TRUE, Boolean.TRUE, 0)));

        Map[] metadataList = new Map[18];

        metadataList[0] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.TYPE_NAME,
                DataTypeManager.DefaultDataTypes.STRING,
                ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                driverConnection);

        metadataList[1] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.DATA_TYPE,
                DataTypeManager.DefaultDataTypes.INTEGER,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[2] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.PRECISION,
                DataTypeManager.DefaultDataTypes.INTEGER,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[3] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.LITERAL_PREFIX,
                DataTypeManager.DefaultDataTypes.STRING,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[4] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.LITERAL_SUFFIX,
                DataTypeManager.DefaultDataTypes.STRING,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[5] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.CREATE_PARAMS,
                DataTypeManager.DefaultDataTypes.STRING,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[6] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.NULLABLE,
                DataTypeManager.DefaultDataTypes.SHORT,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[7] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.CASE_SENSITIVE,
                DataTypeManager.DefaultDataTypes.BOOLEAN,
                ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                driverConnection);

        metadataList[8] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.SEARCHABLE,
                DataTypeManager.DefaultDataTypes.SHORT,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[9] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.UNSIGNED_ATTRIBUTE,
                DataTypeManager.DefaultDataTypes.BOOLEAN,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[10] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.FIXED_PREC_SCALE,
                DataTypeManager.DefaultDataTypes.BOOLEAN,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[11] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.AUTOINCREMENT,
                DataTypeManager.DefaultDataTypes.BOOLEAN,
                ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                driverConnection);

        metadataList[12] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.LOCAL_TYPE_NAME,
                DataTypeManager.DefaultDataTypes.STRING,
                ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                driverConnection);

        metadataList[13] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.MINIMUM_SCALE,
                DataTypeManager.DefaultDataTypes.SHORT,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[14] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.MAXIMUM_SCALE,
                DataTypeManager.DefaultDataTypes.SHORT,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[15] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.SQL_DATA_TYPE,
                DataTypeManager.DefaultDataTypes.INTEGER,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[16] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.SQL_DATETIME_SUB,
                DataTypeManager.DefaultDataTypes.INTEGER,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                driverConnection);

        metadataList[17] = StatementImpl.getColumnMetadata(
                CoreConstants.SYSTEM_MODEL + "." + DATA_TYPES,
                JDBCColumnNames.TYPE_INFO.NUM_PREC_RADIX,
                DataTypeManager.DefaultDataTypes.INTEGER,
                ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                driverConnection);

        ResultSetMetaData rmetadata = new ResultSetMetaDataImpl(new MetadataProvider(metadataList), null);

        logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getTypes_success"));

        // construct results object from column values and their metadata
        return dummyStatement().createResultSet(records, rmetadata);
    }

    private Object[] createTypeInfoRow(String typeName, String prefix, String suffix, Boolean unsigned, Boolean fixedPrecScale, int radix) {
        return new Object[]{
                typeName,
                JDBCSQLTypeInfo.getSQLType(typeName),
                JDBCSQLTypeInfo.getDefaultPrecision(typeName),
                prefix, suffix, null,
                (short) DatabaseMetaData.typeNullable,
                Boolean.FALSE, (short) DatabaseMetaData.typeSearchable,
                unsigned, fixedPrecScale, Boolean.FALSE, typeName,
                (short) 0, (short) 255, null, null, radix};
    }

    /**
     * <p>Gets a description of the user-defined types defined in a particular
     * schema.  Schema-specific UDTs may have type JAVA_OBJECT, STRUCT, or DISTINCT.
     * Supports 1.4
     *
     * @param catalog         a catalog name
     * @param schemaPattern   a schema name pattern
     * @param typeNamePattern a type name pattern
     * @param types           a list of user-named types to include (JAVA_OBJECT, STRUCT,
     *                        or DISTINCT); null returns all types
     * @return ResultSet. Empty ResultSet object as this method is not supported.
     * @throws SQLException if catalog/schema info does not match for the given connection.
     */
    public ResultSet getUDTs(String catalog, String schemaPattern,
                             String typeNamePattern, int[] types) throws SQLException {
        return emptyUDTSResultSet();
    }

    /**
     * Return an empty result set for to aid getUDTS() functions.
     *
     * @return ResultSet.
     */
    private ResultSet emptyUDTSResultSet() throws SQLException {
        String[] columnNames = new String[]{
                JDBCColumnNames.UDTS.TYPE_CAT,
                JDBCColumnNames.UDTS.TYPE_SCHEM,
                JDBCColumnNames.UDTS.TYPE_NAME,
                JDBCColumnNames.UDTS.CLASS_NAME,
                JDBCColumnNames.UDTS.DATA_TYPE,
                JDBCColumnNames.UDTS.REMARKS,
                JDBCColumnNames.UDTS.BASE_TYPE
        };
        String[] dataTypes = new String[]{
                DataTypeManager.DefaultDataTypes.STRING,
                DataTypeManager.DefaultDataTypes.STRING,
                DataTypeManager.DefaultDataTypes.STRING,
                DataTypeManager.DefaultDataTypes.STRING,
                DataTypeManager.DefaultDataTypes.STRING,
                DataTypeManager.DefaultDataTypes.STRING,
                DataTypeManager.DefaultDataTypes.SHORT
        };
        return dummyStatement().createResultSet(Collections.EMPTY_LIST, columnNames, dataTypes);
    }

    private StatementImpl dummyStatement() {
        return new StatementImpl(this.driverConnection, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    }

    public String getURL() {
        return driverConnection.getUrl();
    }

    public String getUserName() throws SQLException {
        return driverConnection.getUserName();
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {

        // ResultSet returned has the same columns as best row identifier
        // Method not supported, retuning empty ResultSet
        ResultSet resultSet = getBestRowIdentifier(catalog, schema, table, 0, true);

        logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getVersionCols_success"));

        return resultSet;
    }


    /**
     * <p>Checks whether a catalog name appears at the start of a fully qualified table
     * name. If it is not at the beginning, it appears at the end.
     *
     * @return if so return true, else false.
     */
    public boolean isCatalogAtStart() {
        return false;
    }

    @Override
    public boolean insertsAreDetected(int type) {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    /**
     * <p>Indicates whether updates made to a LOB are made on a copy or directly to the LOB.
     *
     * @return if so return true, else false.
     */
    public boolean locatorsUpdateCopy() {
        return false;
    }

    /**
     * <p>Checks whether the concatenation of a NULL value and a non-NULL value results
     * in a NULL value.
     *
     * @return if so return true, else false.
     */
    public boolean nullPlusNonNullIsNull() {
        return true;
    }

    /**
     * <p>Checks whether NULL values are sorted at the end regardless of sort order.
     *
     * @return if so return true, else false.
     */
    public boolean nullsAreSortedAtEnd() {
        return nullSort == NullSort.AtEnd;
    }

    /**
     * <p>Checks whether NULL values are sorted at the start regardless of sort order.
     *
     * @return if so return true, else false.
     */
    public boolean nullsAreSortedAtStart() {
        return nullSort == NullSort.AtStart;
    }

    /**
     * <p>Checks whether NULL values are sorted high.
     *
     * @return if so return true, else false.
     */
    public boolean nullsAreSortedHigh() {
        return nullSort == NullSort.High;
    }

    /**
     * <p>Checks whether NULL values are sorted low.
     *
     * @return if so return true, else false.
     */
    public boolean nullsAreSortedLow() {
        return nullSort == NullSort.Low;
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) {
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(int type) {
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(int type) {
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) {
        return false;
    }

    @Override
    public boolean othersDeletesAreVisible(int type) {
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(int type) {
        return false;
    }

    public boolean storesLowerCaseIdentifiers() {
        return false;
    }

    public boolean storesLowerCaseQuotedIdentifiers() {
        return false;
    }

    public boolean storesMixedCaseIdentifiers() {
        return true;
    }

    public boolean storesMixedCaseQuotedIdentifiers() {
        return true;
    }

    public boolean storesUpperCaseIdentifiers() {
        return false;
    }

    public boolean storesUpperCaseQuotedIdentifiers() {
        return false;
    }

    public boolean supportsAlterTableWithAddColumn() {
        return false;
    }

    public boolean supportsAlterTableWithDropColumn() {
        return false;
    }

    public boolean supportsANSI92EntryLevelSQL() {
        return false;
    }

    public boolean supportsANSI92FullSQL() {
        return false;
    }

    public boolean supportsANSI92IntermediateSQL() {
        return false;
    }

    /**
     * <p>Indicates whether the driver supports batch updates.
     *
     * @return true if the driver supports batch updates; false otherwise
     */
    public boolean supportsBatchUpdates() {
        return true;
    }

    public boolean supportsCatalogsInDataManipulation() {
        return false;
    }

    public boolean supportsCatalogsInIndexDefinitions() {
        return false;
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() {
        return false;
    }

    public boolean supportsCatalogsInProcedureCalls() {
        return false;
    }

    public boolean supportsCatalogsInTableDefinitions() {
        return false;
    }

    public boolean supportsColumnAliasing() {
        return true;
    }

    public boolean supportsConvert() {
        return true;
    }

    public boolean supportsConvert(int fromType, int toType) {
        String fromName = JDBCSQLTypeInfo.getTypeName(fromType);
        String toName = JDBCSQLTypeInfo.getTypeName(toType);

        if (fromName.equals(toName)) {
            return true;
        }
        return DataTypeManager.isTransformable(fromName, toName);
    }

    public boolean supportsCorrelatedSubqueries() {
        return true;
    }

    public boolean supportsCoreSQLGrammar() {
        return false;
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return false;
    }

    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    public boolean supportsExpressionsInOrderBy() {
        return true;
    }

    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    public boolean supportsFullOuterJoins() {
        return true;
    }

    public boolean supportsGetGeneratedKeys() {
        return true;
    }

    public boolean supportsGroupBy() {
        return true;
    }

    public boolean supportsGroupByBeyondSelect() {
        return true;
    }

    public boolean supportsGroupByUnrelated() {
        return true;
    }

    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    public boolean supportsLikeEscapeClause() {
        return true;
    }

    public boolean supportsLimitedOuterJoins() {
        return true;
    }

    public boolean supportsMinimumSQLGrammar() {
        return true;
    }

    public boolean supportsMixedCaseIdentifiers() {
        return false;
    }

    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    public boolean supportsMultipleResultSets() {
        return false;
    }

    public boolean supportsMultipleOpenResults() {
        return false;
    }

    public boolean supportsMultipleTransactions() {
        return true;
    }

    public boolean supportsNamedParameters() {
        return true;
    }

    public boolean supportsNonNullableColumns() {
        return true;
    }

    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    public boolean supportsOpenStatementsAcrossCommit() {
        return true;
    }

    public boolean supportsOpenStatementsAcrossRollback() {
        return true;
    }

    public boolean supportsOrderByUnrelated() {
        return true;
    }

    public boolean supportsOuterJoins() {
        return true;
    }

    public boolean supportsPositionedDelete() {
        return false;
    }

    public boolean supportsPositionedUpdate() {
        return false;
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency) {
        if (type == ResultSet.TYPE_FORWARD_ONLY || type == ResultSet.TYPE_SCROLL_INSENSITIVE) {
            return concurrency == ResultSet.CONCUR_READ_ONLY;
        }
        return false;
    }

    @Override
    public boolean supportsResultSetHoldability(int holdability) {
        return false;
    }

    /**
     * <p>Does the database support the given result set type?
     *
     * @param type defined in <code>java.sql.ResultSet</code>
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @see Connection
     */
    public boolean supportsResultSetType(int type) {

        return type == ResultSet.TYPE_FORWARD_ONLY || type == ResultSet.TYPE_SCROLL_INSENSITIVE;
    }

    public boolean supportsSavepoints() {
        return false;
    }

    public boolean supportsSchemasInDataManipulation() {
        return true;
    }

    public boolean supportsSchemasInIndexDefinitions() {
        return false;
    }

    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false;
    }

    public boolean supportsSchemasInProcedureCalls() {
        return true;
    }

    public boolean supportsSchemasInTableDefinitions() {
        return false;
    }

    public boolean supportsSelectForUpdate() {
        return false;
    }

    public boolean supportsStatementPooling() {
        return false;
    }

    public boolean supportsStoredProcedures() {
        return true;
    }

    public boolean supportsSubqueriesInComparisons() {
        return true;
    }

    public boolean supportsSubqueriesInExists() {
        return true;
    }

    public boolean supportsSubqueriesInIns() {
        return true;
    }

    public boolean supportsSubqueriesInQuantifieds() {
        return true;
    }

    public boolean supportsTableCorrelationNames() {
        return true;
    }

    public boolean supportsTransactionIsolationLevel(int level) {
        return false;
    }

    public boolean supportsTransactions() {
        return true;
    }

    public boolean supportsUnion() {
        return true;
    }

    public boolean supportsUnionAll() {
        return true;
    }

    public boolean updatesAreDetected(int type) {
        return false;
    }

    public boolean usesLocalFilePerTable() {
        return false;
    }

    public boolean usesLocalFiles() {
        return false;
    }

    /**
     * <p>This method is used to produce ResultSets from server's Results objects for
     * getCrossReference, getExportedKeys and getImportedKeys methods.
     *
     * @param results server's Results object.
     * @return ResultSet object giving the reference key info.
     * @throws SQLException if there is an accesing server results
     */
    private ResultSet getReferenceKeys(ResultSet results) throws SQLException {

        // list which represent records containing reference key info
        List records = new ArrayList();
        ResultSetMetaData rmetadata;
        try {
            // build the list of records from Results object.
            while (results.next()) {
                // list represents a record on the Results object.
                List currentRow = new ArrayList(15);
                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.
                for (int i = 0; i < JDBCColumnPositions.REFERENCE_KEYS.MAX_COLUMNS; i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }

                // add the current row to the list of records.
                records.add(currentRow);
            }// end of while

            // get the metadata for the results
            rmetadata = results.getMetaData();

        } catch (Exception e) {
            String msg = JDBCPlugin.Util.getString("MMDatabaseMetadata.Err_getting_primary_keys");
            throw KublingSQLException.create(e, msg);
        }

        // close the resultset and driver connection
        //results.close();

        logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getRefKey_success"));

        // construct results object from column values and their metadata
        return dummyStatement().createResultSet(records, rmetadata);
    }

    public boolean autoCommitFailureClosesAllResultSets() {
        return false;
    }

    public int getResultSetHoldability() {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    public Connection getConnection() throws SQLException {
        return driverConnection;
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() {
        return false;
    }

    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern)
            throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public ResultSet getClientInfoProperties() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public ResultSet getFunctionColumns(String catalog, String schemaPattern,
                                        String functionNamePattern, String columnNamePattern)
            throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schemaPattern == null) {
            schemaPattern = PERCENT;
        }

        if (functionNamePattern == null) {
            functionNamePattern = PERCENT;
        }

        if (columnNamePattern == null) {
            columnNamePattern = PERCENT;
        }

        List records = new ArrayList();

        ResultSetMetaData rmetadata;
        ResultSetImpl results;

        boolean newMetadata = driverConnection.getServerConnection().getServerVersion().compareTo("09.03") >= 0;
        try (PreparedStatementImpl prepareQuery = driverConnection.prepareStatement(newMetadata
                ? QUERY_FUNCTION_COLUMNS
                : QUERY_FUNCTION_COLUMNS_OLD)) {
            prepareQuery.setString(1, catalog.toUpperCase());
            prepareQuery.setString(2, schemaPattern.toUpperCase());
            prepareQuery.setString(3, functionNamePattern.toUpperCase());
            prepareQuery.setString(4, columnNamePattern.toUpperCase());
            results = prepareQuery.executeQuery();
            // Get the metadata for the results
            rmetadata = results.getMetaData();
            int cols = rmetadata.getColumnCount();
            while (results.next()) {
                List currentRow = new ArrayList(cols);
                for (int i = 0; i < cols; i++) {
                    currentRow.add(results.getObject(i + 1));
                }
                if (!newMetadata) {
                    String typeName = (String) currentRow.get(6);
                    Integer length = (Integer) currentRow.get(8);
                    Integer precision = (Integer) currentRow.get(7);
                    if (precision != null && precision <= 0) {
                        currentRow.set(7, JDBCSQLTypeInfo.getDefaultPrecision(typeName));
                    }
                    if (length != null && length <= 0) {
                        currentRow.set(8, JDBCSQLTypeInfo.getDefaultPrecision(typeName));
                    }
                    if (typeName != null) {
                        currentRow.set(5, JDBCSQLTypeInfo.getSQLType(typeName));
                    } else {
                        currentRow.set(5, null);
                    }
                }
                // add the current row to the list of records.
                records.add(currentRow);
            }// end of while

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getfunctioncolumns_success"));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e, JDBCPlugin.Util.getString("MMDatabaseMetadata.getfunctioncolumns_error", e.getMessage()));
        }
    }

    public ResultSet getFunctions(String catalog, String schemaPattern,
                                  String functionNamePattern) throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schemaPattern == null) {
            schemaPattern = PERCENT;
        }

        if (functionNamePattern == null) {
            functionNamePattern = PERCENT;
        }
        List records = new ArrayList();

        ResultSetMetaData rmetadata;
        ResultSetImpl results;
        try (PreparedStatementImpl prepareQuery = driverConnection.prepareStatement(QUERY_FUNCTIONS)) {
            prepareQuery.setString(1, catalog.toUpperCase());
            prepareQuery.setString(2, schemaPattern.toUpperCase());
            prepareQuery.setString(3, functionNamePattern.toUpperCase());
            results = prepareQuery.executeQuery();
            // Get the metadata for the results
            rmetadata = results.getMetaData();
            int cols = rmetadata.getColumnCount();
            while (results.next()) {
                List currentRow = new ArrayList(cols);

                for (int i = 0; i < cols; i++) {
                    currentRow.add(results.getObject(i + 1));
                }

                records.add(currentRow);
            }

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getfunctions_success"));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e, JDBCPlugin.Util.getString("MMDatabaseMetadata.getfunctions_error", e.getMessage()));
        }
    }

    public RowIdLifetime getRowIdLifetime() throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }

    public ResultSet getSchemas(String catalog, String schemaPattern)
            throws SQLException {
        if (catalog == null) {
            catalog = PERCENT;
        }

        if (schemaPattern == null) {
            schemaPattern = PERCENT;
        }
        // list which represent records containing schema info
        List records = new ArrayList();

        ResultSetMetaData rmetadata;
        ResultSetImpl results;
        try (PreparedStatementImpl prepareQuery = driverConnection.prepareStatement(QUERY_SCHEMAS)) {
            prepareQuery.setObject(1, catalog.toUpperCase());
            prepareQuery.setObject(2, schemaPattern.toUpperCase());
            // make a query against runtimemetadata and get results
            results = prepareQuery.executeQuery();

            while (results.next()) {
                // each row will have only one column(Virtual database name)
                List currentRow = new ArrayList(2);

                for (int i = 0; i < JDBCColumnPositions.SCHEMAS.MAX_COLUMNS; i++) {
                    // get the value at the current index add it to currentRow
                    currentRow.add(results.getObject(i + 1));
                }

                records.add(currentRow);
            }

            // Get the metadata for the results
            rmetadata = results.getMetaData();

            logger.fine(JDBCPlugin.Util.getString("MMDatabaseMetadata.getschema_success"));

            // construct results object from column values and their metadata
            return dummyStatement().createResultSet(records, rmetadata);
        } catch (Exception e) {
            throw KublingSQLException.create(e, JDBCPlugin.Util.getString("MMDatabaseMetadata.getschema_error", e.getMessage()));
        }
    }

    public boolean generatedKeyAlwaysReturned() {
        return false;
    }

    public ResultSet getPseudoColumns(String catalog, String schemaPattern,
                                      String tableNamePattern, String columnNamePattern) throws SQLException {
        throw SqlUtil.createFeatureNotSupportedException();
    }
}
