/*
 * =====================================================================
 *  Original Work:
 *    Copyright Red Hat, Inc. and/or its affiliates
 *    and other contributors as indicated by the @author tags
 *    and the COPYRIGHT.txt file distributed with this work.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * This file was modified as part of the Kubling project.
 */

package com.kubling.jdbc.tracing;

import com.kubling.core.util.ReflectionHelper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to reflection load logic that is dependent upon the OpenTelemetry library, or provide a dummy implementation
 */
public class TracingHelper {

    public interface Injector {
        String getSpanContext();
    }

    private static final Logger logger = Logger.getLogger("com.kubling.jdbc");

    private static Injector INJECTOR;

    public static String getSpanContext() {
        if (INJECTOR == null) {
            try {
                INJECTOR = (Injector) ReflectionHelper
                        .create("com.kubling.jdbc.tracing.GlobalTracerInjector", null, TracingHelper.class.getClassLoader());
            } catch (Throwable e) { //must catch both Error and Exception
                logger.log(Level.FINE, "Unable to load OpenTelemetry libraries, propagation will not be used", e);
            }
            if (INJECTOR == null) {
                INJECTOR = () -> null;
            }
        }
        return INJECTOR.getSpanContext();
    }

}
