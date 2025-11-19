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

        val newStorage = AgentStorage(context)
        val result = newStorage.readAppInstallationId()

        assertEquals(testId, result)
    }
}
