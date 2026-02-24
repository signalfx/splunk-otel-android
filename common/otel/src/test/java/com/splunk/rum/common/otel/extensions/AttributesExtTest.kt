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

package com.splunk.rum.common.otel.extensions

import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Test

class AttributesExtTest {

    @Test
    fun `joinToString should format attributes correctly`() {
        val attributes = Attributes.builder()
            .put("key1", "value1")
            .put("key2", true)
            .put("key3", 123L)
            .build()

        val result = attributes.joinToString()

        assertEquals("key1=value1,key2=true,key3=123", result)
    }

    @Test
    fun `joinToString with custom arguments should format attributes correctly`() {
        val attributes = Attributes.builder()
            .put("key1", "value1")
            .put("key2", true)
            .build()

        val result = attributes.joinToString(
            separator = ";",
            prefix = "[",
            postfix = "]",
            transform = { key, value -> "$key:$value" }
        )

        assertEquals("[key1:value1;key2:true]", result)
    }

    @Test
    fun `joinToString with empty attributes should return an empty string`() {
        val attributes = Attributes.empty()

        val result = attributes.joinToString()

        assertEquals("", result)
    }
}
