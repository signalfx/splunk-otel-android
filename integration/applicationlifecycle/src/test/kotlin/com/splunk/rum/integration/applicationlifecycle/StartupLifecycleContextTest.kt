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
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupLifecycleContextTest {

    @Test
    fun `keeps only the current Activity attempt with its original times`() {
        val context = StartupLifecycleContext()
        val abandoned = ApplicationLifecycleData(1L, AppState.CREATED)
        val created = ApplicationLifecycleData(3L, AppState.CREATED)
        val foreground = ApplicationLifecycleData(4L, AppState.FOREGROUND)

        context.hold(abandoned)
        context.clear() // The first Activity was destroyed before reaching foreground.
        context.hold(created)
        context.hold(foreground)

        assertEquals(listOf(created, foreground), context.take())
        assertTrue(context.take().isEmpty())
    }

    @Test
    fun `background events are not kept without a session`() {
        val context = StartupLifecycleContext()

        context.hold(ApplicationLifecycleData(5L, AppState.BACKGROUND))

        assertTrue(context.take().isEmpty())
    }
}
