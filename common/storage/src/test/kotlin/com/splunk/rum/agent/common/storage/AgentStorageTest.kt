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

package com.splunk.rum.agent.common.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.splunk.rum.common.storage.extensions.noBackupFilesDirCompat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
        testStorageDir = File(context.noBackupFilesDirCompat, "agent")
        resetProcessStorage()
        storage = AgentStorage.attach(context) as AgentStorage
    }

    @After
    fun tearDown() {
        resetProcessStorage()
        if (testStorageDir.exists()) {
            testStorageDir.deleteRecursively()
        }
    }

    @Test
    fun `attach returns the same published instance to concurrent callers`() {
        resetProcessStorage()
        val callerCount = 8
        val executor = Executors.newFixedThreadPool(callerCount)
        val ready = CountDownLatch(callerCount)
        val start = CountDownLatch(1)

        try {
            val results = (1..callerCount).map {
                executor.submit<IAgentStorage> {
                    ready.countDown()
                    start.await()
                    AgentStorage.attach(context)
                }
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            val instances = results.map { it.get(5, TimeUnit.SECONDS) }

            instances.forEach { assertSame(instances.first(), it) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `attach candidates use the process preferences owner`() {
        resetProcessStorage()
        AgentPreferencesStore.preload(context)

        val attachedStorage = AgentStorage.attach(context)
        val preferencesField = AgentStorage::class.java.getDeclaredField("preferences")
        preferencesField.isAccessible = true

        assertSame(AgentPreferencesStore.obtain(context), preferencesField.get(attachedStorage))
    }

    @Test
    fun `preference store returns one owner to concurrent callers`() {
        resetProcessStorage()
        val callerCount = 8
        val executor = Executors.newFixedThreadPool(callerCount)
        val ready = CountDownLatch(callerCount)
        val start = CountDownLatch(1)

        try {
            val results = (1..callerCount).map {
                executor.submit {
                    ready.countDown()
                    start.await()
                    AgentPreferencesStore.obtain(context)
                }
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            val preferences = results.map { it.get(5, TimeUnit.SECONDS) }

            preferences.forEach { assertSame(preferences.first(), it) }
        } finally {
            executor.shutdownNow()
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

        resetProcessStorage()
        val newStorage = AgentStorage.attach(context)
        val result = newStorage.readAppInstallationId()

        assertEquals(testId, result)
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
            sessionReplayBaseUrl = "https://rum-ingest.us0.signalfx.com/v1/logs",
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

        resetProcessStorage()
        val newStorage = AgentStorage.attach(context)
        assertEquals(config, newStorage.readEndpointConfig())
    }

    // --- Legacy migration tests ---

    @Test
    fun `readEndpointConfig migrates from legacy keys`() {
        storage.writeLegacyEndpointKeys(
            tracesBaseUrl = "https://legacy-traces.com",
            logsBaseUrl = "https://legacy-logs.com",
            rumAccessToken = "legacy-token"
        )

        val result = storage.readEndpointConfig()

        assertNotNull(result)
        assertEquals("https://legacy-traces.com", result!!.tracesBaseUrl)
        assertEquals("https://legacy-logs.com", result.sessionReplayBaseUrl)
        assertEquals("legacy-token", result.rumAccessToken)
    }

    @Test
    fun `readEndpointConfig migrates partial legacy state gracefully`() {
        storage.writeLegacyEndpointKeys(tracesBaseUrl = "https://traces-only.com")

        val result = storage.readEndpointConfig()

        assertNotNull(result)
        assertEquals("https://traces-only.com", result!!.tracesBaseUrl)
        assertNull(result.sessionReplayBaseUrl)
        assertNull(result.rumAccessToken)
    }

    @Test
    fun `readEndpointConfig returns null when only legacy token exists without URL`() {
        storage.writeLegacyEndpointKeys(rumAccessToken = "orphan-token")

        assertNull(storage.readEndpointConfig())
    }

    @Test
    fun `writeEndpointConfig clears legacy keys`() {
        storage.writeLegacyEndpointKeys(
            tracesBaseUrl = "https://old.com",
            rumAccessToken = "old-token"
        )

        val config = StoredEndpointConfig("https://new.com", null, "new-token")
        storage.writeEndpointConfig(config)

        assertEquals(config, storage.readEndpointConfig())
    }

    private fun resetProcessStorage() {
        AgentStorage.resetForTest()
        AgentPreferencesStore.resetForTest()
    }
}
