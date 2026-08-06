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

import android.os.Looper
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ComposeNavigationTrackerTest {

    private val exportedLogs = mutableListOf<LogRecordData>()
    private lateinit var emitter: NavigationEventEmitter
    private lateinit var detector: ScreenChangeDetector
    private lateinit var tracker: ComposeNavigationTracker

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
        SplunkOpenTelemetrySdk.instance = OpenTelemetrySdk.builder()
            .setLoggerProvider(loggerProvider)
            .build()

        emitter = NavigationEventEmitter()
        emitter.processCachedEvents()
        detector = ScreenChangeDetector(emitter)
        tracker = ComposeNavigationTracker(screenChangeDetector = detector, processor = null)
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
    }

    @Test
    fun `destination change with route emits navigation event`() {
        val navController = mock(NavController::class.java)
        tracker.register(navController)

        val destination = createDestination(route = "home!")
        simulateDestinationChanged(navController, destination)

        assertEquals(1, exportedLogs.size)
        assertEquals("home!", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
    }

    @Test
    fun `LinkageError on route access disables tracking without crash`() {
        val navController = mock(NavController::class.java)
        tracker.register(navController)

        val destination = mock(NavDestination::class.java)
        `when`(destination.navigatorName).thenReturn("composable")
        `when`(destination.route).thenThrow(NoSuchMethodError("getRoute"))

        simulateDestinationChanged(navController, destination)

        assertTrue("Should not crash, events should be empty", exportedLogs.isEmpty())
    }

    @Test
    fun `subsequent destinations are skipped after LinkageError`() {
        val navController = mock(NavController::class.java)
        tracker.register(navController)

        val badDestination = mock(NavDestination::class.java)
        `when`(badDestination.navigatorName).thenReturn("composable")
        `when`(badDestination.route).thenThrow(NoSuchMethodError("getRoute"))
        simulateDestinationChanged(navController, badDestination)

        val goodDestination = createDestination(route = "settings")
        simulateDestinationChanged(navController, goodDestination)

        assertTrue("No events should emit after LinkageError disabled tracking", exportedLogs.isEmpty())
    }

    @Test
    fun `IncompatibleClassChangeError is also caught`() {
        val navController = mock(NavController::class.java)
        tracker.register(navController)

        val destination = mock(NavDestination::class.java)
        `when`(destination.navigatorName).thenReturn("composable")
        `when`(destination.route).thenThrow(IncompatibleClassChangeError("test"))

        simulateDestinationChanged(navController, destination)

        assertTrue("Should not crash", exportedLogs.isEmpty())
    }

    private fun createDestination(route: String): NavDestination {
        val destination = mock(NavDestination::class.java)
        `when`(destination.navigatorName).thenReturn("composable")
        `when`(destination.route).thenReturn(route)
        `when`(destination.parent).thenReturn(null)
        return destination
    }

    private fun simulateDestinationChanged(navController: NavController, destination: NavDestination) {
        val listenerField = tracker.javaClass.getDeclaredField("activeListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(tracker) as? NavController.OnDestinationChangedListener
        listener?.onDestinationChanged(navController, destination, null)
        shadowOf(Looper.getMainLooper()).idle()
    }
}
