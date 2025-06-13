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

object SessionId {

    private const val LENGTH = 32

    /**
     * Generates a random lowercase hexadecimal session ID with length of 32 characters.
     *
     * @return The generated ID:
     * - Is exactly 32 characters long.
     * - Consists only of characters `0` through `9` and `a` through `f`.
     * - Is never composed entirely of zeroes (e.g., "000000...").
     */
    fun generate() = SimpleId.generate(LENGTH)
}
