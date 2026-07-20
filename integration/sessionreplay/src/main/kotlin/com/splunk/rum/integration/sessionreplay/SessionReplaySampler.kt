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

/**
 * Deterministic, session-stable sampling decision.
 *
 * The decision is derived purely from the session id, so the same session always yields the same
 * result. This keeps the decision consistent across process restarts for a reused session, instead
 * of being re-rolled on every cold start.
 */
internal object SessionReplaySampler {

    private const val UINT32_MAX = 0xFFFFFFFFL

    fun shouldRecord(sessionId: String, samplingRate: Float): Boolean =
        when (val rate = samplingRate.coerceIn(0f, 1f)) {
            0f -> false
            1f -> true
            else -> sessionIdToUInt32(sessionId) < (rate * UINT32_MAX).toLong()
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
}
