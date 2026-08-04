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
import com.splunk.rum.agent.common.utils.extensions.isStartedInForeground
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.instrumentation.anr.internal.extractor.AnrAttributesExtractor
import io.opentelemetry.api.OpenTelemetry
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Entry point for installing ANR (application not responding) detection.
 *
 * Register any additional [AnrAttributesExtractor]s via [addAttributesExtractor] before calling
 * [install]. Detection is foreground-only and backed by a daemon watchdog thread that is cancelled
 * whenever the app is backgrounded.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class AnrReporterInstrumentation {

    private val additionalExtractors = mutableListOf<AnrAttributesExtractor>()

    /** Adds an [AnrAttributesExtractor] that enriches emitted ANR events. */
    fun addAttributesExtractor(extractor: AnrAttributesExtractor): AnrReporterInstrumentation {
        additionalExtractors.add(extractor)
        return this
    }

    /** Installs the ANR watchdog and starts foreground-only detection. */
    fun install(context: Context, openTelemetry: OpenTelemetry) {
        val application = context.applicationContext as Application
        val reporter = AnrReporter(openTelemetry, additionalExtractors.toList())

        val mainLooper = Looper.getMainLooper()
        val watcher = AnrWatcher(Handler(mainLooper), mainLooper.thread, reporter::report)

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
        private const val WATCHDOG_THREAD_NAME = "splunk-anr-watcher"
    }
}
