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

package com.splunk.rum.integration.agent.internal.session

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionActivityTrackerTest {

    @Test
    fun `sessionLastActivity is null before any activity`() {
        val tracker = SessionActivityTracker()

        assertNull(tracker.sessionLastActivity)
    }

    @Test
    fun `trackActivity stores current timestamp`() {
        val tracker = SessionActivityTracker()
        val before = System.currentTimeMillis()

        tracker.trackActivity()

        val tracked = tracker.sessionLastActivity
        val after = System.currentTimeMillis()

        assertNotNull(tracked)
        assertTrue(tracked!! in before..after)
    }

    @Test
    fun `reset clears tracked activity`() {
        val tracker = SessionActivityTracker()

        tracker.trackActivity()
        assertNotNull(tracker.sessionLastActivity)

        tracker.reset()

        assertNull(tracker.sessionLastActivity)
    }
}
