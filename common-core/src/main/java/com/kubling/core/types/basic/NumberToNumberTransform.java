/*
 * This file was modified as part of the Kubling project.
 */

package com.kubling.core.types.basic;

import com.kubling.core.CorePlugin;
import com.kubling.core.types.Transform;
import com.kubling.core.types.TransformationException;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class NumberToNumberTransform extends Transform {

    private Class<?> sourceType;
    private Comparable<?> min;
    private Comparable<?> max;

    public NumberToNumberTransform(Number min, Number max, Class<?> sourceType) {
        this.sourceType = sourceType;
        if (sourceType == Short.class) {
            this.min = min.shortValue();
            this.max = max.shortValue();
        } else if (sourceType == Integer.class) {
            this.min = min.intValue();
            this.max = max.intValue();
        } else if (sourceType == Long.class) {
            this.min = min.longValue();
            this.max = max.longValue();
        } else if (sourceType == Float.class) {
            this.min = min.floatValue();
            this.max = max.floatValue();
        } else if (sourceType == Double.class) {
            this.min = min.doubleValue();
            this.max = max.doubleValue();
        } else if (sourceType == BigInteger.class) {
            if (min instanceof Double || min instanceof Float) {
                this.min = BigDecimal.valueOf(min.doubleValue()).toBigInteger();
                this.max = BigDecimal.valueOf(max.doubleValue()).toBigInteger();
            } else {
                this.min = BigInteger.valueOf(min.longValue());
                this.max = BigInteger.valueOf(max.longValue());
            }
        } else if (sourceType == BigDecimal.class) {
            if (min instanceof Double || min instanceof Float) {
                this.min = BigDecimal.valueOf(min.doubleValue());
                this.max = BigDecimal.valueOf(max.doubleValue());
            } else {
                this.min = BigDecimal.valueOf(min.longValue());
                this.max = BigDecimal.valueOf(max.longValue());
            }
        } else if (sourceType == Byte.class) {
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public Class<?> getSourceType() {
        return sourceType;
    }

    protected void checkValueRange(Object value)
            throws TransformationException {
        Comparable<?> comparable = (Comparable<?>) value;
        if (compare(comparable, min) < 0 || compare(comparable, max) > 0) {
              throw new TransformationException(CorePlugin.Event.KBL10058,
                      CorePlugin.Util.gs(CorePlugin.Event.KBL10058, value, getSourceType().getSimpleName(),
                              getTargetType().getSimpleName()));
        }
    }

    @SuppressWarnings("unchecked")
    static int compare(Comparable<?> left, Object right) {
        return ((Comparable<Object>) left).compareTo(right);
    }

}
