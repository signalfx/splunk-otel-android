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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Watches the UI thread for ANRs by posting a heartbeat [Runnable] to the main thread. When the
 * main thread fails to process the heartbeat within [thresholdNs] wall-clock nanoseconds, an ANR
 * is reported via [onAnr] with the main thread's current stack trace.
 *
 * Only one ANR is reported per stall. A new report is only emitted after the main thread recovers
 * and subsequently stalls again.
 *
 * @property thresholdNs Wall-clock time in nanoseconds that the main thread must be unresponsive
 *           before an ANR is reported.
 * @property clock Monotonic clock source; injectable for testing.
 */
internal class AnrWatcher(
    private val uiHandler: Handler,
    private val mainThread: Thread,
    private val onAnr: (Array<StackTraceElement>) -> Unit,
    private val thresholdNs: Long = DEFAULT_THRESHOLD_NS,
    private val clock: () -> Long = { System.nanoTime() }
) : Runnable {

    private val heartbeatOutstanding = AtomicBoolean(false)
    private val nextReportAtNs = AtomicLong(Long.MAX_VALUE)

    override fun run() {
        val nowNs = clock()

        if (heartbeatOutstanding.compareAndSet(false, true)) {
            val deadline = if (nowNs > Long.MAX_VALUE - thresholdNs) Long.MAX_VALUE else nowNs + thresholdNs
            nextReportAtNs.set(deadline)

            if (!uiHandler.post {
                    if (clock() < nextReportAtNs.get()) {
                        heartbeatOutstanding.set(false)
                    }
                }
            ) {
                heartbeatOutstanding.set(false)
            }
            return
        }

        if (nowNs >= nextReportAtNs.get()) {
            onAnr(mainThread.stackTrace)
            nextReportAtNs.set(Long.MAX_VALUE)
        }
    }

    companion object {
        val DEFAULT_THRESHOLD_NS: Long = TimeUnit.SECONDS.toNanos(5)
    }
}
