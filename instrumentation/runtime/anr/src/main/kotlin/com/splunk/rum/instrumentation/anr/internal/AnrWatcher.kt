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
 * main thread fails to process the heartbeat within [thresholdNs] of monotonic elapsed time, an
 * ANR is reported via [onAnr] with the main thread's stalled stack trace.
 *
 * Only one ANR is reported per stall. A new report is only emitted after the main thread recovers
 * and subsequently stalls again.
 *
 * The heartbeat only records when it ran; the watchdog thread owns every state transition. That
 * keeps main-thread work minimal and lets a watchdog run delayed past recovery still see the stall.
 *
 * @property thresholdNs Monotonic elapsed time in nanoseconds that the main thread must be
 *           unresponsive before an ANR is reported.
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
    private val heartbeatStartedAtNs = AtomicLong(0)
    private val heartbeatCompletedAtNs = AtomicLong(NOT_COMPLETED)
    private val reported = AtomicBoolean(false)

    /** Sampled while the main thread was still stalled, in case it recovers before the next run. */
    @Volatile
    private var stalledStack: Array<StackTraceElement>? = null

    override fun run() {
        if (heartbeatOutstanding.get() && !resolveOutstandingHeartbeat()) {
            return
        }
        startHeartbeat()
    }

    /** Drops in-flight state so a stall cannot be reported against a later detection period. */
    fun reset() {
        heartbeatCompletedAtNs.set(NOT_COMPLETED)
        reported.set(false)
        stalledStack = null
        heartbeatOutstanding.set(false)
    }

    /** Returns true once the heartbeat is accounted for and a new cycle can start. */
    private fun resolveOutstandingHeartbeat(): Boolean {
        val startedAtNs = heartbeatStartedAtNs.get()
        val completedAtNs = heartbeatCompletedAtNs.get()

        // A completion stamped before this cycle began belongs to an earlier heartbeat.
        val responded = completedAtNs != NOT_COMPLETED && completedAtNs - startedAtNs >= 0

        // Subtraction stays correct across nanoTime wraparound.
        val elapsedNs = if (responded) completedAtNs - startedAtNs else clock() - startedAtNs

        if (!reported.get()) {
            if (elapsedNs >= thresholdNs) {
                onAnr(if (responded) stalledStack ?: mainThread.stackTrace else mainThread.stackTrace)
                reported.set(true)
                stalledStack = null
            } else if (!responded) {
                stalledStack = mainThread.stackTrace
            }
        }

        if (!responded) {
            return false
        }
        heartbeatOutstanding.set(false)
        return true
    }

    private fun startHeartbeat() {
        if (!heartbeatOutstanding.compareAndSet(false, true)) {
            return
        }
        reported.set(false)
        stalledStack = null
        heartbeatCompletedAtNs.set(NOT_COMPLETED)
        heartbeatStartedAtNs.set(clock())

        if (!uiHandler.post { heartbeatCompletedAtNs.set(clock()) }) {
            // The main thread is probably shutting down. Retry on the next run.
            heartbeatOutstanding.set(false)
        }
    }

    companion object {
        val DEFAULT_THRESHOLD_NS: Long = TimeUnit.SECONDS.toNanos(5)
        private const val NOT_COMPLETED = Long.MIN_VALUE
    }
}
