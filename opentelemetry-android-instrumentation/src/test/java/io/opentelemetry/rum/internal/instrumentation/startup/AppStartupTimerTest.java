/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal.instrumentation.startup;

import static io.opentelemetry.rum.internal.RumConstants.START_TYPE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AppStartupTimerTest {
    @RegisterExtension final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();
    private Tracer tracer;

    @BeforeEach
    void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

    @Test
    void start_end() {
        AppStartupTimer appStartupTimer = new AppStartupTimer();
        Span startSpan = appStartupTimer.start(tracer);
        assertNotNull(startSpan);
        appStartupTimer.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData spanData = spans.get(0);

        assertEquals("AppStart", spanData.getName());
        assertEquals("cold", spanData.getAttributes().get(START_TYPE_KEY));
    }

    @Test
    void multi_end() {
        AppStartupTimer appStartupTimer = new AppStartupTimer();
        appStartupTimer.start(tracer);
        appStartupTimer.end();
        appStartupTimer.end();

        assertEquals(1, otelTesting.getSpans().size());
    }

    @Test
    void multi_start() {
        AppStartupTimer appStartupTimer = new AppStartupTimer();
        appStartupTimer.start(tracer);
        assertSame(appStartupTimer.start(tracer), appStartupTimer.start(tracer));

        appStartupTimer.end();
        assertEquals(1, otelTesting.getSpans().size());
    }
}
