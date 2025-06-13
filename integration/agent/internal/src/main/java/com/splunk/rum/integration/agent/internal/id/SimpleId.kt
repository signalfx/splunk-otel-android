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

package com.splunk.rum.integration.agent.internal.id

import java.security.SecureRandom

internal object SimpleId {

    private const val CHARACTERS = "0123456789abcdef"
    private val random = SecureRandom()

    /**
     * Generates a random lowercase hexadecimal ID of the given length.
     *
     * @param length The number of characters in the ID. Must be greater than 0.
     * @return The generated ID:
     * - Is exactly [length] characters long.
     * - Consists only of characters `0` through `9` and `a` through `f`.
     * - Is never composed entirely of zeroes (e.g., "000000...").
     *
     * @throws IllegalArgumentException if [length] is not positive.
     */
    internal fun generate(length: Int): String {
        require(length > 0) { "ID length must be positive" }

        val invalid = "0".repeat(length)
        var result: String

        do {
            val builder = StringBuilder(length)
            repeat(length) {
                builder.append(CHARACTERS[random.nextInt(CHARACTERS.length)])
            }
            result = builder.toString()
        } while (result == invalid)

        return result
    }
}
