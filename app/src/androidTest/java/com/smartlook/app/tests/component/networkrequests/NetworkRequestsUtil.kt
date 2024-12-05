/*
 * Copyright 2024 Splunk Inc.
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

package com.smartlook.app.tests.component.networkrequests

import android.util.Log
import com.smartlook.app.lib.ZipkinCommunicator

object NetworkRequestsUtil {
    fun verifyBySpanNameWithZipkin(method: String, duration: Long) {
        val traces = ZipkinCommunicator.getAllTracesBySpanName(method.lowercase(), duration)
        Log.i("TRACES", traces)

        assert(
            traces.contains(
                """
                "http.method":"${method.uppercase()}"
                """.trimIndent()
            )
        )

        assert(
            traces.contains(
                """
                "name":"${method.lowercase()}"
                """.trimIndent()
            )
        )
    }
}