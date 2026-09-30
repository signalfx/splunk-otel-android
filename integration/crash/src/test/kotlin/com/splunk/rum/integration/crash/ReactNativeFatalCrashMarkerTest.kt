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

package com.splunk.rum.integration.crash

import com.facebook.react.common.JavascriptException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactNativeFatalCrashMarkerTest {

    @Test
    fun `fresh marker suppresses one JavascriptException`() {
        var now = 1_000L
        val marker = ReactNativeFatalCrashMarker(elapsedRealtime = { now }, validityMillis = 2_000)
        marker.arm("0123456789abcdef")
        now += 20

        assertTrue(marker.consumeIfMatching(JavascriptException("fatal")))
        assertFalse(marker.consumeIfMatching(JavascriptException("fatal")))
    }

    @Test
    fun `marker never suppresses another throwable type`() {
        val marker = ReactNativeFatalCrashMarker(elapsedRealtime = { 1_000L })
        marker.arm("0123456789abcdef")

        assertFalse(marker.consumeIfMatching(RuntimeException("native")))
    }

    @Test
    fun `expired marker fails open`() {
        var now = 1_000L
        val marker = ReactNativeFatalCrashMarker(elapsedRealtime = { now }, validityMillis = 2_000)
        marker.arm("0123456789abcdef")
        now += 2_001

        assertFalse(marker.consumeIfMatching(JavascriptException("fatal")))
    }
}
