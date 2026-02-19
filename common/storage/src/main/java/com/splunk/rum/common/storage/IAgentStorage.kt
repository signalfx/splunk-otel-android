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

import java.io.File

interface IAgentStorage {
    val freeSpace: Long
    val rootDirPath: String
    val isStorageFull: Boolean

    fun writeTracesBaseUrl(value: String)
    fun deleteTracesBaseUrl()
    fun readTracesBaseUrl(): String?

    fun writeLogsBaseUrl(value: String)
    fun deleteLogsBaseUrl()
    fun readLogsBaseUrl(): String?

    fun writeRumAccessToken(value: String)
    fun deleteRumAccessToken()
    fun readRumAccessToken(): String?

    fun writeDeviceId(value: String)
    fun readDeviceId(): String?

    fun writeAppInstallationId(value: String)
    fun readAppInstallationId(): String?

    fun writeSessionId(value: String)
    fun readSessionId(): String?
    fun deleteSessionId()

    fun writeSessionValidUntil(value: Long)
    fun readSessionValidUntil(): Long?
    fun deleteSessionValidUntil()

    fun writeSessionValidUntilInBackground(value: Long)
    fun readSessionValidUntilInBackground(): Long?
    fun deleteSessionValidUntilInBackground()

    fun writeOtelLogData(id: String, data: ByteArray): Boolean
    fun readOtelLogData(id: String): File?
    fun deleteOtelLogData(id: String)

    fun writeOtelSpanData(id: String, data: ByteArray): Boolean
    fun readOtelSpanData(id: String): File?
    fun deleteOtelSpanData(id: String)
    fun addBufferedSpanId(id: String)
    fun getBufferedSpanIds(): List<String>
    fun clearBufferedSpanIds()

    fun writeOtelSessionReplayData(id: String, data: ByteArray): Boolean
    fun readOtelSessionReplayData(id: String): File?
    fun deleteOtelSessionReplayData(id: String)

    fun addBufferedSessionReplayId(id: String)
    fun getBufferedSessionReplayIds(): List<String>
    fun clearBufferedSessionReplayIds()

    fun readSessionIds(): List<SessionId>
    fun writeSessionIds(sessionIds: List<SessionId>)

    fun commit()
}
