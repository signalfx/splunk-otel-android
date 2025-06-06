/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.agent.internal.utils

import java.security.SecureRandom

internal object TraceId {

    private const val LENGTH = 32
    private val INVALID = "0".repeat(LENGTH)
    private val CHARACTERS = "0123456789abcdef"

    fun random(): String {
        val random = SecureRandom()
        var result: String

        do {
            result = ""

            for (i in 0 until LENGTH) {
                result += CHARACTERS[random.nextInt(CHARACTERS.length)]
            }
        } while (result == INVALID)

        return result
    }
}
