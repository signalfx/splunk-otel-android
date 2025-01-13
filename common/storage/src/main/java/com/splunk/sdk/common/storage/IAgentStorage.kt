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

import java.io.File

interface IAgentStorage {
    val freeSpace: Long
    val rootDirPath: String
    val isStorageFull: Boolean

    fun writeBaseUrl(value: String)
    fun deleteBaseUrl()
    fun readBaseUrl(): String?

    fun writeDeviceId(value: String)
    fun readDeviceId(): String?

    fun writeAnonId(value: String)
    fun readAnonId(): String?
    fun deleteAnonId()

    fun writeSessionId(value: String)
    fun readSessionId(): String?
    fun deleteSessionId()

    fun writeSessionValidUntil(value: Long)
    fun readSessionValidUntil(): Long?
    fun deleteSessionValidUntil()

    fun writeSessionValidUntilInBackground(value: Long)
    fun readSessionValidUntilInBackground(): Long?
    fun deleteSessionValidUntilInBackground()

    fun createOtelLogDataFile(id: String): File
    fun getOtelLogDataFile(id: String): File
    fun createOtelSpanDataFile(id: String): File
    fun getOtelSpanDataFile(id: String): File
}
