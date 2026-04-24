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

package com.splunk.rum.common.otel.http

import com.splunk.rum.common.storage.IAgentStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AuthHeaderBuilderTest {

    private val logTag = "TestTag"

    @Test
    fun `buildHeaders includes auth token when available`() {
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readRumAccessToken()).thenReturn("test-token-123")

        val headers = AuthHeaderBuilder.buildHeaders(storage, logTag)

        assertEquals(2, headers.size)
        assertTrue(headers.any { it.name == "Content-Type" && it.value == "application/x-protobuf" })
        assertTrue(headers.any { it.name == "X-SF-Token" && it.value == "test-token-123" })
    }

    @Test
    fun `buildHeaders does not crash when token is null`() {
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readRumAccessToken()).thenReturn(null)

        val headers = AuthHeaderBuilder.buildHeaders(storage, logTag)

        assertEquals(1, headers.size)
        assertEquals("Content-Type", headers[0].name)
        assertEquals("application/x-protobuf", headers[0].value)
    }

    @Test
    fun `buildHeaders always includes Content-Type header`() {
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readRumAccessToken()).thenReturn(null)

        val headers = AuthHeaderBuilder.buildHeaders(storage, logTag)

        assertTrue(headers.any { it.name == "Content-Type" && it.value == "application/x-protobuf" })
    }

    @Test
    fun `buildHeaders does not include X-SF-Token when token is null`() {
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readRumAccessToken()).thenReturn(null)

        val headers = AuthHeaderBuilder.buildHeaders(storage, logTag)

        assertTrue(headers.none { it.name == "X-SF-Token" })
    }

    @Test
    fun `buildHeaders does not include X-SF-Token when token is empty`() {
        val storage = mock(IAgentStorage::class.java)
        `when`(storage.readRumAccessToken()).thenReturn("")

        val headers = AuthHeaderBuilder.buildHeaders(storage, logTag)

        assertTrue(headers.any { it.name == "X-SF-Token" && it.value == "" })
    }
}
