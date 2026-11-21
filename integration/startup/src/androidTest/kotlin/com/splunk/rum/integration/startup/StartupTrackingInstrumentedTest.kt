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

package com.splunk.rum.integration.startup

import android.app.Application
import android.app.Instrumentation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.processor.SessionIdSpanProcessor
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.session.SessionSnapshot
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import com.splunk.rum.startup.ApplicationStartupTimekeeper
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for startup tracking.
 *
 * Startup callbacks are seeded directly because AndroidJUnitRunner starts the target process in the
 * background, which causes its first test activity to be classified as a warm start. This mirrors
 * the direct detector seeding in NavigationTrackingInstrumentedTest while exercising the real
 * startup listener, deferred cache, module installation, OpenTelemetry SDK, and span exporter.
 *
 * The whole sequence runs in a single test because startup tracking maintains process-wide state:
 * the install span is reported at most once per process, so a second test method would assert
 * against a startup module that has already reported.
 */
@RunWith(AndroidJUnit4::class)
class StartupTrackingInstrumentedTest {

    private val exportedSpans = CopyOnWriteArrayList<SpanData>()

    private val collectingExporter = object : SpanExporter {
        override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
            exportedSpans.addAll(spans)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()
        override fun shutdown() = CompletableResultCode.ofSuccess()
    }

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private lateinit var tracerProvider: SdkTracerProvider

    private val sessionManager = object : ISplunkSessionManager {
        override val allowBackgroundSessionCreation = false
        var currentId: String? = null
        override val sessionId: String? get() = currentId
        override val sessionStart: Long? get() = if (currentId == null) null else 1L
        override val sessionLastActivity: Long? get() = if (currentId == null) null else 1L
        override val sessionSnapshot: SessionSnapshot?
            get() = currentId?.let { SessionSnapshot(it, 1L, 1L) }
        override var previousSessionId: String? = null
        override val sessionListeners = mutableSetOf<SplunkSessionManager.SessionListener>()
        override fun install(application: Application) {
            // A started Activity was already observed before this late SDK install.
            currentId = "session-A"
            sessionListeners.forEach { it.onSessionChanged("session-A", System.currentTimeMillis()) }
        }
        override fun trackSessionActivity() = Unit
        override fun reset() = Unit
        override fun sessionId(timestamp: Long): String? = currentId
    }

    @Before
    fun setUp() {
        exportedSpans.clear()
        tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SessionIdSpanProcessor(sessionManager))
            .addSpanProcessor(SimpleSpanProcessor.create(collectingExporter))
            .build()
        SplunkOpenTelemetrySdk.instance = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build()
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
        AgentIntegration.installStartTimestamp = null
        AgentIntegration.installStartElapsed = null
        AgentIntegration.installEndElapsed = null
        AgentIntegration.onInstallTimingComplete = null
        tracerProvider.shutdown().join(5, TimeUnit.SECONDS)
    }

    @Test
    fun coldStartDefersInstallSpanUntilInstallTimingCompletes() {
        val installStart = System.currentTimeMillis() - 150
        val installStartElapsed = android.os.SystemClock.elapsedRealtime() - 150
        val installEndElapsed = android.os.SystemClock.elapsedRealtime() - 50

        // SplunkRum.install() records the start timestamps before installing the agent, and only
        // records installEndElapsed once it returns.
        AgentIntegration.installStartTimestamp = installStart
        AgentIntegration.installStartElapsed = installStartElapsed

        reportColdStart()

        assertTrue("Cold start should be deferred until agent installation", appStartSpans().isEmpty())

        val application = instrumentation.targetContext.applicationContext as Application
        AgentIntegration.instance.install(
            application,
            SplunkOpenTelemetrySdk.instance!!,
            sessionManager,
            listOf(StartupModuleConfiguration()),
            Attributes.empty()
        )

        waitForIdle()

        val coldStart = appStartSpans().single()
        assertEquals(RumConstants.APP_START_TYPE_COLD, coldStart.startType())
        assertEquals("session-A", coldStart.attributes.get(SESSION_ID_KEY))
        assertNull(coldStart.attributes.get(PREVIOUS_SESSION_ID_KEY))

        assertTrue(
            "Install span should not be emitted before install timing completes",
            installSpans().isEmpty()
        )
        assertNotNull(
            "Install span emission should be deferred until install timing completes",
            AgentIntegration.onInstallTimingComplete
        )

        sessionManager.currentId = "session-B"
        sessionManager.previousSessionId = "session-A"
        sessionManager.sessionListeners.forEach { it.onSessionChanged("session-B", System.currentTimeMillis()) }

        completeInstallTiming(installEndElapsed)
        waitForIdle()

        assertNull(
            "Deferred callback should be cleared once invoked",
            AgentIntegration.onInstallTimingComplete
        )

        val installSpans = installSpans()
        assertEquals("Install span should only be reported once", 1, installSpans.size)

        val installSpan = installSpans.single()
        assertEquals("session-A", installSpan.attributes.get(SESSION_ID_KEY))
        assertNull(installSpan.attributes.get(PREVIOUS_SESSION_ID_KEY))
        assertEquals(
            "SplunkRum.install should be child of AppStart",
            coldStart.spanId,
            installSpan.parentSpanId
        )

        val spanStartMillis = TimeUnit.NANOSECONDS.toMillis(installSpan.startEpochNanos)
        assertEquals(
            "Install span should use the early install start timestamp",
            installStart,
            spanStartMillis
        )

        val expectedEndMillis = installStart + (installEndElapsed - installStartElapsed)
        val spanEndMillis = TimeUnit.NANOSECONDS.toMillis(installSpan.endEpochNanos)
        assertEquals(
            "Install span should use the computed install end timestamp",
            expectedEndMillis,
            spanEndMillis
        )

        assertTrue(
            "Install span should have per-module events",
            installSpan.events.isNotEmpty()
        )
        assertTrue(
            installSpan.attributes.get(RumConstants.APP_START_CONFIG_SETTINGS_KEY)
                ?.contains("allowBackgroundSessionCreation:false") == true
        )

        AppStateObserver.listeners.forEach { it.onAppBackgrounded() }
        AppStateObserver.listeners.forEach { it.onAppForegrounded() }
        sessionManager.currentId = "session-C"
        sessionManager.previousSessionId = "session-B"
        sessionManager.sessionListeners.forEach { it.onSessionChanged("session-C", System.currentTimeMillis()) }
        reportHotStart()
        waitForIdle()

        val appStarts = appStartSpans()
        assertEquals("Expected cold and hot AppStart spans", 2, appStarts.size)
        assertEquals(
            listOf(RumConstants.APP_START_TYPE_COLD, RumConstants.APP_START_TYPE_HOT),
            appStarts.map { it.startType() }
        )
        assertEquals(listOf("session-A", "session-B"), appStarts.map { it.attributes.get(SESSION_ID_KEY) })
        assertEquals(listOf(null, "session-A"), appStarts.map { it.attributes.get(PREVIOUS_SESSION_ID_KEY) })

        AppStateObserver.listeners.forEach { it.onAppBackgrounded() }
        sessionManager.currentId = null
        sessionManager.previousSessionId = null
        reportHotStart()
        waitForIdle()
        assertEquals("No App Start should be emitted without a session", 2, appStartSpans().size)

        AppStateObserver.listeners.forEach { it.onAppForegrounded() }
        sessionManager.currentId = "session-D"
        sessionManager.sessionListeners.forEach { it.onSessionChanged("session-D", System.currentTimeMillis()) }
        reportWarmStart()
        waitForIdle()

        assertEquals(
            listOf("session-A", "session-B", "session-D"),
            appStartSpans().map { it.attributes.get(SESSION_ID_KEY) }
        )

        assertEquals("Install span should still only be reported once after hot start", 1, installSpans().size)
    }

    /** Mirrors the tail of [com.splunk.rum.integration.agent.api.SplunkRum.install]. */
    private fun completeInstallTiming(installEndElapsed: Long) {
        AgentIntegration.installEndElapsed = installEndElapsed
        AgentIntegration.onInstallTimingComplete?.invoke()
        AgentIntegration.onInstallTimingComplete = null
    }

    private fun reportColdStart() {
        val endTimestamp = System.currentTimeMillis()
        val startTimestamp = endTimestamp - START_DURATION_MILLIS
        ApplicationStartupTimekeeper.listeners.forEach {
            it.onColdStarted(startTimestamp, endTimestamp, START_DURATION_MILLIS)
        }
    }

    private fun reportHotStart() {
        val endTimestamp = System.currentTimeMillis()
        val startTimestamp = endTimestamp - START_DURATION_MILLIS
        ApplicationStartupTimekeeper.listeners.forEach {
            it.onHotStarted(startTimestamp, endTimestamp, START_DURATION_MILLIS)
        }
    }

    private fun reportWarmStart() {
        val endTimestamp = System.currentTimeMillis()
        val startTimestamp = endTimestamp - START_DURATION_MILLIS
        ApplicationStartupTimekeeper.listeners.forEach {
            it.onWarmStarted(startTimestamp, endTimestamp, START_DURATION_MILLIS)
        }
    }

    private fun waitForIdle() {
        instrumentation.waitForIdleSync()
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { latch.countDown() }
        assertTrue("Main thread did not become idle", latch.await(5, TimeUnit.SECONDS))
    }

    private fun appStartSpans(): List<SpanData> = exportedSpans.filter {
        it.name == RumConstants.APP_START_SPAN_NAME
    }

    private fun installSpans(): List<SpanData> = exportedSpans.filter {
        it.name == RumConstants.APP_START_INSTALL_SPAN_NAME
    }

    private fun SpanData.startType(): String? = attributes.get(RumConstants.APP_START_TYPE_KEY)

    private companion object {
        const val START_DURATION_MILLIS = 100L
    }
}
