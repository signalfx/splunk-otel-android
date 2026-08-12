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

import java.security.MessageDigest

/**
 * Deterministic, session-stable sampling decision.
 */
object SessionSampler {

    private const val UINT32_MAX = 0xFFFFFFFFL

    /**
     * Returns whether the session identified by [sessionId] should be sampled in for the given
     * [samplingRate] (clamped to `0.0..1.0`).
     */
    fun shouldSample(samplingRate: Double, domain: String = "", sessionId: () -> String): Boolean =
        when (val rate = samplingRate.coerceIn(0.0, 1.0)) {
            0.0 -> false
            1.0 -> true
            else -> samplingKey(domain, sessionId()) < (rate * UINT32_MAX).toLong()
        }

    private fun samplingKey(domain: String, sessionId: String): Long {
        val input = "$domain\u0000$sessionId".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)

        return ((digest[0].toLong() and 0xFF) shl 24) or
            ((digest[1].toLong() and 0xFF) shl 16) or
            ((digest[2].toLong() and 0xFF) shl 8) or
            (digest[3].toLong() and 0xFF)
    }
}
