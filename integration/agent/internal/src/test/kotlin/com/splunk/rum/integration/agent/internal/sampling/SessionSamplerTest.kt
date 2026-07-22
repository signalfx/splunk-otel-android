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

package com.splunk.rum.integration.agent.internal.sampling

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSamplerTest {

    @Test
    fun `rate of 1 always samples in`() {
        repeat(1000) {
            assertTrue(SessionSampler.shouldSample(1.0) { randomSessionId() })
        }
    }

    @Test
    fun `rate of 0 never samples in`() {
        repeat(1000) {
            assertFalse(SessionSampler.shouldSample(0.0) { randomSessionId() })
        }
    }

    @Test
    fun `rate is clamped to valid range`() {
        assertTrue(SessionSampler.shouldSample(2.0) { randomSessionId() })
        assertFalse(SessionSampler.shouldSample(-1.0) { randomSessionId() })
    }

    @Test
    fun `decision is stable for the same session id`() {
        repeat(1000) {
            val sessionId = randomSessionId()
            val first = SessionSampler.shouldSample(0.2) { sessionId }
            val second = SessionSampler.shouldSample(0.2) { sessionId }
            assertEquals(first, second)
        }
    }

    @Test
    fun `session id is not resolved for trivial rates`() {
        val boom: () -> String = { error("session id must not be resolved for 0.0 or 1.0 rates") }

        assertFalse(SessionSampler.shouldSample(0.0, sessionId = boom))
        assertTrue(SessionSampler.shouldSample(1.0, sessionId = boom))
    }

    @Test
    fun `session id is resolved once for a mid-range rate`() {
        var resolveCount = 0

        SessionSampler.shouldSample(0.5) {
            resolveCount++
            randomSessionId()
        }

        assertEquals(1, resolveCount)
    }

    @Test
    fun `sampled proportion is close to the configured rate`() {
        val rate = 0.2
        val total = 100_000

        val sampledIn = (0 until total).count {
            SessionSampler.shouldSample(rate) { randomSessionId() }
        }

        val observed = sampledIn.toDouble() / total
        assertTrue("observed=$observed, expected~$rate", abs(observed - rate) < 0.02)
    }

    @Test
    fun `default domain matches empty domain`() {
        repeat(1000) {
            val sessionId = randomSessionId()
            assertEquals(
                SessionSampler.shouldSample(0.3) { sessionId },
                SessionSampler.shouldSample(0.3, "") { sessionId }
            )
        }
    }

    @Test
    fun `decision is stable for the same session id and domain`() {
        repeat(1000) {
            val sessionId = randomSessionId()
            val first = SessionSampler.shouldSample(0.2, "sessionReplay") { sessionId }
            val second = SessionSampler.shouldSample(0.2, "sessionReplay") { sessionId }
            assertEquals(first, second)
        }
    }

    @Test
    fun `different domains are not fully nested for the same session and rate`() {
        val total = 5_000
        val rate = 0.5
        var sameDecisionCount = 0

        repeat(total) {
            val sessionId = randomSessionId()
            val default = SessionSampler.shouldSample(rate) { sessionId }
            val replay = SessionSampler.shouldSample(rate, "sessionReplay") { sessionId }
            if (default == replay) sameDecisionCount++
        }

        val observedAgreement = sameDecisionCount.toDouble() / total
        assertTrue(
            "observedAgreement=$observedAgreement should not be nearly fully correlated",
            observedAgreement < 0.7
        )
    }

    @Test
    fun `sampled proportion is close to the configured rate for a non-default domain`() {
        val rate = 0.2
        val total = 100_000

        val sampledIn = (0 until total).count {
            SessionSampler.shouldSample(rate, "sessionReplay") { randomSessionId() }
        }

        val observed = sampledIn.toDouble() / total
        assertTrue("observed=$observed, expected~$rate", abs(observed - rate) < 0.02)
    }

    private fun randomSessionId(): String = (0 until 32).joinToString("") { "0123456789abcdef".random().toString() }
}
