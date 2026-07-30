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

    @Before
    fun setUp() {
        handler = mock(Handler::class.java)
        mainThread = Thread.currentThread()
        reportedStackTraces.clear()
    }

    @Test
    fun `does not report when the main thread handler rejects the post`() {
        `when`(handler.post(any())).thenReturn(false)

        val watcher = AnrWatcher(handler, mainThread, onAnr, SHORT_POLL_NS)
        repeat(6) { watcher.run() }

        assertTrue(reportedStackTraces.isEmpty())
    }

    @Test
    fun `does not report while the main thread keeps responding`() {
        `when`(handler.post(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as Runnable).run()
            true
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, SHORT_POLL_NS)
        repeat(6) { watcher.run() }

        assertTrue(reportedStackTraces.isEmpty())
    }

    @Test
    fun `does not report a temporary pause shorter than the threshold`() {
        var poll = 0
        `when`(handler.post(any())).thenAnswer { invocation ->
            // Miss a single poll, then recover.
            if (poll != 3) {
                (invocation.getArgument(0) as Runnable).run()
            }
            poll++
            true
        }

        val watcher = AnrWatcher(handler, mainThread, onAnr, SHORT_POLL_NS)
        repeat(6) { watcher.run() }

        assertTrue(reportedStackTraces.isEmpty())
    }

    @Test
    fun `reports once after five consecutive missed polls then resets`() {
        // Post is accepted but the callback never runs, so every poll times out.
        `when`(handler.post(any())).thenReturn(true)

        val watcher = AnrWatcher(handler, mainThread, onAnr, SHORT_POLL_NS)

        repeat(5) { watcher.run() }
        assertEquals(1, reportedStackTraces.size)

        // No second report until another five misses accumulate.
        repeat(4) { watcher.run() }
        assertEquals(1, reportedStackTraces.size)

        watcher.run()
        assertEquals(2, reportedStackTraces.size)
    }

    @Test
    fun `reports after the configured number of missed polls`() {
        `when`(handler.post(any())).thenReturn(true)

        val customThreshold = 3
        val watcher = AnrWatcher(handler, mainThread, onAnr, SHORT_POLL_NS, maxMissedPolls = customThreshold)

        repeat(customThreshold - 1) { watcher.run() }
        assertTrue(reportedStackTraces.isEmpty())

        watcher.run()
        assertEquals(1, reportedStackTraces.size)
    }

    @Test
    fun `custom threshold resets after reporting`() {
        `when`(handler.post(any())).thenReturn(true)

        val customThreshold = 2
        val watcher = AnrWatcher(handler, mainThread, onAnr, SHORT_POLL_NS, maxMissedPolls = customThreshold)

        repeat(customThreshold) { watcher.run() }
        assertEquals(1, reportedStackTraces.size)

        watcher.run()
        assertEquals(1, reportedStackTraces.size)

        repeat(customThreshold) { watcher.run() }
        assertEquals(2, reportedStackTraces.size)
    }

    private companion object {
        private val SHORT_POLL_NS = TimeUnit.MILLISECONDS.toNanos(20)
    }
}
