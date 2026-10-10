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

public class NumberToBooleanTransform extends Transform {

    private final Comparable<?> falseVal;
    private final Class<?> sourceType;

    public NumberToBooleanTransform(Comparable<?> falseVal) {
        this.falseVal = falseVal;
        this.sourceType = falseVal.getClass();
    }

    @Override
    public Class<?> getSourceType() {
        return sourceType;
    }

    @Override
    public Class<?> getTargetType() {
        return DataTypeManager.DefaultDataClasses.BOOLEAN;
    }

    @Override
    public Object transformDirect(Object value) throws TransformationException {
        if (NumberToNumberTransform.compare(falseVal, value) == 0) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    public boolean isExplicit() {
        return true;
    }

}
