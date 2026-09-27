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

import com.kubling.core.types.BinaryType;
import com.kubling.core.types.DataTypeManager;
import com.kubling.core.util.ReaderInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;


/**
 * <p>This class is used to transform objects into desired data types. The static
 * method on this class are used by Metadatresults, ResultsWrapper and
 * MMCallableStatement classes.
 */
final class DataTypeTransformer {

    // Prevent instantiation
    private DataTypeTransformer() {
    }

    /**
     * Gets an object value and transforms it into a java.math.BigDecimal object.
     *
     * @param value the object to be transformed
     * @return a BigDecimal object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static BigDecimal getBigDecimal(Object value) throws SQLException {
        return transform(value, BigDecimal.class);
    }

    static <T> T transform(Object value, Class<T> targetType) throws SQLException {
        return transform(value, targetType, getRuntimeType(targetType));
    }

    static <T> T transform(Object value, Class<T> targetType, Class<?> runtimeType) throws SQLException {
        if (value == null || targetType.isAssignableFrom(value.getClass())) {
            return targetType.cast(value);
        }
        if (targetType == byte[].class) {
            switch (value) {
                case Blob blob -> {
                    long length = blob.length();
                    if (length > Integer.MAX_VALUE) {
                        throw new KublingSQLException(JDBCPlugin.Util.getString("DataTypeTransformer.blob_too_big"));
                    }
                    return targetType.cast(blob.getBytes(1, (int) length));
                }
                case String s -> {
                    return targetType.cast(s.getBytes());
                }
                case BinaryType binaryType -> {
                    return targetType.cast(binaryType.getBytesDirect());
                }
                default -> {
                }
            }
        } else if (targetType == String.class) {
            if (value instanceof SQLXML) {
                return targetType.cast(((SQLXML) value).getString());
            } else if (value instanceof Clob c) {
                long length = c.length();
                if (length == 0) {
                    //there is a bug in SerialClob with 0 length
                    return targetType.cast("");
                }
                return targetType.cast(c.getSubString(1, length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length));
            }
        }
        try {
            return (T) DataTypeManager.transformValue(DataTypeManager.convertToRuntimeType(value, true), runtimeType);
        } catch (Exception e) {
            String valueStr = value.toString();
            if (valueStr.length() > 20) {
                valueStr = valueStr.substring(0, 20) + "...";
            }
            String msg = JDBCPlugin.Util.getString("DataTypeTransformer.Err_converting", valueStr, targetType.getSimpleName());
            throw KublingSQLException.create(e, msg);
        }
    }

    static <T> Class<?> getRuntimeType(Class<T> type) {
        Class<?> runtimeType = type;
        if (!DataTypeManager.getAllDataTypeClasses().contains(type)) {
            if (type == Clob.class) {
                runtimeType = DataTypeManager.DefaultDataClasses.CLOB;
            } else if (type == Blob.class) {
                runtimeType = DataTypeManager.DefaultDataClasses.BLOB;
            } else if (type == SQLXML.class) {
                runtimeType = DataTypeManager.DefaultDataClasses.XML;
            } else if (type == byte[].class) {
                runtimeType = DataTypeManager.DefaultDataClasses.VARBINARY;
            } else {
                runtimeType = DataTypeManager.DefaultDataClasses.OBJECT;
            }
        }
        return runtimeType;
    }

    /**
     * Gets an object value and transforms it into a boolean
     *
     * @param value the object to be transformed
     * @return a Boolean object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static boolean getBoolean(Object value) throws SQLException {
        if (value == null) {
            return false;
        }
        return transform(value, Boolean.class);
    }

    /**
     * Gets an object value and transforms it into a byte
     *
     * @param value the object to be transformed
     * @return a Byte object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static byte getByte(Object value) throws SQLException {
        if (value == null) {
            return 0;
        }
        return transform(value, Byte.class);
    }

    static byte[] getBytes(Object value) throws SQLException {
        return transform(value, byte[].class);
    }

    static Character getCharacter(Object value) throws SQLException {
        return transform(value, Character.class);
    }

    /**
     * Gets an object value and transforms it into a java.sql.Date object.
     *
     * @param value the object to be transformed
     * @throws SQLException if failed to transform to the desired datatype
     */
    static Date getDate(Object value) throws SQLException {
        return transform(value, Date.class);
    }

    /**
     * Gets an object value and transforms it into a double
     *
     * @param value the object to be transformed
     * @return a Double object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static double getDouble(Object value) throws SQLException {
        if (value == null) {
            return 0;
        }
        return transform(value, Double.class);
    }

    /**
     * Gets an object value and transforms it into a float
     *
     * @param value the object to be transformed
     * @return a Float object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static float getFloat(Object value) throws SQLException {
        if (value == null) {
            return 0;
        }
        return transform(value, Float.class);
    }

    /**
     * Gets an object value and transforms it into an integer
     *
     * @param value the object to be transformed
     * @return a Integer object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static int getInteger(Object value) throws SQLException {
        if (value == null) {
            return 0;
        }
        return transform(value, Integer.class);
    }

    /**
     * Gets an object value and transforms it into a long
     *
     * @param value the object to be transformed
     * @return a Long object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static long getLong(Object value) throws SQLException {
        if (value == null) {
            return 0;
        }
        return transform(value, Long.class);
    }

    /**
     * Gets an object value and transforms it into a short
     *
     * @param value the object to be transformed
     * @return a Short object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static short getShort(Object value) throws SQLException {
        if (value == null) {
            return 0;
        }
        return transform(value, Short.class);
    }

    /**
     * Gets an object value and transforms it into a java.sql.Time object.
     *
     * @param value the object to be transformed
     * @return a Time object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static Time getTime(Object value) throws SQLException {
        return transform(value, Time.class);
    }

    /**
     * Gets an object value and transforms it into a java.sql.Timestamp object.
     *
     * @param value the object to be transformed
     * @return a Timestamp object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static Timestamp getTimestamp(Object value) throws SQLException {
        return transform(value, Timestamp.class);
    }

    static String getString(Object value) throws SQLException {
        return transform(value, String.class);
    }

    /**
     * Gets an object value and transforms it into a java.sql.Timestamp object.
     *
     * @param value the object to be transformed
     * @return a Timestamp object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static Blob getBlob(Object value) throws SQLException {
        return transform(value, Blob.class);
    }

    /**
     * Gets an object value and transforms it into a java.sql.Timestamp object.
     *
     * @param value the object to be transformed
     * @return a Timestamp object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static Clob getClob(Object value) throws SQLException {
        return transform(value, Clob.class);
    }

    /**
     * Gets an object value and transforms it into a SQLXML object.
     *
     * @param value the object to be transformed
     * @return a SQLXML object
     * @throws SQLException if failed to transform to the desired datatype
     */
    static SQLXML getSQLXML(Object value) throws SQLException {
        return transform(value, SQLXML.class);
    }

    static Reader getCharacterStream(Object value) throws SQLException {
        return switch (value) {
            case null -> null;
            case Clob clob -> clob.getCharacterStream();
            case SQLXML sqlxml -> sqlxml.getCharacterStream();
            default -> new StringReader(getString(value));
        };

    }

    static InputStream getAsciiStream(Object value) throws SQLException {
        return switch (value) {
            case null -> null;
            case Clob clob -> clob.getAsciiStream();
            case SQLXML sqlxml ->
                //TODO: could check the SQLXML encoding
                    new ReaderInputStream(sqlxml.getCharacterStream(), StandardCharsets.US_ASCII);
            default -> new ByteArrayInputStream(getString(value).getBytes(StandardCharsets.US_ASCII));
        };

    }

    static NClob getNClob(Object value) throws SQLException {
        final Clob clob = getClob(value);
        if (clob == null) {
            return null;
        }
        if (clob instanceof NClob) {
            return (NClob) clob;
        }
        return (NClob) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{NClob.class}, (proxy, method, args) -> {
            try {
                return method.invoke(clob, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    static Array getArray(Object obj) throws SQLException {
        //TODO: type primitive arrays more closely
        return transform(obj, Array.class, Object[].class);
    }

}