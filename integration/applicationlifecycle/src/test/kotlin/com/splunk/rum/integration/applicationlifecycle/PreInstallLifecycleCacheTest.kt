/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.applicationlifecycle

import com.splunk.rum.integration.applicationlifecycle.model.AppState
import com.splunk.rum.integration.applicationlifecycle.model.ApplicationLifecycleData
import org.junit.Assert.assertEquals
import org.junit.Test

class PreInstallLifecycleCacheTest {

    @Test
    fun `closed Activity events do not attach to the next foreground session`() {
        val cache = PreInstallLifecycleCache()
        val abandoned = ApplicationLifecycleData(100L, AppState.CREATED)
        val created = ApplicationLifecycleData(1_000L, AppState.CREATED)
        val foreground = ApplicationLifecycleData(2_000L, AppState.FOREGROUND)

        cache.add(abandoned)
        cache.markAppClosed()
        cache.add(created)
        cache.add(foreground)

        assertEquals(
            listOf(created, foreground),
            cache.eventsToReport(allowBackgroundSessionCreation = false)
        )
        assertEquals(
            listOf(abandoned, created, foreground),
            cache.eventsToReport(allowBackgroundSessionCreation = true)
        )
    }

    @Test
    fun `full event cache cannot hide the next foreground attempt`() {
        val cache = PreInstallLifecycleCache()
        val earlierEvents = mutableListOf<ApplicationLifecycleData>()
        repeat(5) { attempt ->
            val created = ApplicationLifecycleData(attempt * 2L, AppState.CREATED)
            val foreground = ApplicationLifecycleData(attempt * 2L + 1L, AppState.FOREGROUND)
            cache.add(created)
            cache.add(foreground)
            earlierEvents += created
            earlierEvents += foreground
            cache.markAppClosed()
        }

        val created = ApplicationLifecycleData(100L, AppState.CREATED)
        val foreground = ApplicationLifecycleData(101L, AppState.FOREGROUND)
        cache.add(created)
        cache.add(foreground)

        assertEquals(listOf(created, foreground), cache.eventsToReport(allowBackgroundSessionCreation = false))
        assertEquals(earlierEvents, cache.eventsToReport(allowBackgroundSessionCreation = true))
    }

    @Test
    fun `backgrounding without closing discards the old foreground attempt`() {
        val cache = PreInstallLifecycleCache()
        val created = ApplicationLifecycleData(1_000L, AppState.CREATED)
        val foreground = ApplicationLifecycleData(2_000L, AppState.FOREGROUND)
        val background = ApplicationLifecycleData(3_000L, AppState.BACKGROUND)

        cache.add(created)
        cache.add(foreground)
        cache.add(background)

        assertEquals(emptyList<ApplicationLifecycleData>(), cache.eventsToReport(false))
        assertEquals(listOf(created, foreground, background), cache.eventsToReport(true))
    }
}
