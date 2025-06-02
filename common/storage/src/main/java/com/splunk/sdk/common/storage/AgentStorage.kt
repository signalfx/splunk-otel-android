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

package com.splunk.sdk.common.storage

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.cisco.android.common.storage.Storage
import com.cisco.android.common.storage.cache.FilePermanentCache
import com.cisco.android.common.storage.cache.FileSimplePermanentCache
import com.cisco.android.common.storage.extensions.noBackupFilesDirCompat
import com.cisco.android.common.storage.filemanager.EncryptedFileManager
import com.cisco.android.common.storage.filemanager.FileManagerFactory
import com.cisco.android.common.storage.preferences.Preferences
import com.cisco.android.common.utils.runOnBackgroundThread
import com.splunk.sdk.common.storage.extensions.MB
import com.splunk.sdk.common.storage.extensions.statFsFreeSpace
import com.splunk.sdk.common.storage.policy.StoragePolicy
import java.io.File
import org.json.JSONArray

/**
 * Ideas:
 * - Remove the unnecessary folders with single files in them.
 * - Video image storage needs rework!
 * - If we rework the network layer we can make paths private, because they are now used only for the FILE multipart parts.
 *
 * SDK storage structure:
 *  agent/
 *    └─<STORAGE_VERSION>/
 *           ├─logs/
 *           ├─spans/
 *           └─session_replay/
 */
class AgentStorage(context: Context) : IAgentStorage {

    private val preferencesFileManager = FileManagerFactory.createEncryptedFileManagerIfPossible(
        context,
        "Agent-Preferences"
    )
    private val preferences: Preferences

    private val encryptedStorage =
        Storage(FilePermanentCache(FileManagerFactory.createEncryptedFileManagerIfPossible(context, "Agent-Storage")))

    private val rootDir = File(context.noBackupFilesDirCompat, "agent")
    private val agentVersionDir =
        File(rootDir, "$VERSION${if (preferencesFileManager is EncryptedFileManager) "e" else ""}")
    private val preferencesFile = File(agentVersionDir, "preferences/preferences.dat")
    private val logDir = File(agentVersionDir, "logs")
    private val spanDir = File(agentVersionDir, "spans")
    private val sessionReplayDir = File(agentVersionDir, "session_replay")

    init {
        preferences = Preferences(FileSimplePermanentCache(preferencesFile, preferencesFileManager))

        agentVersionDir.mkdirs()
        logDir.mkdirs()
        spanDir.mkdirs()
        sessionReplayDir.mkdirs()
    }

    override val freeSpace: Long
        get() {
            val freeSpace = rootDir.statFsFreeSpace
            Logger.v(TAG, "freeSpace: $freeSpace")
            return freeSpace
        }

    override val rootDirPath: String
        get() {
            val path = rootDir.path
            Logger.v(TAG, "consistentDirPath: $path")
            return path
        }

    override val isStorageFull: Boolean
        get() {
            val isFull = !StoragePolicy(rootDir, 1000.MB, 0.2f, 50.MB).check(freeSpace)
            Logger.v(TAG, "isStorageFull: $isFull")
            return isFull
        }

    override fun writeBaseUrl(value: String) {
        preferences.putString(BASE_URL, value).commit()
    }

    override fun deleteBaseUrl() {
        preferences.remove(BASE_URL)
    }

    override fun readBaseUrl(): String? = preferences.getString(BASE_URL)

    override fun writeSessionReplayBaseUrl(value: String) {
        preferences.putString(SESSION_REPLAY_BASE_URL, value).commit()
    }

    override fun deleteSessionReplayBaseUrl() {
        preferences.remove(SESSION_REPLAY_BASE_URL)
    }

    override fun readSessionReplayBaseUrl(): String? = preferences.getString(SESSION_REPLAY_BASE_URL)

    override fun writeDeviceId(value: String) {
        preferences.putString(DEVICE_ID, value).commit()
    }

    override fun readDeviceId(): String? = preferences.getString(DEVICE_ID)

    override fun writeSessionId(value: String) {
        preferences.putString(SESSION_ID, value).commit()
    }

    override fun readSessionId(): String? = preferences.getString(SESSION_ID)

    override fun deleteSessionId() {
        preferences.remove(SESSION_ID)
    }

    override fun writePreviousSessionId(value: String?) {
        if (value == null) {
            preferences.remove(PREVIOUS_SESSION_ID)
        } else {
            preferences.putString(PREVIOUS_SESSION_ID, value).commit()
        }
    }

    override fun readPreviousSessionId(): String? = preferences.getString(PREVIOUS_SESSION_ID)

    override fun writeSessionValidUntil(value: Long) {
        preferences.putLong(SESSION_VALID_UNTIL, value).commit()
    }

    override fun readSessionValidUntil(): Long? = preferences.getLong(SESSION_VALID_UNTIL)

    override fun deleteSessionValidUntil() {
        preferences.remove(SESSION_VALID_UNTIL)
    }

    override fun writeSessionValidUntilInBackground(value: Long) {
        preferences.putLong(SESSION_VALID_UNTIL_IN_BACKGROUND, value).commit()
    }

    override fun readSessionValidUntilInBackground(): Long? = preferences.getLong(SESSION_VALID_UNTIL_IN_BACKGROUND)

    override fun deleteSessionValidUntilInBackground() {
        preferences.remove(SESSION_VALID_UNTIL_IN_BACKGROUND)
    }

    override fun writeOtelLogData(id: String, data: ByteArray): Boolean {
        val file: File = otelLogDataFile(id)
        val success = encryptedStorage.writeBytes(file, data)
        Logger.d(TAG, "createOtelLogDataFile(): id = $id, success = $success")

        return success
    }

    override fun readOtelLogData(id: String): ByteArray? {
        val file: File = otelLogDataFile(id)
        return encryptedStorage.readBytes(file)
    }

    override fun deleteOtelLogData(id: String) {
        val file: File = otelLogDataFile(id)
        file.delete()
    }

    override fun writeOtelSpanData(id: String, data: ByteArray): Boolean {
        val file: File = otelSpanDataFile(id)
        val success = encryptedStorage.writeBytes(file, data)
        Logger.d(TAG, "writeOtelSpanData(): id = $id, success = $success")

        return success
    }

    override fun readOtelSpanData(id: String): ByteArray? {
        val file: File = otelSpanDataFile(id)
        return encryptedStorage.readBytes(file)
    }

    override fun deleteOtelSpanData(id: String) {
        val file: File = otelSpanDataFile(id)
        file.delete()
    }

    override fun writeOtelSessionReplayData(id: String, data: ByteArray): Boolean {
        val file: File = sessionReplayDataFile(id)
        val success = encryptedStorage.writeBytes(file, data)
        Logger.d(TAG, "writeOtelSessionReplayData(): id = $id, success = $success")

        return success
    }

    override fun readOtelSessionReplayData(id: String): ByteArray? {
        val file: File = sessionReplayDataFile(id)
        return encryptedStorage.readBytes(file)
    }

    override fun deleteOtelSessionReplayData(id: String) {
        val file: File = sessionReplayDataFile(id)
        file.delete()
    }

    override fun addBufferedSpanId(id: String) {
        val ids = getBufferedSpanIds().toMutableSet()
        if (ids.add(id)) {
            val array = JSONArray(ids)
            preferences.putString(SPAN_IDS_KEY, array.toString()).commit()
        }
    }

    override fun getBufferedSpanIds(): List<String> {
        val json = preferences.getString(SPAN_IDS_KEY)
        return if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val array = JSONArray(json)
                List(array.length()) { array.getString(it) }
            } catch (e: Exception) {
                Logger.e(TAG, "getBufferedSpanIds(): spanIds: $e")
                emptyList()
            }
        }
    }

    override fun clearBufferedSpanIds() {
        preferences.remove(SPAN_IDS_KEY)
    }

    private fun otelLogDataFile(id: String) = File(logDir, "$id.dat")
    private fun otelSpanDataFile(id: String) = File(spanDir, "$id.dat")
    private fun sessionReplayDataFile(id: String) = File(sessionReplayDir, "$id.dat")

    fun cleanUpStorage(context: Context): Boolean {
        val files = ArrayList<File>()
        files += rootDir.listFiles()

        val filesToDelete = ArrayList<File>()

        for (file in files) {
            if (file.exists() && file != agentVersionDir) {
                filesToDelete += file
            }
        }

        return if (filesToDelete.isNotEmpty()) {
            runOnBackgroundThread {
                for (file in filesToDelete) {
                    val success = file.deleteRecursively()
                    Logger.w(TAG, "deleteOldDirectories(): file = $file, success = $success")
                }
            }

            false
        } else {
            true
        }
    }

    companion object {
        private const val BASE_URL = "BASE_URL"
        private const val SESSION_REPLAY_BASE_URL = "SESSION_REPLAY_BASE_URL"
        private const val DEVICE_ID = "DEVICE_ID"
        private const val SESSION_ID = "SESSION_ID"
        private const val PREVIOUS_SESSION_ID = "PREVIOUS_SESSION_ID"
        private const val SESSION_VALID_UNTIL = "SESSION_VALID_UNTIL"
        private const val SESSION_VALID_UNTIL_IN_BACKGROUND = "SESSION_VALID_UNTIL_IN_BACKGROUND"
        private const val SPAN_IDS_KEY = "BUFFERED_SPAN_IDS"

        private const val TAG = "AgentStorage"

        /**
         * If storage model changes this version needs to be changed. This will ensure data consistency.
         * The storage will wipe all the legacy data (older version than this one).
         */
        private const val VERSION = 1

        private var instance: IAgentStorage? = null

        fun attach(context: Context): IAgentStorage {
            Logger.v(TAG, "attach(): AgentStorage attached.")
            return instance ?: AgentStorage(context).also { instance = it }
        }
    }
}
