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
import org.junit.Assert.assertEquals
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

    private companion object {
        private val THRESHOLD_NS = TimeUnit.SECONDS.toNanos(5)
    }
}
