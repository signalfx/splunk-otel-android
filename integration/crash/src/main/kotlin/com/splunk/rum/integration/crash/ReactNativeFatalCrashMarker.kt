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

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicReference

internal class ReactNativeFatalCrashMarker(
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val validityMillis: Long = DEFAULT_VALIDITY_MILLIS
) {
    private data class Marker(val spanId: String, val armedAtMillis: Long)

    private val marker = AtomicReference<Marker?>()

    fun arm(spanId: String) {
        marker.set(Marker(spanId, elapsedRealtime()))
    }

    fun consumeIfMatching(throwable: Throwable): Boolean {
        if (throwable.javaClass.name != REACT_NATIVE_JAVASCRIPT_EXCEPTION) return false

        while (true) {
            val candidate = marker.get() ?: return false
            val age = elapsedRealtime() - candidate.armedAtMillis
            if (age !in 0..validityMillis) {
                marker.compareAndSet(candidate, null)
                return false
            }
            if (marker.compareAndSet(candidate, null)) return true
        }
    }

    fun clear() {
        marker.set(null)
    }

    private companion object {
        private const val DEFAULT_VALIDITY_MILLIS = 2_000L
        private const val REACT_NATIVE_JAVASCRIPT_EXCEPTION =
            "com.facebook.react.common.JavascriptException"
    }
}

internal object ReactNativeFatalCrashState {
    val marker = ReactNativeFatalCrashMarker()
}
