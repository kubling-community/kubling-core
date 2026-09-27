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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("nls")
public class TestTracing {

    private InMemorySpanExporter exporter;
    private SdkTracerProvider provider;
    private Tracer tracer;

    @BeforeEach
    void setUp() {
        exporter = InMemorySpanExporter.create();
        provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        GlobalOpenTelemetry.resetForTest(); // optional, ensures clean global state
        GlobalOpenTelemetry.set(openTelemetry);

        tracer = openTelemetry.getTracer("com.kubling.test");
    }

    @AfterEach
    void tearDown() {
        provider.shutdown();
    }

    @Test
    void testSpanContextInjection() {
        assertNull(GlobalTracerInjector.getSpanContext(tracer));

        var span = tracer.spanBuilder("x").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            String json = GlobalTracerInjector.getSpanContext(tracer);
            assertNotNull(json);
            assertTrue(json.contains("traceparent"), "Expected W3C trace context key");
        } finally {
            span.end();
        }

        List<?> finishedSpans = exporter.getFinishedSpanItems();
        assertEquals(1, finishedSpans.size());
    }
}
