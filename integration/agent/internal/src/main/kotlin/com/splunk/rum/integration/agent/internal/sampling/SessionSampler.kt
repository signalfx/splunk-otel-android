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
            else -> (sessionIdToUInt32(sessionId()) xor domainSalt(domain)) < (rate * UINT32_MAX).toLong()
        }

    private fun sessionIdToUInt32(sessionId: String): Long {
        var accumulator = 0L
        var index = 0

        while (index < sessionId.length) {
            val end = minOf(index + 8, sessionId.length)
            accumulator = accumulator xor (sessionId.substring(index, end).toLongOrNull(16) ?: 0L)
            index += 8
        }

        return accumulator and UINT32_MAX
    }

    private fun domainSalt(domain: String): Long =
        if (domain.isEmpty()) 0L else domain.hashCode().toLong() and UINT32_MAX
}
