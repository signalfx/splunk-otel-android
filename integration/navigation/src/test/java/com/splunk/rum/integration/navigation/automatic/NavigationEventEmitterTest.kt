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

package com.splunk.rum.integration.navigation.automatic

import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
import com.splunk.rum.integration.navigation.RumConstant
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationEventEmitterTest {

    private val exportedLogs = mutableListOf<LogRecordData>()

    private val collectingExporter = object : LogRecordExporter {
        override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
            exportedLogs.addAll(logs)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()
        override fun shutdown() = CompletableResultCode.ofSuccess()
    }

    @Before
    fun setUp() {
        exportedLogs.clear()
        ScreenNameTracker.screenName = GlobalRumConstants.DEFAULT_SCREEN_NAME

        val loggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(collectingExporter))
            .build()

        val sdk = OpenTelemetrySdk.builder()
            .setLoggerProvider(loggerProvider)
            .build()

        SplunkOpenTelemetrySdk.instance = sdk
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
    }

    @Test
    fun `events are cached before processCachedEvents is called`() {
        val emitter = NavigationEventEmitter()

        emitter.emitNavigationEvent("Menu")
        emitter.emitNavigationEvent("CrashReportsFragment")

        assertEquals("CrashReportsFragment", ScreenNameTracker.screenName)
        assertTrue("Events should be cached, not exported", exportedLogs.isEmpty())
    }

    @Test
    fun `processCachedEvents drains and emits all cached events`() {
        val emitter = NavigationEventEmitter()

        emitter.emitNavigationEvent("Menu")
        emitter.emitNavigationEvent("CrashReportsFragment")
        emitter.processCachedEvents()

        assertEquals("CrashReportsFragment", ScreenNameTracker.screenName)
        assertEquals(2, exportedLogs.size)
    }

    @Test
    fun `events emit directly after processCachedEvents`() {
        val emitter = NavigationEventEmitter()
        emitter.processCachedEvents()

        ScreenNameTracker.screenName = "Menu"
        emitter.emitNavigationEvent("OkHttpFragment")

        assertEquals("OkHttpFragment", ScreenNameTracker.screenName)
        assertEquals(1, exportedLogs.size)
    }

    @Test
    fun `emitted log record contains correct event name and component`() {
        val emitter = NavigationEventEmitter()
        emitter.processCachedEvents()

        emitter.emitNavigationEvent("Menu")

        val log = exportedLogs.single()
        assertEquals(
            RumConstant.NAVIGATION_LOG_EVENT_NAME,
            log.attributes.get(GlobalRumConstants.LOG_EVENT_NAME_KEY)
        )
        assertEquals(
            RumConstant.COMPONENT_NAVIGATION,
            log.attributes.get(GlobalRumConstants.COMPONENT_KEY)
        )
    }

    @Test
    fun `emitted log record contains screen name`() {
        val emitter = NavigationEventEmitter()
        emitter.processCachedEvents()

        ScreenNameTracker.screenName = "Menu"
        emitter.emitNavigationEvent("CustomTrackingFragment")

        val log = exportedLogs.single()
        assertEquals("CustomTrackingFragment", log.attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
    }

    @Test
    fun `emitted log record contains last screen name from ScreenNameTracker`() {
        val emitter = NavigationEventEmitter()
        emitter.processCachedEvents()

        ScreenNameTracker.screenName = "Menu"
        emitter.emitNavigationEvent("CrashReportsFragment")

        val log = exportedLogs.single()
        assertEquals("Menu", log.attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `emitted log record includes unknown as last screen name on first navigation`() {
        val emitter = NavigationEventEmitter()
        emitter.processCachedEvents()

        emitter.emitNavigationEvent("Menu")

        val log = exportedLogs.single()
        assertEquals("unknown", log.attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `setAllAttributes preserves caller-provided attribute types`() {
        val emitter = NavigationEventEmitter()
        emitter.processCachedEvents()

        val attrs = Attributes.builder()
            .put(AttributeKey.stringKey("custom.string"), "value")
            .put(AttributeKey.longKey("custom.long"), 42L)
            .put(AttributeKey.doubleKey("custom.double"), 3.14)
            .put(AttributeKey.booleanKey("custom.bool"), true)
            .build()

        emitter.emitNavigationEvent("GlobalAttributesFragment", attrs)

        val log = exportedLogs.single()
        assertEquals("value", log.attributes.get(AttributeKey.stringKey("custom.string")))
        assertEquals(42L, log.attributes.get(AttributeKey.longKey("custom.long")))
        assertEquals(3.14, log.attributes.get(AttributeKey.doubleKey("custom.double")))
        assertEquals(true, log.attributes.get(AttributeKey.booleanKey("custom.bool")))
    }

    @Test
    fun `cached events preserve their attributes through drain`() {
        val emitter = NavigationEventEmitter()

        val attrs = Attributes.of(AttributeKey.stringKey("section"), "network")
        emitter.emitNavigationEvent("OkHttpFragment", attrs)
        emitter.processCachedEvents()

        val log = exportedLogs.single()
        assertEquals("network", log.attributes.get(AttributeKey.stringKey("section")))
        assertEquals("OkHttpFragment", log.attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
    }

    @Test
    fun `processCachedEvents with empty cache does not emit`() {
        val emitter = NavigationEventEmitter()
        emitter.processCachedEvents()

        assertTrue(exportedLogs.isEmpty())
    }

    @Test
    fun `ScreenNameTracker is updated eagerly during caching and correctly during drain`() {
        val emitter = NavigationEventEmitter()

        emitter.emitNavigationEvent("Menu")
        emitter.emitNavigationEvent("CrashReportsFragment")
        emitter.emitNavigationEvent("Menu")
        emitter.processCachedEvents()

        assertEquals("Menu", ScreenNameTracker.screenName)
        assertEquals("CrashReportsFragment", ScreenNameTracker.lastScreenName)
    }

    @Test
    fun `cached events capture correct previousScreenName chain`() {
        val emitter = NavigationEventEmitter()

        emitter.emitNavigationEvent("Menu")
        emitter.emitNavigationEvent("CrashReportsFragment")
        emitter.processCachedEvents()

        assertEquals(2, exportedLogs.size)
        assertEquals("unknown", exportedLogs[0].attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
        assertEquals("Menu", exportedLogs[1].attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `manual track events before init are cached and drained with correct screen chain`() {
        val emitter = NavigationEventEmitter()

        emitter.emitNavigationEvent("Login")
        emitter.emitNavigationEvent("Dashboard")
        assertTrue(exportedLogs.isEmpty())

        emitter.processCachedEvents()

        assertEquals(2, exportedLogs.size)
        assertEquals("Login", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
        assertEquals("unknown", exportedLogs[0].attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
        assertEquals("Dashboard", exportedLogs[1].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
        assertEquals("Login", exportedLogs[1].attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `manual track events emit directly after cache drain`() {
        val emitter = NavigationEventEmitter()

        emitter.emitNavigationEvent("Login")
        emitter.processCachedEvents()
        assertEquals(1, exportedLogs.size)

        emitter.emitNavigationEvent("Dashboard")

        assertEquals(2, exportedLogs.size)
        assertEquals("Dashboard", exportedLogs[1].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
        assertEquals("Login", exportedLogs[1].attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `clearCache drops cached events without emitting`() {
        val emitter = NavigationEventEmitter()

        emitter.emitNavigationEvent("Menu")
        emitter.emitNavigationEvent("CrashReportsFragment")
        emitter.clearCache()

        assertTrue(exportedLogs.isEmpty())
    }

    @Test
    fun `clearCache marks install complete so future events emit directly`() {
        val emitter = NavigationEventEmitter()

        emitter.clearCache()
        emitter.emitNavigationEvent("Menu")

        assertEquals(1, exportedLogs.size)
        assertEquals("Menu", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
    }
}
