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

package com.splunk.rum.common.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.splunk.android.common.storage.extensions.noBackupFilesDirCompat
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgentStorageTest {

    private lateinit var context: Context
    private lateinit var storage: AgentStorage
    private lateinit var testStorageDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = AgentStorage(context)
        testStorageDir = File(context.noBackupFilesDirCompat, "agent")
    }

    @After
    fun tearDown() {
        if (testStorageDir.exists()) {
            testStorageDir.deleteRecursively()
        }
    }

    @Test
    fun `readAppInstallationId returns null when not set`() {
        val result = storage.readAppInstallationId()
        assertNull(result)
    }

    @Test
    fun `readAppInstallationId returns stored value`() {
        val expectedId = "6ba7b8109dad11d180b400c04fd430c8"

        storage.writeAppInstallationId(expectedId)
        val result = storage.readAppInstallationId()

        assertEquals(expectedId, result)
    }

    @Test
    fun `writeAppInstallationId overwrites previous value`() {
        val firstId = "fb61a5d8d42043fc89cb405ca8998892"
        val secondId = "5af7b7106dqd21d150b480c04ad430c0"

        storage.writeAppInstallationId(firstId)
        storage.writeAppInstallationId(secondId)
        val result = storage.readAppInstallationId()

        assertEquals(secondId, result)
    }

    @Test
    fun `readAppInstallationId persists across storage instances`() {
        val testId = "fb61a5d8d42043fc89cb405ca8998892"

        storage.writeAppInstallationId(testId)
        storage.commit()

        val newStorage = AgentStorage(context)
        val result = newStorage.readAppInstallationId()

        assertEquals(testId, result)
    }

    @Test
    fun `readRumAccessToken returns null when not set`() {
        val result = storage.readRumAccessToken()
        assertNull(result)
    }

    @Test
    fun `readRumAccessToken returns stored value`() {
        val expectedToken = "test_token_123"

        storage.writeRumAccessToken(expectedToken)
        val result = storage.readRumAccessToken()

        assertEquals(expectedToken, result)
    }

    @Test
    fun `writeRumAccessToken overwrites previous value`() {
        val firstToken = "first_token_123"
        val secondToken = "second_token_123"

        storage.writeRumAccessToken(firstToken)
        storage.writeRumAccessToken(secondToken)
        val result = storage.readRumAccessToken()

        assertEquals(secondToken, result)
    }

    @Test
    fun `readRumAccessToken persists across storage instances`() {
        val testToken = "persistent_token_123"

        storage.writeRumAccessToken(testToken)
        storage.commit()

        val newStorage = AgentStorage(context)
        val result = newStorage.readRumAccessToken()

        assertEquals(testToken, result)
    }

    @Test
    fun `deleteRumAccessToken removes stored value`() {
        val testToken = "token_to_delete_123"

        storage.writeRumAccessToken(testToken)
        storage.deleteRumAccessToken()
        val result = storage.readRumAccessToken()

        assertNull(result)
    }

    @Test
    fun `deleteRumAccessToken handles null gracefully`() {
        storage.deleteRumAccessToken()
        val result = storage.readRumAccessToken()

        assertNull(result)
    }

    // --- Atomic endpoint config tests ---

    @Test
    fun `readEndpointConfig returns null when not set`() {
        assertNull(storage.readEndpointConfig())
    }

    @Test
    fun `writeEndpointConfig and readEndpointConfig roundtrip`() {
        val config = StoredEndpointConfig(
            tracesBaseUrl = "https://rum-ingest.us0.signalfx.com/v1/traces",
            logsBaseUrl = "https://rum-ingest.us0.signalfx.com/v1/logs",
            rumAccessToken = "test-token"
        )

        storage.writeEndpointConfig(config)
        val result = storage.readEndpointConfig()

        assertNotNull(result)
        assertEquals(config, result)
    }

    @Test
    fun `writeEndpointConfig overwrites previous config`() {
        val first = StoredEndpointConfig("https://first.com", null, "token1")
        val second = StoredEndpointConfig("https://second.com", "https://logs.com", "token2")

        storage.writeEndpointConfig(first)
        storage.writeEndpointConfig(second)

        assertEquals(second, storage.readEndpointConfig())
    }

    @Test
    fun `deleteEndpointConfig removes config`() {
        val config = StoredEndpointConfig("https://example.com", null, "token")

        storage.writeEndpointConfig(config)
        storage.deleteEndpointConfig()

        assertNull(storage.readEndpointConfig())
    }

    @Test
    fun `readEndpointConfig persists across storage instances`() {
        val config = StoredEndpointConfig("https://example.com", "https://logs.com", "token")

        storage.writeEndpointConfig(config)
        storage.commit()

        val newStorage = AgentStorage(context)
        assertEquals(config, newStorage.readEndpointConfig())
    }

    @Test
    fun `readEndpointConfig migrates from legacy keys`() {
        @Suppress("DEPRECATION")
        storage.writeTracesBaseUrl("https://legacy-traces.com")
        @Suppress("DEPRECATION")
        storage.writeLogsBaseUrl("https://legacy-logs.com")
        @Suppress("DEPRECATION")
        storage.writeRumAccessToken("legacy-token")

        val result = storage.readEndpointConfig()

        assertNotNull(result)
        assertEquals("https://legacy-traces.com", result!!.tracesBaseUrl)
        assertEquals("https://legacy-logs.com", result.logsBaseUrl)
        assertEquals("legacy-token", result.rumAccessToken)
    }

    @Test
    fun `readEndpointConfig migrates partial legacy state gracefully`() {
        @Suppress("DEPRECATION")
        storage.writeTracesBaseUrl("https://traces-only.com")

        val result = storage.readEndpointConfig()

        assertNotNull(result)
        assertEquals("https://traces-only.com", result!!.tracesBaseUrl)
        assertNull(result.logsBaseUrl)
        assertNull(result.rumAccessToken)
    }

    @Test
    fun `readEndpointConfig returns null when only legacy token exists without URL`() {
        @Suppress("DEPRECATION")
        storage.writeRumAccessToken("orphan-token")

        assertNull(storage.readEndpointConfig())
    }

    @Test
    fun `writeEndpointConfig clears legacy keys`() {
        @Suppress("DEPRECATION")
        storage.writeTracesBaseUrl("https://old.com")
        @Suppress("DEPRECATION")
        storage.writeRumAccessToken("old-token")

        val config = StoredEndpointConfig("https://new.com", null, "new-token")
        storage.writeEndpointConfig(config)

        assertEquals(config, storage.readEndpointConfig())
    }
}
