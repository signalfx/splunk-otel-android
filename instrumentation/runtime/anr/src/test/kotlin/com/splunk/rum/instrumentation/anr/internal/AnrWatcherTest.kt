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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnrWatcherTest {

    private lateinit var handler: Handler
    private lateinit var mainThread: Thread
    private val reportedStackTraces = mutableListOf<Array<StackTraceElement>>()
    private val onAnr: (Array<StackTraceElement>) -> Unit = { reportedStackTraces.add(it) }
    private var fakeTimeNs = 0L
    private val clock: () -> Long = { fakeTimeNs }

    @Before
    fun setUp() {
        handler = mock(Handler::class.java)
        mainThread = Thread.currentThread()
        reportedStackTraces.clear()
        fakeTimeNs = 0L
    }

    @Test
    fun `does not report when the main thread handler rejects the post`() {
        `when`(handler.post(any())).thenReturn(false)

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        repeat(6) {
            fakeTimeNs += TimeUnit.SECONDS.toNanos(2)
            watcher.run()
        }

        assertTrue(reportedStackTraces.isEmpty())
    }

    @Test
    fun `does not report while the main thread keeps responding`() {
        `when`(handler.post(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as Runnable).run()
            true
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        repeat(10) {
            fakeTimeNs += TimeUnit.SECONDS.toNanos(2)
            watcher.run()
        }

        assertTrue(reportedStackTraces.isEmpty())
    }

    @Test
    fun `does not report when stall is shorter than threshold`() {
        var callCount = 0
        `when`(handler.post(any())).thenAnswer { invocation ->
            callCount++
            if (callCount == 1) {
                true
            } else {
                (invocation.getArgument(0) as Runnable).run()
                true
            }
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(3)
        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()

        assertTrue(reportedStackTraces.isEmpty())
    }

    @Test
    fun `reports when stall reaches threshold`() {
        `when`(handler.post(any())).thenReturn(true)

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(4)
        watcher.run()
        assertTrue(reportedStackTraces.isEmpty())

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()
        assertEquals(1, reportedStackTraces.size)
    }

    @Test
    fun `does not report again during the same continuous stall`() {
        `when`(handler.post(any())).thenReturn(true)

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(5)
        watcher.run()
        assertEquals(1, reportedStackTraces.size)

        fakeTimeNs += TimeUnit.SECONDS.toNanos(5)
        watcher.run()
        assertEquals(1, reportedStackTraces.size)

        fakeTimeNs += TimeUnit.SECONDS.toNanos(10)
        watcher.run()
        assertEquals(1, reportedStackTraces.size)
    }

    @Test
    fun `resets after main thread recovers`() {
        var heartbeatCallback: Runnable? = null
        `when`(handler.post(any())).thenAnswer { invocation ->
            heartbeatCallback = invocation.getArgument(0) as Runnable
            true
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()
        fakeTimeNs += TimeUnit.SECONDS.toNanos(5)
        watcher.run()
        assertEquals(1, reportedStackTraces.size)

        heartbeatCallback?.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(4)
        watcher.run()
        assertEquals(1, reportedStackTraces.size)

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()
        assertEquals(2, reportedStackTraces.size)
    }

    @Test
    fun `works with custom threshold`() {
        `when`(handler.post(any())).thenReturn(true)

        val customThresholdNs = TimeUnit.SECONDS.toNanos(3)
        val watcher = AnrWatcher(handler, mainThread, onAnr, customThresholdNs, clock)

        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(2)
        watcher.run()
        assertTrue(reportedStackTraces.isEmpty())

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()
        assertEquals(1, reportedStackTraces.size)
    }

    @Test
    fun `reports ANR when main thread recovers past deadline but watchdog was delayed`() {
        var heartbeatCallback: Runnable? = null
        `when`(handler.post(any())).thenAnswer { invocation ->
            heartbeatCallback = invocation.getArgument(0) as Runnable
            true
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(6)
        heartbeatCallback?.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()
        assertEquals(
            "Should still report ANR even though main thread recovered",
            1,
            reportedStackTraces.size
        )

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(5)
        watcher.run()
        assertEquals(
            "Detection must re-arm after late-recovery ANR is reported",
            2,
            reportedStackTraces.size
        )
    }

    @Test
    fun `does not false-report with a threshold near Long MAX_VALUE`() {
        `when`(handler.post(any())).thenReturn(true)

        fakeTimeNs = TimeUnit.SECONDS.toNanos(100)
        val watcher = AnrWatcher(handler, mainThread, onAnr, Long.MAX_VALUE, clock)

        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()
        assertTrue("Should not false-report with an extreme threshold", reportedStackTraces.isEmpty())

        fakeTimeNs += TimeUnit.SECONDS.toNanos(10)
        watcher.run()
        assertTrue("Should still not report with an extreme threshold", reportedStackTraces.isEmpty())
    }

    @Test
    fun `reports when the heartbeat completes past the threshold between two watchdog runs`() {
        var heartbeatCallback: Runnable? = null
        `when`(handler.post(any())).thenAnswer { invocation ->
            heartbeatCallback = invocation.getArgument(0) as Runnable
            true
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()

        // Watchdog samples just before the threshold and finds the heartbeat still outstanding.
        fakeTimeNs = THRESHOLD_NS - 1
        watcher.run()
        assertTrue(reportedStackTraces.isEmpty())

        // Main thread recovers just past the threshold, after that observation.
        fakeTimeNs = THRESHOLD_NS + 1
        heartbeatCallback?.run()

        watcher.run()
        assertEquals(
            "An expired heartbeat observed only after completion must still be reported",
            1,
            reportedStackTraces.size
        )
    }

    @Test
    fun `does not drop the stall when the main thread completes mid watchdog run`() {
        var heartbeatCallback: Runnable? = null
        `when`(handler.post(any())).thenAnswer { invocation ->
            heartbeatCallback = invocation.getArgument(0) as Runnable
            true
        }

        // Recovers the main thread past the threshold while the watchdog is between reading the
        // clock and acting on it, so the run decides using a below-threshold reading.
        var interleaveRecovery = false
        val racingClock: () -> Long = {
            val observed = fakeTimeNs
            if (interleaveRecovery) {
                interleaveRecovery = false
                fakeTimeNs = THRESHOLD_NS + 1
                heartbeatCallback?.run()
            }
            observed
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, racingClock)

        watcher.run()

        fakeTimeNs = THRESHOLD_NS - 1
        interleaveRecovery = true
        watcher.run()

        watcher.run()
        assertEquals(
            "A heartbeat that expired while the watchdog was mid-run must still be reported",
            1,
            reportedStackTraces.size
        )
    }

    @Test
    fun `late recovery reports the stalled stack instead of the post-recovery stack`() {
        var heartbeatCallback: Runnable? = null
        `when`(handler.post(any())).thenAnswer { invocation ->
            heartbeatCallback = invocation.getArgument(0) as Runnable
            true
        }

        val worker = StallingThread()
        worker.start()
        worker.enteredStalledFrame.await()

        try {
            val watcher = AnrWatcher(handler, worker, onAnr, THRESHOLD_NS, clock)

            watcher.run()

            // Watchdog samples the stall while it is still observable.
            fakeTimeNs += TimeUnit.SECONDS.toNanos(4)
            watcher.run()
            assertTrue(reportedStackTraces.isEmpty())

            // Main thread recovers and moves on to unrelated work.
            worker.releaseStalledFrame.countDown()
            worker.enteredRecoveredFrame.await()

            fakeTimeNs += TimeUnit.SECONDS.toNanos(2)
            heartbeatCallback?.run()

            watcher.run()

            assertEquals(1, reportedStackTraces.size)
            val reported = reportedStackTraces.single()
            assertTrue(
                "Report should describe the stalled code",
                reported.any { it.methodName == STALLED_FRAME }
            )
            assertFalse(
                "Report should not describe work started after recovery",
                reported.any { it.methodName == RECOVERED_FRAME }
            )
        } finally {
            worker.releaseStalledFrame.countDown()
            worker.releaseRecoveredFrame.countDown()
            worker.join(TimeUnit.SECONDS.toMillis(5))
        }
    }

    @Test
    fun `reset discards an in-flight stall so it cannot be reported later`() {
        `when`(handler.post(any())).thenReturn(true)

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(4)
        watcher.run()
        assertTrue(reportedStackTraces.isEmpty())

        watcher.reset()

        // Past the original deadline, but that cycle was discarded.
        fakeTimeNs += TimeUnit.SECONDS.toNanos(2)
        watcher.run()
        assertTrue("A discarded stall must not be reported", reportedStackTraces.isEmpty())

        fakeTimeNs += TimeUnit.SECONDS.toNanos(4)
        watcher.run()
        assertTrue("Threshold is measured from the new cycle", reportedStackTraces.isEmpty())

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()
        assertEquals("Detection still works after a reset", 1, reportedStackTraces.size)
    }

    @Test
    fun `reset discards a pending late recovery`() {
        var heartbeatCallback: Runnable? = null
        `when`(handler.post(any())).thenAnswer { invocation ->
            heartbeatCallback = invocation.getArgument(0) as Runnable
            true
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, THRESHOLD_NS, clock)

        watcher.run()

        // Main thread recovers past the deadline; the report is still pending a watchdog run.
        fakeTimeNs += TimeUnit.SECONDS.toNanos(6)
        heartbeatCallback?.run()

        watcher.reset()

        fakeTimeNs += TimeUnit.SECONDS.toNanos(1)
        watcher.run()
        assertTrue("A pending late recovery must not survive a reset", reportedStackTraces.isEmpty())
    }

    private class StallingThread : Thread("anr-watcher-test-main") {

        val enteredStalledFrame = CountDownLatch(1)
        val releaseStalledFrame = CountDownLatch(1)
        val enteredRecoveredFrame = CountDownLatch(1)
        val releaseRecoveredFrame = CountDownLatch(1)

        override fun run() {
            stalledFrame()
            recoveredFrame()
        }

        private fun stalledFrame() {
            enteredStalledFrame.countDown()
            releaseStalledFrame.await()
        }

        private fun recoveredFrame() {
            enteredRecoveredFrame.countDown()
            releaseRecoveredFrame.await()
        }
    }

    private companion object {
        private val THRESHOLD_NS = TimeUnit.SECONDS.toNanos(5)
        private const val STALLED_FRAME = "stalledFrame"
        private const val RECOVERED_FRAME = "recoveredFrame"
    }
}
