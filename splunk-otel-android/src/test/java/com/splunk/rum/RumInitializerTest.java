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

package com.splunk.rum;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import com.google.common.base.Strings;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetwork;
import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RumInitializerTest {

    @Mock Looper mainLooper;
    @Mock Application application;
    @Mock Context context;

    @Test
    void initializationSpan() {
        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setRealm("dev")
                        .setApplicationName("testApp")
                        .setRumAccessToken("accessToken");

        when(application.getApplicationContext()).thenReturn(context);

        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        AppStartupTimer startupTimer = new AppStartupTimer();
        RumInitializer testInitializer =
                new RumInitializer(splunkRumBuilder, application, startupTimer) {
                    @Override
                    SpanExporter buildFilteringExporter(CurrentNetworkProvider connectionUtil) {
                        return testExporter;
                    }
                };
        SplunkRum splunkRum =
                testInitializer.initialize(
                        app -> mock(CurrentNetworkProvider.class, RETURNS_DEEP_STUBS), mainLooper);
        startupTimer.runCompletionCallback();
        splunkRum.flushSpans();

        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        SpanData initSpan = spans.get(0);
        assertEquals(
                initSpan.getParentSpanContext(), startupTimer.getStartupSpan().getSpanContext());

        assertEquals("SplunkRum.initialize", initSpan.getName());
        assertEquals("appstart", initSpan.getAttributes().get(SplunkRum.COMPONENT_KEY));
        assertEquals(
                "[debug:false,crashReporting:true,anrReporting:true,slowRenderingDetector:true,networkMonitor:true]",
                initSpan.getAttributes().get(stringKey("config_settings")));

        List<EventData> events = initSpan.getEvents();
        assertTrue(events.size() > 0);
        checkEventExists(events, "connectionUtilInitialized");
        checkEventExists(events, "exporterInitialized");
        checkEventExists(events, "tracerProviderInitialized");
        checkEventExists(events, "activityLifecycleCallbacksInitialized");
        checkEventExists(events, "crashReportingInitialized");
        checkEventExists(events, "anrMonitorInitialized");
        checkEventExists(events, "networkMonitorInitialized");
    }

    private void checkEventExists(List<EventData> events, String eventName) {
        assertTrue(
                events.stream().map(EventData::getName).anyMatch(name -> name.equals(eventName)),
                "Event with name " + eventName + " not found");
    }

    @Test
    void spanLimitsAreConfigured() {
        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setRealm("dev")
                        .setApplicationName("testApp")
                        .setRumAccessToken("accessToken");

        when(application.getApplicationContext()).thenReturn(context);

        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        AppStartupTimer startupTimer = new AppStartupTimer();
        RumInitializer testInitializer =
                new RumInitializer(splunkRumBuilder, application, startupTimer) {
                    @Override
                    SpanExporter buildFilteringExporter(CurrentNetworkProvider connectionUtil) {
                        return testExporter;
                    }
                };
        SplunkRum splunkRum =
                testInitializer.initialize(
                        app -> mock(CurrentNetworkProvider.class, RETURNS_DEEP_STUBS), mainLooper);
        splunkRum.flushSpans();

        testExporter.reset();

        AttributeKey<String> longAttributeKey = stringKey("longAttribute");
        splunkRum.addRumEvent(
                "testEvent",
                Attributes.of(
                        longAttributeKey,
                        Strings.repeat("a", RumInitializer.MAX_ATTRIBUTE_LENGTH + 1)));

        splunkRum.flushSpans();
        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());

        SpanData eventSpan = spans.get(0);
        assertEquals("testEvent", eventSpan.getName());
        String truncatedValue = eventSpan.getAttributes().get(longAttributeKey);
        assertEquals(Strings.repeat("a", RumInitializer.MAX_ATTRIBUTE_LENGTH), truncatedValue);
    }

    /** Verify that we have buffering in place in our exporter implementation. */
    @Test
    void verifyExporterBuffering() {
        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setRealm("dev")
                        .setApplicationName("testApp")
                        .setRumAccessToken("accessToken");
        AppStartupTimer startupTimer = new AppStartupTimer();
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();

        RumInitializer testInitializer =
                new RumInitializer(splunkRumBuilder, application, startupTimer) {
                    @Override
                    SpanExporter getCoreSpanExporter(String endpoint) {
                        return testExporter;
                    }
                };

        CurrentNetworkProvider currentNetworkProvider = mock(CurrentNetworkProvider.class);
        CurrentNetwork currentNetwork = mock(CurrentNetwork.class);

        when(currentNetworkProvider.refreshNetworkStatus()).thenReturn(currentNetwork);
        when(currentNetwork.isOnline()).thenReturn(false, true);

        long currentTimeNanos = MILLISECONDS.toNanos(System.currentTimeMillis());

        SpanExporter spanExporter = testInitializer.buildFilteringExporter(currentNetworkProvider);
        List<SpanData> batch1 = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            batch1.add(createTestSpan(currentTimeNanos - MINUTES.toNanos(1)));
        }
        // space out the two batches, so they are well under the rate limit
        List<SpanData> batch2 = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            batch2.add(createTestSpan(currentTimeNanos));
        }
        spanExporter.export(batch1);
        spanExporter.export(batch2);

        // we want to verify that everything got exported, including everything buffered while
        // offline.
        assertEquals(198, testExporter.getFinishedSpanItems().size());
    }

    private TestSpanData createTestSpan(long startTimeNanos) {
        return TestSpanData.builder()
                .setName("span")
                .setKind(SpanKind.INTERNAL)
                .setStatus(StatusData.unset())
                .setStartEpochNanos(startTimeNanos)
                .setHasEnded(true)
                .setEndEpochNanos(startTimeNanos)
                .build();
    }

    @Test
    void shouldTranslateExceptionEventsToSpanAttributes() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setRealm("us0")
                        .setRumAccessToken("secret!")
                        .setApplicationName("test");

        when(application.getApplicationContext()).thenReturn(context);

        CurrentNetworkProvider currentNetworkProvider =
                mock(CurrentNetworkProvider.class, RETURNS_DEEP_STUBS);
        when(currentNetworkProvider.refreshNetworkStatus().isOnline()).thenReturn(true);

        AppStartupTimer appStartupTimer = new AppStartupTimer();
        RumInitializer initializer =
                new RumInitializer(splunkRumBuilder, application, appStartupTimer) {
                    @Override
                    SpanExporter getCoreSpanExporter(String endpoint) {
                        return spanExporter;
                    }
                };

        SplunkRum splunkRum = initializer.initialize(app -> currentNetworkProvider, mainLooper);
        appStartupTimer.runCompletionCallback();

        Exception e = new IllegalArgumentException("booom!");
        splunkRum.addRumException(e, Attributes.of(stringKey("attribute"), "oh no!"));
        splunkRum.flushSpans();

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans)
                .satisfiesExactly(
                        span ->
                                OpenTelemetryAssertions.assertThat(span)
                                        .hasName("SplunkRum.initialize"),
                        span ->
                                OpenTelemetryAssertions.assertThat(span)
                                        .hasName("IllegalArgumentException")
                                        .hasAttributesSatisfying(
                                                attributes ->
                                                        OpenTelemetryAssertions.assertThat(
                                                                        attributes)
                                                                .containsEntry(
                                                                        SplunkRum.COMPONENT_KEY,
                                                                        SplunkRum.COMPONENT_ERROR)
                                                                .containsEntry(
                                                                        stringKey("attribute"),
                                                                        "oh no!")
                                                                .containsEntry(
                                                                        SemanticAttributes
                                                                                .EXCEPTION_TYPE,
                                                                        "IllegalArgumentException")
                                                                .containsEntry(
                                                                        SplunkRum.ERROR_TYPE_KEY,
                                                                        "IllegalArgumentException")
                                                                .containsEntry(
                                                                        SemanticAttributes
                                                                                .EXCEPTION_MESSAGE,
                                                                        "booom!")
                                                                .containsEntry(
                                                                        SplunkRum.ERROR_MESSAGE_KEY,
                                                                        "booom!")
                                                                .containsKey(
                                                                        SemanticAttributes
                                                                                .EXCEPTION_STACKTRACE))
                                        .hasEvents(emptyList()));
    }
}
