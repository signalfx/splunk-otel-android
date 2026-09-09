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

package com.splunk.rum.agent.common.storage

import android.content.Context
import com.splunk.rum.common.storage.cache.FileSimplePermanentCache
import com.splunk.rum.common.storage.extensions.noBackupFilesDirCompat
import com.splunk.rum.common.storage.filemanager.FileManagerFactory
import com.splunk.rum.common.storage.preferences.Preferences
import java.io.File

/** Owns the single Preferences loader used by production AgentStorage attachment in this process. */
internal object AgentPreferencesStore {
    @Volatile
    private var instance: Preferences? = null

    private val lock = Any()

    fun preload(context: Context) {
        obtain(context)
    }

    fun obtain(context: Context): Preferences {
        instance?.let { return it }

        return synchronized(lock) {
            instance ?: createPreferences(context.applicationContext ?: context).also {
                instance = it
            }
        }
    }

    internal fun resetForTest() {
        synchronized(lock) {
            instance?.close()
            instance = null
        }
    }

    private fun createPreferences(context: Context): Preferences {
        return Preferences.create {
            FileSimplePermanentCache(
                AgentStorageFiles.preferencesFile(context),
                FileManagerFactory.createPlainFileManager()
            )
        }
    }
}

internal object AgentStorageFiles {
    private const val VERSION = 1

    fun rootDir(context: Context): File = File(context.noBackupFilesDirCompat, "agent")

    fun versionDir(rootDir: File): File = File(rootDir, "$VERSION")

    fun preferencesFile(context: Context): File {
        return File(versionDir(rootDir(context)), "preferences/preferences.dat")
    }
}
