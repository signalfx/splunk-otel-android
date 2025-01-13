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

/**
 * Ideas:
 * - Remove the unnecessary folders with single files in them.
 * - Video image storage needs rework!
 * - If we rework the network layer we can make paths private, because they are now used only for the FILE multipart parts.
 *
 * SDK storage structure:
 *  agent/
 *    └─<STORAGE_VERSION>/
 *           └─dataChunks/
 *              └─<data_chunk_id>/
 *                ├──metrics.dat
 *                ├──dataChunk.dat
 *                ├──wireframe.dat
 *                └──video/
 *                  ├──<frame_number>.jpg
 *                  ├──video.mp4
 *                  └──metadata.dat
 */
class AgentStorage(context: Context) : IAgentStorage {

    private val preferencesFileManager = FileManagerFactory.createEncryptedFileManagerIfPossible(context, "Agent-Preferences")
    private val preferences: Preferences

    private val encryptedStorage = Storage(FilePermanentCache(FileManagerFactory.createEncryptedFileManagerIfPossible(context, "Agent-Storage")))

    private val rootDir = File(context.noBackupFilesDirCompat, "agent")
    private val agentVersionDir = File(rootDir, "$VERSION${if (preferencesFileManager is EncryptedFileManager) "e" else ""}")
    private val preferencesFile = File(agentVersionDir, "preferences/preferences.dat")
    private val logDir = File(agentVersionDir, "logs")
    private val spanDir = File(agentVersionDir, "spanDir")

    init {
        preferences = Preferences(FileSimplePermanentCache(preferencesFile, preferencesFileManager))

        agentVersionDir.mkdirs()
        logDir.mkdirs()
        spanDir.mkdirs()
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

    override fun writeDeviceId(value: String) {
        preferences.putString(DEVICE_ID, value).commit()
    }

    override fun readDeviceId(): String? = preferences.getString(DEVICE_ID)

    override fun writeAnonId(value: String) {
        TODO("Not yet implemented")
    }

    override fun readAnonId(): String? {
        TODO("Not yet implemented")
    }

    override fun deleteAnonId() {
        TODO("Not yet implemented")
    }

    override fun writeSessionId(value: String) {
        TODO("Not yet implemented")
    }

    override fun readSessionId(): String? {
        TODO("Not yet implemented")
    }

    override fun deleteSessionId() {
        TODO("Not yet implemented")
    }

    override fun writeSessionValidUntil(value: Long) {
        TODO("Not yet implemented")
    }

    override fun readSessionValidUntil(): Long? {
        TODO("Not yet implemented")
    }

    override fun deleteSessionValidUntil() {
        TODO("Not yet implemented")
    }

    override fun writeSessionValidUntilInBackground(value: Long) {
        TODO("Not yet implemented")
    }

    override fun readSessionValidUntilInBackground(): Long? {
        TODO("Not yet implemented")
    }

    override fun deleteSessionValidUntilInBackground() {
        TODO("Not yet implemented")
    }

    override fun createOtelLogDataFile(id: String): File {
        TODO("Not yet implemented")
    }

    override fun getOtelLogDataFile(id: String): File {
        TODO("Not yet implemented")
    }

    override fun createOtelSpanDataFile(id: String): File {
        TODO("Not yet implemented")
    }

    override fun getOtelSpanDataFile(id: String): File {
        TODO("Not yet implemented")
    }

    fun cleanUpStorage(context: Context): Boolean {
        val files = ArrayList<File>()
        files += rootDir.listFiles()

        val filesToDelete = ArrayList<File>()

        for (file in files)
            if (file.exists() && file != agentVersionDir)
                filesToDelete += file

        return if (filesToDelete.isNotEmpty()) {
            runOnBackgroundThread {
                for (file in filesToDelete) {
                    val success = file.deleteRecursively()
                    Logger.w(TAG, "deleteOldDirectories(): file = $file, success = $success")
                }
            }

            false
        } else
            true
    }

    companion object {
        private const val BASE_URL = "LOG_BASE_URL"
        private const val DEVICE_ID = "DEVICE_ID"

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
