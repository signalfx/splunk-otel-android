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
 * Only one ANR is reported per stall. The heartbeat just records when it ran; the watchdog thread
 * owns every state transition, so a run delayed past recovery still sees the stall.
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
    private val heartbeatCompletedAtNs = AtomicLong(NOT_COMPLETED_SENTINEL)
    private val reported = AtomicBoolean(false)

    /** Bumped by [reset] so an in-flight run can tell its observations are stale. */
    private val latestWatcherVersion = AtomicLong(0)

    /** Identifies the active heartbeat so an abandoned callback cannot complete a later one. */
    private val heartbeatToken = AtomicLong(0)

    /** Sampled while the main thread was still stalled, in case it recovers before the next run. */
    @Volatile
    private var stalledStack: Array<StackTraceElement>? = null

    override fun run() {
        val watcherVersion = latestWatcherVersion.get()
        if (heartbeatOutstanding.get() && !resolveOutstandingHeartbeat(watcherVersion)) {
            return
        }
        startHeartbeat(watcherVersion)
    }

    /** Drops in-flight state so a stall cannot be reported against a later detection period. */
    fun reset() {
        latestWatcherVersion.incrementAndGet()
        heartbeatToken.incrementAndGet()
        heartbeatCompletedAtNs.set(NOT_COMPLETED_SENTINEL)
        reported.set(false)
        stalledStack = null
        heartbeatOutstanding.set(false)
    }

    /** Returns true once the heartbeat is accounted for and a new cycle can start. */
    private fun resolveOutstandingHeartbeat(watcherVersion: Long): Boolean {
        val startedAtNs = heartbeatStartedAtNs.get()

        // Clock before completion stamp: a stale clock undercounts the stall; a stale stamp fakes an ANR.
        val nowNs = clock()
        val completedAtNs = heartbeatCompletedAtNs.get()

        // A completion stamped before this cycle began belongs to an earlier heartbeat.
        val responded = completedAtNs != NOT_COMPLETED_SENTINEL && completedAtNs - startedAtNs >= 0

        // Subtraction stays correct across nanoTime wraparound.
        val elapsedNs = if (responded) completedAtNs - startedAtNs else nowNs - startedAtNs

        // Reads taken before a reset describe a cancelled period.
        if (latestWatcherVersion.get() != watcherVersion) {
            return false
        }

        if (!reported.get()) {
            if (elapsedNs >= thresholdNs) {
                val stack = if (responded) stalledStack ?: mainThread.stackTrace else mainThread.stackTrace

                // Recheck after the stack walk, which dominates the gap, then claim the report.
                if (latestWatcherVersion.get() == watcherVersion && reported.compareAndSet(false, true)) {
                    stalledStack = null
                    onAnr(stack)
                }
            } else if (!responded && stalledStack == null) {
                // One sample per stall; refreshing every poll would scale stack walks with the threshold.
                stalledStack = mainThread.stackTrace
            }
        }

        if (!responded) {
            return false
        }
        heartbeatOutstanding.set(false)
        return true
    }

    private fun startHeartbeat(watcherVersion: Long) {
        if (!heartbeatOutstanding.compareAndSet(false, true)) {
            return
        }
        reported.set(false)
        stalledStack = null
        heartbeatCompletedAtNs.set(NOT_COMPLETED_SENTINEL)
        val token = heartbeatToken.incrementAndGet()
        heartbeatStartedAtNs.set(clock())

        // Reset landed mid-start; leave detection disarmed.
        if (latestWatcherVersion.get() != watcherVersion) {
            heartbeatOutstanding.set(false)
            return
        }

        val posted = uiHandler.post {
            if (heartbeatToken.get() == token) {
                heartbeatCompletedAtNs.set(clock())
            }
        }
        if (!posted) {
            // The main thread is probably shutting down. Retry on the next run.
            heartbeatOutstanding.set(false)
        }
    }

    companion object {
        val DEFAULT_THRESHOLD_NS: Long = TimeUnit.SECONDS.toNanos(5)
        private const val NOT_COMPLETED_SENTINEL = Long.MIN_VALUE
    }
}
