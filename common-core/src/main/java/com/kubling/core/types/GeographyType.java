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

package com.kubling.core.types;

import java.io.Serial;
import java.sql.Blob;

public final class GeographyType extends AbstractGeospatialType {

    @Serial
    private static final long serialVersionUID = 7304786855176679353L;

    public static final int DEFAULT_SRID = 4326;

    public GeographyType() {
        setSrid(DEFAULT_SRID);
    }

    public GeographyType(Blob blob) {
        this(blob, DEFAULT_SRID);
    }

    public GeographyType(byte[] bytes) {
        this(bytes, DEFAULT_SRID);
    }

    public GeographyType(Blob blob, int srid) {
        super(blob);
        setSrid(srid);
    }

    public GeographyType(byte[] bytes, int srid) {
        super(bytes);
        setSrid(srid);
    }

    /**
     * Creates a geography whose coordinate reference system is explicitly unknown.
     */
    public static GeographyType withUnknownSrid(Blob blob) {
        GeographyType result = new GeographyType(blob);
        result.setSridDirect(GeometryType.UNKNOWN_SRID);
        return result;
    }

    /**
     * Creates a geography whose coordinate reference system is explicitly unknown.
     */
    public static GeographyType withUnknownSrid(byte[] bytes) {
        GeographyType result = new GeographyType(bytes);
        result.setSridDirect(GeometryType.UNKNOWN_SRID);
        return result;
    }

    @Override
    public void setSrid(int srid) {
        if (srid == GeometryType.UNKNOWN_SRID) {
            srid = DEFAULT_SRID;
        }
        super.setSrid(srid);
    }

}
