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

package com.splunk.rum.integration.navigation

import android.app.Instrumentation
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
import com.splunk.rum.integration.navigation.automatic.NavigationEventEmitter
import com.splunk.rum.integration.navigation.automatic.ScreenChangeDetector
import com.splunk.rum.integration.navigation.automatic.callback.NavigationFragmentCallback
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for automatic navigation tracking.
 *
 * These complement the Robolectric-based ScreenChangeDetectorTest by exercising
 * the full fragment callback chain (FragmentLifecycleCallbacks → ScreenChangeDetector →
 * NavigationEventEmitter → log record) with real Android lifecycle callbacks and
 * real Handler.post() timing.
 *
 * Activity state is seeded via direct detector calls (not application-level callbacks)
 * to avoid callback leaking across tests through the shared Application instance.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTrackingInstrumentedTest {

    private val exportedLogs = CopyOnWriteArrayList<LogRecordData>()

    private val collectingExporter = object : LogRecordExporter {
        override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
            exportedLogs.addAll(logs)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()
        override fun shutdown() = CompletableResultCode.ofSuccess()
    }

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        exportedLogs.clear()
        ScreenNameTracker.screenName = GlobalRumConstants.DEFAULT_SCREEN_NAME

        val loggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(collectingExporter))
            .build()
        SplunkOpenTelemetrySdk.instance = OpenTelemetrySdk.builder()
            .setLoggerProvider(loggerProvider)
            .build()
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
    }

    @Test
    fun fragmentNavigationEmitsCorrectEvents() {
        val scenario = ActivityScenario.launch(NavigationTestActivity::class.java)

        scenario.onActivity { activity ->
            val emitter = NavigationEventEmitter()
            emitter.processCachedEvents()
            val detector = ScreenChangeDetector(emitter)

            val fragmentCallback = NavigationFragmentCallback(detector)
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)

            detector.onActivityResumed(activity)

            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, TrackedFragmentA())
                .commitNow()
        }

        waitForIdle()

        val navLogs = navigationLogs()
        assertTrue("Expected at least one navigation event", navLogs.isNotEmpty())
        assertEquals("ScreenA", navLogs.last().screenName())

        scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, TrackedFragmentB())
                .commitNow()
        }

        waitForIdle()

        val updatedLogs = navigationLogs()
        assertTrue("Expected at least two navigation events", updatedLogs.size >= 2)

        val lastEvent = updatedLogs.last()
        assertEquals("ScreenB", lastEvent.screenName())
        assertEquals("ScreenA", lastEvent.lastScreenName())

        scenario.close()
    }

    @Test
    fun ignoredFragmentDoesNotEmitNavigationEvent() {
        val scenario = ActivityScenario.launch(NavigationTestActivity::class.java)

        scenario.onActivity { activity ->
            val emitter = NavigationEventEmitter()
            emitter.processCachedEvents()
            val detector = ScreenChangeDetector(emitter)

            val fragmentCallback = NavigationFragmentCallback(detector)
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)

            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, IgnoredTestFragment())
                .commitNow()
        }

        waitForIdle()

        val navLogs = navigationLogs()
        assertTrue("Ignored fragment should not emit navigation events", navLogs.isEmpty())

        scenario.close()
    }

    @Test
    fun navigatingToIgnoredFragmentDoesNotEmitFalseActivityEvent() {
        val scenario = ActivityScenario.launch(NavigationTestActivity::class.java)

        scenario.onActivity { activity ->
            val emitter = NavigationEventEmitter()
            emitter.processCachedEvents()
            val detector = ScreenChangeDetector(emitter)

            val fragmentCallback = NavigationFragmentCallback(detector)
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)

            detector.onActivityResumed(activity)

            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, TrackedFragmentA())
                .commitNow()
        }

        waitForIdle()

        val initialCount = navigationLogs().size
        assertEquals("ScreenA", navigationLogs().last().screenName())

        scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, IgnoredTestFragment())
                .addToBackStack(null)
                .commitNow()
        }

        waitForIdle()

        val afterIgnored = navigationLogs()
        assertEquals(
            "Navigating to ignored fragment should not produce additional events",
            initialCount,
            afterIgnored.size
        )
        assertEquals(
            "Last emitted screen should still be ScreenA",
            "ScreenA",
            afterIgnored.last().screenName()
        )

        scenario.close()
    }

    @Test
    fun returningFromIgnoredFragmentToTrackedFragmentDeduplicates() {
        val scenario = ActivityScenario.launch(NavigationTestActivity::class.java)

        scenario.onActivity { activity ->
            val emitter = NavigationEventEmitter()
            emitter.processCachedEvents()
            val detector = ScreenChangeDetector(emitter)

            val fragmentCallback = NavigationFragmentCallback(detector)
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)

            detector.onActivityResumed(activity)

            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, TrackedFragmentA())
                .commitNow()
        }

        waitForIdle()

        val countAfterA = navigationLogs().size

        scenario.onActivity { activity ->
            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, IgnoredTestFragment())
                .addToBackStack(null)
                .commitNow()
        }

        waitForIdle()

        scenario.onActivity { activity ->
            activity.supportFragmentManager.popBackStackImmediate()
        }

        waitForIdle()

        val finalLogs = navigationLogs()
        assertEquals(
            "Returning from ignored fragment to ScreenA should not emit a duplicate",
            countAfterA,
            finalLogs.size
        )
        assertEquals("ScreenA", finalLogs.last().screenName())

        scenario.close()
    }

    @Test
    fun fragmentTakesPrecedenceOverActivityOnResume() {
        val scenario = ActivityScenario.launch(NavigationTestActivity::class.java)

        scenario.onActivity { activity ->
            val emitter = NavigationEventEmitter()
            emitter.processCachedEvents()
            val detector = ScreenChangeDetector(emitter)

            val fragmentCallback = NavigationFragmentCallback(detector)
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)

            activity.supportFragmentManager.beginTransaction()
                .replace(NavigationTestActivity.CONTAINER_ID, TrackedFragmentA())
                .commitNow()

            detector.onActivityResumed(activity)
        }

        waitForIdle()

        val navLogs = navigationLogs()
        assertEquals(
            "Fragment should take precedence; first event should be ScreenA not TestActivity",
            "ScreenA",
            navLogs.first().screenName()
        )

        scenario.close()
    }

    private fun waitForIdle() {
        instrumentation.waitForIdleSync()
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { latch.countDown() }
        latch.await(5, TimeUnit.SECONDS)
    }

    private fun navigationLogs(): List<LogRecordData> = exportedLogs.filter {
        it.attributes.get(GlobalRumConstants.LOG_EVENT_NAME_KEY) ==
            GlobalRumConstants.NAVIGATION_EVENT_NAME
    }

    private fun LogRecordData.screenName(): String? = attributes.get(GlobalRumConstants.SCREEN_NAME_KEY)

    private fun LogRecordData.lastScreenName(): String? = attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY)
}
