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

package com.kubling.core.types;

import com.kubling.core.util.UnitTestUtil;
import org.junit.jupiter.api.Test;

import javax.sql.rowset.serial.SerialBlob;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGeographyType {

    @Test
    public void testLegacyDefaultSrid() throws Exception {
        assertEquals(GeographyType.DEFAULT_SRID, new GeographyType().getSrid());
        assertEquals(GeographyType.DEFAULT_SRID, new GeographyType(new byte[0]).getSrid());
        assertEquals(GeographyType.DEFAULT_SRID, new GeographyType(new byte[0],
                GeometryType.UNKNOWN_SRID).getSrid());
        assertEquals(GeographyType.DEFAULT_SRID, new GeographyType(
                new SerialBlob(new byte[0]), GeometryType.UNKNOWN_SRID).getSrid());
    }

    @Test
    public void testExplicitUnknownSrid() throws Exception {
        GeographyType bytes = GeographyType.withUnknownSrid(new byte[0]);
        GeographyType blob = GeographyType.withUnknownSrid(new SerialBlob(new byte[0]));

        assertEquals(GeometryType.UNKNOWN_SRID, bytes.getSrid());
        assertEquals(GeometryType.UNKNOWN_SRID, blob.getSrid());
    }

    @Test
    public void testCopyPreservesSrid() {
        GeographyType unknown = GeographyType.withUnknownSrid(new byte[0]);
        GeographyType unknownCopy = new GeographyType();
        unknown.copyTo(unknownCopy);
        assertEquals(GeometryType.UNKNOWN_SRID, unknownCopy.getSrid());

        GeographyType legacy = new GeographyType(new byte[0]);
        GeographyType legacyCopy = GeographyType.withUnknownSrid(new byte[0]);
        legacy.copyTo(legacyCopy);
        assertEquals(GeographyType.DEFAULT_SRID, legacyCopy.getSrid());
    }

    @Test
    public void testGeographyToGeometryPreservesUnknownSrid() throws Exception {
        GeographyType unknown = GeographyType.withUnknownSrid(new byte[0]);

        GeometryType geometry = (GeometryType) DataTypeManager.transformValue(
                unknown, DataTypeManager.DefaultDataClasses.GEOMETRY);

        assertEquals(GeometryType.UNKNOWN_SRID, geometry.getSrid());
    }

    @Test
    public void testSerializationPreservesSrid() throws Exception {
        GeographyType unknown = GeographyType.withUnknownSrid(new byte[0]);
        unknown.setReferenceStreamId(null);
        GeographyType unknownCopy = UnitTestUtil.helpSerialize(unknown);
        assertEquals(GeometryType.UNKNOWN_SRID, unknownCopy.getSrid());

        GeographyType legacy = new GeographyType(new byte[0]);
        legacy.setReferenceStreamId(null);
        GeographyType legacyCopy = UnitTestUtil.helpSerialize(legacy);
        assertEquals(GeographyType.DEFAULT_SRID, legacyCopy.getSrid());
    }
}
