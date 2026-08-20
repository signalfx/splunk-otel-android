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
import android.os.Handler
import android.os.Looper
import com.splunk.rum.agent.common.utils.extensions.isStartedInForeground
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.instrumentation.anr.internal.extractor.AnrAttributesExtractor
import io.opentelemetry.api.OpenTelemetry
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Entry point for installing ANR (application not responding) detection.
 *
 * Register any additional [AnrAttributesExtractor]s via [addAttributesExtractor] and optionally
 * configure the detection threshold via [setThreshold] before calling [install]. The threshold
 * specifies the wall-clock duration the main thread must be unresponsive before an ANR is reported.
 * Detection is foreground-only and backed by a daemon watchdog thread that is cancelled whenever the
 * app is backgrounded.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class AnrReporterInstrumentation {

    private val additionalExtractors = mutableListOf<AnrAttributesExtractor>()

    @Suppress("NewApi") // Duration requires API 26 or core library desugaring
    private var thresholdNs: Long = AnrWatcher.DEFAULT_THRESHOLD_NS

    /** Adds an [AnrAttributesExtractor] that enriches emitted ANR events. */
    fun addAttributesExtractor(extractor: AnrAttributesExtractor): AnrReporterInstrumentation {
        additionalExtractors.add(extractor)
        return this
    }

    /**
     * Sets the ANR detection threshold. An ANR is reported after the main thread is unresponsive
     * for [threshold] wall-clock time.
     */
    @Suppress("NewApi") // Duration requires API 26 or core library desugaring
    fun setThreshold(threshold: Duration): AnrReporterInstrumentation {
        if (threshold.isZero || threshold.isNegative) {
            Logger.w(TAG, "Invalid threshold ($threshold), using default threshold")
            return this
        }
        thresholdNs = try {
            threshold.toNanos()
        } catch (_: ArithmeticException) {
            Logger.w(TAG, "Threshold ($threshold) overflows nanosecond range, using default threshold")
            AnrWatcher.DEFAULT_THRESHOLD_NS
        }
        return this
    }

    /** Installs the ANR watchdog and starts foreground-only detection. */
    fun install(application: Application, openTelemetry: OpenTelemetry) {
        val reporter = AnrReporter(openTelemetry, additionalExtractors.toList())

        val mainLooper = Looper.getMainLooper()
        val watcher = AnrWatcher(
            Handler(mainLooper),
            mainLooper.thread,
            reporter::report,
            thresholdNs = thresholdNs
        )

        val watchdogScheduler = Executors.newScheduledThreadPool(1, daemonThreadFactory())

        val toggler = AnrDetectorToggler(watcher, watchdogScheduler)
        AppStateObserver.listeners += toggler
        AppStateObserver.attach(application)

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
        private const val TAG = "AnrReporterInstrumentation"
        private const val WATCHDOG_THREAD_NAME = "splunk-anr-watcher"
    }
}
