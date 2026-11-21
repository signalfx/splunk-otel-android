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

package com.splunk.rum.integration.startup

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupSessionContextTest {

    @Test
    fun delayedFirstDrawKeepsTheFirstForegroundSession() {
        val context = StartupSessionContext()

        context.onForeground(null)
        context.onSessionCreated(StartupSession("A", null))
        context.onSessionCreated(StartupSession("B", "A")) // A rotated before first draw.

        assertEquals(StartupSession("A", null), context.sessionForStart())
        context.onStartReported()
        assertNull(context.sessionForStart())

        context.onForeground(StartupSession("B", "A"))
        assertEquals(StartupSession("B", "A"), context.sessionForStart())
    }

    @Test
    fun abandonedAttemptCannotGiveItsSessionToALaterLaunch() {
        val context = StartupSessionContext()

        context.onForeground(StartupSession("A", null))
        context.onBackground()
        assertNull(context.sessionForStart())

        context.onForeground(null)
        context.onSessionCreated(StartupSession("B", null))
        assertEquals(StartupSession("B", null), context.sessionForStart())
    }
}
