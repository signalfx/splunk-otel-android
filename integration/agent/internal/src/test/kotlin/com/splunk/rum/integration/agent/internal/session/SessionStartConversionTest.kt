/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.agent.internal.session

import com.splunk.rum.agent.common.otel.OpenTelemetryInitializer
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.LOG_EVENT_NAME_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.RUM_TRACER_NAME
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.integration.agent.internal.RumConstants
import com.splunk.rum.integration.agent.internal.processor.SessionIdSpanProcessor
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SessionStartConversionTest {

    @Test
    fun `conversion preserves absent ancestry on a delayed SDK start`() = withSdk { sdk, collector ->
        val emitter = SessionStartEmitter()
        emitter.attach(sdk.sdkLoggerProvider.get(RUM_TRACER_NAME))
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)
        emitter.emitIfPending("session-a")
        sdk.sdkLoggerProvider.get(RUM_TRACER_NAME).logRecordBuilder()
            .setAttribute(LOG_EVENT_NAME_KEY, RumConstants.SESSION_START_EVENT_NAME)
            .emit()

        flushLogs(sdk)

        assertEquals(2, collector.spans.size)
        val sdkStart = collector.spans.single { it.attributes.get(SESSION_ID_KEY) == "session-a" }
        assertNull(sdkStart.attributes.get(PREVIOUS_SESSION_ID_KEY))
        assertEquals(TimeUnit.MILLISECONDS.toNanos(1_000L), sdkStart.startEpochNanos)
        val customLog = collector.spans.single { it.attributes.get(SESSION_ID_KEY) == "session-b" }
        assertEquals("session-a", customLog.attributes.get(PREVIOUS_SESSION_ID_KEY))
        collector.spans.forEach { assertStartSpan(it) }
    }

    @Test
    fun `conversion preserves captured nonnull SDK start ancestry`() = withSdk { sdk, collector ->
        val emitter = SessionStartEmitter()
        emitter.attach(sdk.sdkLoggerProvider.get(RUM_TRACER_NAME))
        emitter.onSessionCreated("session-a", previousSessionId = "session-before", timestamp = 1_000L)
        emitter.emitIfPending("session-a")

        flushLogs(sdk)

        val span = collector.spans.single()
        assertEquals("session-a", span.attributes.get(SESSION_ID_KEY))
        assertEquals("session-before", span.attributes.get(PREVIOUS_SESSION_ID_KEY))
        assertStartSpan(span)
    }

    @Test
    fun `unmarked custom session start log uses current session ancestry`() = withSdk { sdk, collector ->
        sdk.sdkLoggerProvider.get(RUM_TRACER_NAME).logRecordBuilder()
            .setAttribute(LOG_EVENT_NAME_KEY, RumConstants.SESSION_START_EVENT_NAME)
            .setAttribute(PREVIOUS_SESSION_ID_KEY, "customer-value")
            .emit()

        flushLogs(sdk)

        val span = collector.spans.single()
        assertEquals("session-b", span.attributes.get(SESSION_ID_KEY))
        assertEquals("session-a", span.attributes.get(PREVIOUS_SESSION_ID_KEY))
        assertStartSpan(span)
    }

    private fun flushLogs(sdk: OpenTelemetrySdk) {
        val result = sdk.sdkLoggerProvider.forceFlush().join(5, TimeUnit.SECONDS)
        assertTrue("Log conversion should complete", result.isSuccess)
    }

    private fun assertStartSpan(span: SpanData) {
        assertEquals(RumConstants.SESSION_START_EVENT_NAME, span.name)
        assertEquals(RUM_TRACER_NAME, span.instrumentationScopeInfo.name)
    }

    private fun withSdk(test: (OpenTelemetrySdk, CollectingSpanProcessor) -> Unit) {
        val previousSdk = SplunkOpenTelemetrySdk.instance
        val previousListeners = AppStateObserver.listeners.toList()
        val sessionManager = mock(ISplunkSessionManager::class.java)
        `when`(sessionManager.sessionId).thenReturn("session-b")
        `when`(sessionManager.previousSessionId).thenReturn("session-a")
        val collector = CollectingSpanProcessor()
        var sdk: OpenTelemetrySdk? = null

        try {
            val initializedSdk = OpenTelemetryInitializer(
                RuntimeEnvironment.getApplication(),
                mock(IAgentStorage::class.java),
                deferredUntilForeground = true
            ).addSpanProcessor(SessionIdSpanProcessor(sessionManager))
                .addSpanProcessor(collector)
                .build()
            sdk = initializedSdk
            test(initializedSdk, collector)
        } finally {
            try {
                sdk?.close()
            } finally {
                SplunkOpenTelemetrySdk.instance = previousSdk
                AppStateObserver.listeners.removeAll { listener -> previousListeners.none { it === listener } }
            }
        }
    }

    private class CollectingSpanProcessor : SpanProcessor {
        val spans = CopyOnWriteArrayList<SpanData>()

        override fun onStart(parentContext: Context, span: ReadWriteSpan) = Unit

        override fun isStartRequired(): Boolean = false

        override fun onEnd(span: ReadableSpan) {
            spans += span.toSpanData()
        }

        override fun isEndRequired(): Boolean = true
    }
}
