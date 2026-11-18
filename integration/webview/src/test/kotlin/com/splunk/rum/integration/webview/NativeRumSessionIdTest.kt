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

package com.splunk.rum.integration.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeRumSessionIdTest {

    @Test
    fun `bridge follows session A then no session then B`() {
        var id: String? = "A"
        var metadata: String? = "metadata-A"
        val bridge = NativeRumSessionId({ id }, { metadata })

        assertEquals("A", bridge.nativeSessionId)
        assertEquals("metadata-A", bridge.nativeSessionMetadata)

        id = null
        metadata = null
        assertEquals("", bridge.nativeSessionId)
        assertNull(bridge.nativeSessionMetadata)

        id = "B"
        metadata = "metadata-B"
        assertEquals("B", bridge.nativeSessionId)
        assertEquals("metadata-B", bridge.nativeSessionMetadata)
    }

    @Test
    fun `bridge reports no session before SDK installation`() {
        val bridge = NativeRumSessionId()

        assertEquals("", bridge.nativeSessionId)
        assertNull(bridge.nativeSessionMetadata)
    }
}
