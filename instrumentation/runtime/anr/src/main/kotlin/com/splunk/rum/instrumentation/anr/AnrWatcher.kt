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

import android.os.Handler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Watches the UI thread for ANRs by posting a [Runnable] to the main thread on every poll. When the
 * main thread fails to respond to [MAX_MISSED_POLLS] consecutive polls (5 seconds with the default
 * 1-second poll), an ANR is reported via [onAnr] with the main thread's current stack trace.
 *
 * @property pollDurationNs how long to wait for the main thread to respond on each poll; exists for testing.
 */
internal class AnrWatcher @JvmOverloads constructor(
    private val uiHandler: Handler,
    private val mainThread: Thread,
    private val onAnr: (Array<StackTraceElement>) -> Unit,
    private val pollDurationNs: Long = DEFAULT_POLL_DURATION_NS
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

        if (anrCounter.incrementAndGet() >= MAX_MISSED_POLLS) {
            onAnr(mainThread.stackTrace)
            // Only report once per ANR window.
            anrCounter.set(0)
        }
    }

    companion object {
        val DEFAULT_POLL_DURATION_NS: Long = TimeUnit.SECONDS.toNanos(1)
        private const val MAX_MISSED_POLLS = 5
    }
}
