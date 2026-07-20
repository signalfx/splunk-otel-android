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

package com.splunk.rum.instrumentation.anr

import com.splunk.android.common.utils.AppStateObserver
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Enables ANR detection only while the app is in the foreground: the [anrWatcher] is scheduled when
 * the app is foregrounded and cancelled when it is backgrounded, so the watchdog never runs in the
 * background where the OS does not raise ANRs.
 */
internal class AnrDetectorToggler(private val anrWatcher: Runnable, private val scheduler: ScheduledExecutorService) :
    AppStateObserver.Listener {

    private var future: ScheduledFuture<*>? = null

    override fun onAppForegrounded() {
        if (future == null) {
            future = scheduler.scheduleWithFixedDelay(
                anrWatcher,
                POLL_INTERVAL_SECONDS,
                POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            )
        }
    }

    override fun onAppBackgrounded() {
        future?.cancel(true)
        future = null
    }

    override fun onAppStarted() = Unit

    override fun onAppClosed() = onAppBackgrounded()

    private companion object {
        private const val POLL_INTERVAL_SECONDS = 1L
    }
}
