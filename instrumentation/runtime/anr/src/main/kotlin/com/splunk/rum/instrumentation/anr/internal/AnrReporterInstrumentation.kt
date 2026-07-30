/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.anr.internal

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.splunk.android.common.utils.AppStateObserver
import com.splunk.rum.instrumentation.anr.internal.extractor.AnrAttributesExtractor
import com.splunk.rum.utils.extensions.isStartedInForeground
import io.opentelemetry.api.OpenTelemetry
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.math.max

/**
 * Entry point for installing ANR (application not responding) detection.
 *
 * Register any additional [AnrAttributesExtractor]s via [addAttributesExtractor] and optionally
 * configure the detection threshold via [setPollingInterval] before calling [install]. Detection is
 * foreground-only and backed by a daemon watchdog thread that is cancelled whenever the app is
 * backgrounded.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class AnrReporterInstrumentation {

    private val additionalExtractors = mutableListOf<AnrAttributesExtractor>()

    @Suppress("NewApi") // Duration requires API 26 or core library desugaring
    private var maxMissedPolls: Int = AnrWatcher.DEFAULT_MAX_MISSED_POLLS

    /** Adds an [AnrAttributesExtractor] that enriches emitted ANR events. */
    fun addAttributesExtractor(extractor: AnrAttributesExtractor): AnrReporterInstrumentation {
        additionalExtractors.add(extractor)
        return this
    }

    /**
     * Sets the ANR detection threshold. The detector polls the main thread every second; after
     * [pollingInterval] seconds of consecutive unresponsiveness an ANR is reported.
     */
    @Suppress("NewApi") // Duration requires API 26 or core library desugaring
    fun setPollingInterval(pollingInterval: Duration): AnrReporterInstrumentation {
        maxMissedPolls = max(1, pollingInterval.seconds.toInt())
        return this
    }

    /** Installs the ANR watchdog and starts foreground-only detection. */
    fun install(context: Context, openTelemetry: OpenTelemetry) {
        val application = context.applicationContext as Application
        val reporter = AnrReporter(openTelemetry, additionalExtractors.toList())

        val mainLooper = Looper.getMainLooper()
        val watcher = AnrWatcher(
            Handler(mainLooper),
            mainLooper.thread,
            reporter::report,
            maxMissedPolls = maxMissedPolls
        )

        val watchdogScheduler = Executors.newScheduledThreadPool(1, daemonThreadFactory())

        val toggler = AnrDetectorToggler(watcher, watchdogScheduler)
        val observer = AppStateObserver()
        observer.listener = toggler
        observer.attach(application)

        // AppStateObserver only emits onAppForegrounded on a transition. If the app is already in the
        // foreground when we install (e.g. late/hybrid initialization after the first Activity
        // resumed), schedule detection now so the current foreground session is covered.
        if (application.isStartedInForeground) {
            toggler.onAppForegrounded()
        }
    }

    private fun daemonThreadFactory() = ThreadFactory { runnable ->
        Thread(runnable, WATCHDOG_THREAD_NAME).apply { isDaemon = true }
    }

    private companion object {
        private const val WATCHDOG_THREAD_NAME = "splunk-anr-watcher"
    }
}
