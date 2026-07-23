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

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class AnrDetectorTogglerTest {

    private lateinit var anrWatcher: Runnable
    private lateinit var scheduler: ScheduledExecutorService
    private lateinit var future: ScheduledFuture<*>
    private lateinit var toggler: AnrDetectorToggler

    @Before
    fun setUp() {
        anrWatcher = mock(Runnable::class.java)
        scheduler = mock(ScheduledExecutorService::class.java)
        future = mock(ScheduledFuture::class.java)
        `when`(scheduler.scheduleAtFixedRate(anrWatcher, 1L, 1L, TimeUnit.SECONDS)).thenReturn(future)
        toggler = AnrDetectorToggler(anrWatcher, scheduler)
    }

    @Test
    fun `schedules the watcher only once while repeatedly foregrounded`() {
        toggler.onAppForegrounded()
        toggler.onAppForegrounded()
        toggler.onAppForegrounded()

        verify(scheduler, times(1)).scheduleAtFixedRate(anrWatcher, 1L, 1L, TimeUnit.SECONDS)
    }

    @Test
    fun `cancels the scheduled watcher when backgrounded`() {
        toggler.onAppForegrounded()

        toggler.onAppBackgrounded()
        toggler.onAppBackgrounded()
        toggler.onAppBackgrounded()

        verify(future, times(1)).cancel(true)
    }

    @Test
    fun `reschedules after a background then foreground cycle`() {
        toggler.onAppForegrounded()
        toggler.onAppBackgrounded()
        toggler.onAppForegrounded()

        verify(scheduler, times(2)).scheduleAtFixedRate(anrWatcher, 1L, 1L, TimeUnit.SECONDS)
    }
}
