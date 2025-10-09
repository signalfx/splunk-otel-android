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

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class SimpleIdTest {

    @Test
    fun `generate produces string of requested length`() {
        val length = 16
        val id = SimpleId.generate(length)
        assertEquals(length, id.length)
    }

    @Test
    fun `generate only contains valid hexadecimal characters`() {
        val id = SimpleId.generate(4096)
        val validChars = "0123456789abcdef".toSet()
        id.forEach { char ->
            assertTrue("Invalid character: $char", char in validChars)
        }
    }

    @Test
    fun `multiple generated IDs are not all the same`() {
        val ids = mutableSetOf<String>()
        repeat(100) {
            ids += SimpleId.generate(10)
        }
        assertTrue("Expected multiple unique IDs", ids.size > 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generate throws exception for zero length`() {
        SimpleId.generate(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generate throws exception for negative length`() {
        SimpleId.generate(-5)
    }
}
