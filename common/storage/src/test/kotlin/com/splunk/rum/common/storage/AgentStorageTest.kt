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
        val expectedId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"

        storage.writeAppInstallationId(expectedId)
        val result = storage.readAppInstallationId()

        assertEquals(expectedId, result)
    }

    @Test
    fun `writeAppInstallationId overwrites previous value`() {
        val firstId = "fb61a5d8-d420-43fc-89cb-405ca8998892"
        val secondId = "5go7b710-6dqd-21d1-50b4-80c04gd430c0"

        storage.writeAppInstallationId(firstId)
        storage.writeAppInstallationId(secondId)
        val result = storage.readAppInstallationId()

        assertEquals(secondId, result)
    }

    @Test
    fun `readAppInstallationId persists across storage instances`() {
        val testId = "fb61a5d8-d420-43fc-89cb-405ca8998892"

        storage.writeAppInstallationId(testId)

        val newStorage = AgentStorage(context)
        val result = newStorage.readAppInstallationId()

        assertEquals(testId, result)
    }
}
