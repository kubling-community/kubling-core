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
 *
 * This file was modified as part of the Kubling project.
 */

package com.kubling.jdbc.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;

import java.util.HashMap;
import java.util.Map;

/**
 * Uses the OpenTelemetry API to create a JSON string representation of the span context
 * and allows setting a static tracer.
 */
public class GlobalTracerInjector implements TracingHelper.Injector {

    private static Tracer TRACER = GlobalOpenTelemetry.getTracer("com.kubling.jdbc");

    @Override
    public String getSpanContext() {
        return getSpanContext(TRACER);
    }

    protected static String getSpanContext(Tracer tracer) {
        Span span = Span.current();
        if (!span.getSpanContext().isValid()) {
            return null;
        }

        Map<String, String> carrier = new HashMap<>();
        TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
        propagator.inject(Context.current(), carrier, Map::put);

        // Simple JSON serialization
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : carrier.entrySet()) {
            if (!first) json.append(',');
            else first = false;
            json.append('"').append(entry.getKey().replace("\"", "\\\""))
                    .append("\":\"")
                    .append(entry.getValue().replace("\"", "\\\""))
                    .append('"');
        }
        json.append('}');
        return json.toString();
    }

    public static Tracer getTracer() {
        return TRACER;
    }

    public static void setTracer(Tracer tracer) {
        TRACER = tracer;
    }
}

