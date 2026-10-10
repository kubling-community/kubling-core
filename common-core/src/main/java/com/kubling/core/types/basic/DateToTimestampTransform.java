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

package com.kubling.core.types.basic;

import com.kubling.core.types.Transform;
import com.kubling.core.types.TransformationException;

import java.sql.Timestamp;


public class DateToTimestampTransform extends Transform {

    /**
     * This method transforms a value of the source type into a value
     * of the target type.
     *
     * @param value Incoming value of source type
     * @return Outgoing value of target type
     * @throws TransformationException if value is an incorrect input type or
     *                                 the transformation fails
     */
    public Object transformDirect(Object value) throws TransformationException {
        return new Timestamp(((java.sql.Date) value).getTime());
    }

    /**
     * Type of the incoming value.
     *
     * @return Source type
     */
    public Class<?> getSourceType() {
        return java.sql.Date.class;
    }

    /**
     * Type of the outgoing value.
     *
     * @return Target type
     */
    public Class<?> getTargetType() {
        return Timestamp.class;
    }

}
