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

import android.app.Activity
import android.os.Looper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
import com.splunk.rum.integration.navigation.NavigationElement
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class ScreenChangeDetectorTest {

    private val exportedLogs = mutableListOf<LogRecordData>()
    private lateinit var emitter: NavigationEventEmitter
    private lateinit var detector: ScreenChangeDetector
    private lateinit var activityController: ActivityController<MainActivity>

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

        activityController = Robolectric.buildActivity(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
    }

    @Test
    fun `fragment resumed emits navigation event immediately`() {
        val fragment = MenuFragment()

        detector.onFragmentResumed(fragment)

        assertEquals(1, exportedLogs.size)
        assertEquals(
            "Menu",
            exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY)
        )
    }

    @Test
    fun `activity resumed emits after handler post`() {
        val activity = activityController.create().get()

        detector.onActivityResumed(activity)

        assertEquals(0, exportedLogs.size)

        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, exportedLogs.size)
        assertEquals(
            "Main",
            exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY)
        )
    }

    @Test
    fun `fragment takes precedence over activity when both resume together`() {
        val activity = activityController.create().get()
        val fragment = MenuFragment()

        detector.onActivityResumed(activity)
        detector.onFragmentResumed(fragment)

        assertEquals(1, exportedLogs.size)
        assertEquals("Menu", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))

        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, exportedLogs.size)
    }

    @Test
    fun `same screen name does not double-emit`() {
        val menu1 = MenuFragment()
        val menu2 = MenuFragment()

        detector.onFragmentResumed(menu1)
        detector.onFragmentResumed(menu2)

        assertEquals(1, exportedLogs.size)
    }

    @Test
    fun `navigation from Menu to CrashReportsFragment tracks last screen name`() {
        val menu = MenuFragment()
        val crashReports = CrashReportsFragment()

        detector.onFragmentResumed(menu)
        detector.onFragmentPaused(menu)
        detector.onFragmentResumed(crashReports)

        assertEquals(2, exportedLogs.size)

        val firstLog = exportedLogs[0]
        assertEquals("Menu", firstLog.attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))

        val secondLog = exportedLogs[1]
        assertEquals("CrashReportsFragment", secondLog.attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
        assertEquals("Menu", secondLog.attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `recordEmittedScreen prevents duplicate emission from automatic tracking`() {
        detector.recordEmittedScreen("OkHttpFragment")

        val fragment = OkHttpFragment()
        detector.onFragmentResumed(fragment)

        assertTrue(exportedLogs.isEmpty())
    }

    @Test
    fun `activity becomes visible screen when fragment is paused`() {
        val activity = activityController.create().get()
        val menu = MenuFragment()

        detector.onActivityResumed(activity)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, exportedLogs.size)

        detector.onFragmentResumed(menu)
        assertEquals(2, exportedLogs.size)
        assertEquals("Menu", exportedLogs[1].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))

        detector.onFragmentPaused(menu)
        detector.onActivityPaused(activity)

        detector.onActivityResumed(activity)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(3, exportedLogs.size)
        assertEquals(
            "Main",
            exportedLogs[2].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY)
        )
    }

    @Test
    fun `dialog fragment resumed does not emit navigation event`() {
        val dialog = TestDialogFragment()

        detector.onFragmentResumed(dialog)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(exportedLogs.isEmpty())
    }

    @Test
    fun `dialog fragment does not affect underlying screen name`() {
        val menu = MenuFragment()
        val dialog = TestDialogFragment()

        detector.onFragmentResumed(menu)
        assertEquals(1, exportedLogs.size)
        assertEquals("Menu", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))

        detector.onFragmentResumed(dialog)
        detector.onFragmentPaused(dialog)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, exportedLogs.size)
    }

    @Test
    fun `fragment removed without replacement falls back to activity`() {
        val activity = activityController.create().get()
        val menu = MenuFragment()

        detector.onActivityResumed(activity)
        detector.onFragmentResumed(menu)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, exportedLogs.size)
        assertEquals("Menu", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))

        detector.onFragmentPaused(menu)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(2, exportedLogs.size)
        assertEquals("Main", exportedLogs[1].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
        assertEquals("Menu", exportedLogs[1].attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `fragment to fragment transition does not emit extra event from deferred pause`() {
        val menu = MenuFragment()
        val crashReports = CrashReportsFragment()

        detector.onFragmentResumed(menu)
        assertEquals(1, exportedLogs.size)

        detector.onFragmentPaused(menu)
        detector.onFragmentResumed(crashReports)
        assertEquals(2, exportedLogs.size)

        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(2, exportedLogs.size)
    }

    @Test
    fun `ignored activity does not emit navigation event`() {
        val activity = activityController.create().get()
        val ignoredActivity = Robolectric.buildActivity(IgnoredActivity::class.java).create().get()

        detector.onActivityResumed(ignoredActivity)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(exportedLogs.isEmpty())
    }

    @Test
    fun `activity to activity transition tracks last screen name`() {
        val mainActivity = activityController.create().get()
        val settingsActivity = Robolectric.buildActivity(SettingsActivity::class.java).create().get()

        detector.onActivityResumed(mainActivity)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, exportedLogs.size)
        assertEquals("Main", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))

        detector.onActivityPaused(mainActivity)
        detector.onActivityResumed(settingsActivity)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(2, exportedLogs.size)
        assertEquals("Settings", exportedLogs[1].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
        assertEquals("Main", exportedLogs[1].attributes.get(GlobalRumConstants.LAST_SCREEN_NAME_KEY))
    }

    @Test
    fun `ignored fragment does not emit navigation event`() {
        val fragment = IgnoredFragment()

        detector.onFragmentResumed(fragment)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(exportedLogs.isEmpty())
    }

    @Test
    fun `ignored fragment does not affect underlying screen name`() {
        val menu = MenuFragment()
        val ignored = IgnoredFragment()

        detector.onFragmentResumed(menu)
        assertEquals(1, exportedLogs.size)

        detector.onFragmentResumed(ignored)
        detector.onFragmentPaused(ignored)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, exportedLogs.size)
        assertEquals("Menu", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
    }

    @Test
    fun `navigating to ignored fragment does not emit spurious activity event`() {
        val activity = activityController.create().get()
        val menu = MenuFragment()
        val ignored = IgnoredFragment()

        detector.onActivityResumed(activity)
        detector.onFragmentResumed(menu)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, exportedLogs.size)
        assertEquals("Menu", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))

        detector.onFragmentPaused(menu)
        detector.onFragmentResumed(ignored)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, exportedLogs.size)
    }

    @Test
    fun `returning from ignored fragment to tracked fragment emits correctly`() {
        val activity = activityController.create().get()
        val menu = MenuFragment()
        val ignored = IgnoredFragment()

        detector.onActivityResumed(activity)
        detector.onFragmentResumed(menu)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, exportedLogs.size)

        detector.onFragmentPaused(menu)
        detector.onFragmentResumed(ignored)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, exportedLogs.size)

        detector.onFragmentPaused(ignored)
        detector.onFragmentResumed(menu)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, exportedLogs.size)
        assertEquals("Menu", exportedLogs[0].attributes.get(GlobalRumConstants.SCREEN_NAME_KEY))
    }

    @NavigationElement(name = "Main")
    class MainActivity : Activity()

    @NavigationElement(name = "Settings")
    class SettingsActivity : Activity()

    @NavigationElement(name = "Menu")
    class MenuFragment : Fragment()

    class CrashReportsFragment : Fragment()

    class OkHttpFragment : Fragment()

    class TestDialogFragment : DialogFragment()

    @NavigationElement(name = "Ignored", isIgnored = true)
    class IgnoredActivity : Activity()

    @NavigationElement(name = "Hidden", isIgnored = true)
    class IgnoredFragment : Fragment()
}
