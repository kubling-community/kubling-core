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

import com.kubling.core.types.DataTypeManager;
import com.kubling.core.types.Transform;
import com.kubling.core.types.TransformationException;

public class BooleanToNumberTransform extends Transform {

    private final Object trueVal;
    private final Object falseVal;
    private final Class<?> targetType;

    public BooleanToNumberTransform(Object trueVal, Object falseVal) {
        this.trueVal = trueVal;
        this.falseVal = falseVal;
        this.targetType = trueVal.getClass();
    }

    @Override
    public Class<?> getSourceType() {
        return DataTypeManager.DefaultDataClasses.BOOLEAN;
    }

    @Override
    public Class<?> getTargetType() {
        return targetType;
    }

    @Override
    public Object transformDirect(Object value) throws TransformationException {
        return value.equals(Boolean.TRUE) ? trueVal : falseVal;
    }

}
