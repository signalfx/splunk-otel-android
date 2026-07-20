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

package com.splunk.rum.integration.sessionreplay

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionReplaySamplerTest {

    @Test
    fun `rate of 1 always records`() {
        repeat(1000) {
            assertTrue(SessionReplaySampler.shouldRecord(randomSessionId(), 1f))
        }
    }

    @Test
    fun `rate of 0 never records`() {
        repeat(1000) {
            assertFalse(SessionReplaySampler.shouldRecord(randomSessionId(), 0f))
        }
    }

    @Test
    fun `decision is stable for the same session id`() {
        repeat(1000) {
            val sessionId = randomSessionId()
            val first = SessionReplaySampler.shouldRecord(sessionId, 0.2f)
            val second = SessionReplaySampler.shouldRecord(sessionId, 0.2f)
            assertEquals(first, second)
        }
    }

    @Test
    fun `sampled proportion is close to the configured rate`() {
        val rate = 0.2f
        val total = 100_000
        val recorded = (0 until total).count {
            SessionReplaySampler.shouldRecord(randomSessionId(), rate)
        }
        val observed = recorded.toDouble() / total
        assertTrue("observed=$observed, expected~$rate", abs(observed - rate) < 0.02)
    }

    private fun randomSessionId(): String = (0 until 32).joinToString("") { "0123456789abcdef".random().toString() }
}
