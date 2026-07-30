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

import android.os.Handler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Watches the UI thread for ANRs by posting a [Runnable] to the main thread on every poll. When the
 * main thread fails to respond to [maxMissedPolls] consecutive polls, an ANR is reported via
 * [onAnr] with the main thread's current stack trace.
 *
 * @property pollDurationNs How long to wait for the main thread to respond on each poll.
 * @property maxMissedPolls How many consecutive missed polls trigger an ANR report. With the
 *           default 1-second poll duration the total detection threshold is `maxMissedPolls` seconds.
 */
internal class AnrWatcher @JvmOverloads constructor(
    private val uiHandler: Handler,
    private val mainThread: Thread,
    private val onAnr: (Array<StackTraceElement>) -> Unit,
    private val pollDurationNs: Long = DEFAULT_POLL_DURATION_NS,
    private val maxMissedPolls: Int = DEFAULT_MAX_MISSED_POLLS
) : Runnable {

    private val anrCounter = AtomicInteger()

    override fun run() {
        val response = CountDownLatch(1)
        if (!uiHandler.post { response.countDown() }) {
            // The main thread is probably shutting down. Ignore and return.
            return
        }

        val responded = try {
            response.await(pollDurationNs, TimeUnit.NANOSECONDS)
        } catch (e: InterruptedException) {
            return
        }

        if (responded) {
            anrCounter.set(0)
            return
        }

        if (anrCounter.incrementAndGet() >= maxMissedPolls) {
            onAnr(mainThread.stackTrace)
            // Only report once per ANR window.
            anrCounter.set(0)
        }
    }

    companion object {
        val DEFAULT_POLL_DURATION_NS: Long = TimeUnit.SECONDS.toNanos(1)
        const val DEFAULT_MAX_MISSED_POLLS = 5
    }
}
