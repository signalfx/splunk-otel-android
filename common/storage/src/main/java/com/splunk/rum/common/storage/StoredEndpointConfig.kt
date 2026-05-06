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

package com.splunk.rum.common.storage

import org.json.JSONObject

/**
 * Represents the persisted endpoint configuration as a single atomic unit.
 * Stored as a single JSON string in preferences to prevent partial-write
 * inconsistencies between URL and token.
 */
data class StoredEndpointConfig(val tracesBaseUrl: String, val logsBaseUrl: String?, val rumAccessToken: String?) {
    fun toJson(): String {
        val json = JSONObject()
        json.put(KEY_TRACES_URL, tracesBaseUrl)
        json.put(KEY_LOGS_URL, logsBaseUrl ?: JSONObject.NULL)
        json.put(KEY_TOKEN, rumAccessToken ?: JSONObject.NULL)
        return json.toString()
    }

    companion object {
        private const val KEY_TRACES_URL = "tracesBaseUrl"
        private const val KEY_LOGS_URL = "logsBaseUrl"
        private const val KEY_TOKEN = "rumAccessToken"

        fun fromJson(json: String): StoredEndpointConfig? = try {
            val obj = JSONObject(json)
            StoredEndpointConfig(
                tracesBaseUrl = obj.getString(KEY_TRACES_URL),
                logsBaseUrl = obj.optString(KEY_LOGS_URL).takeIf { !obj.isNull(KEY_LOGS_URL) },
                rumAccessToken = obj.optString(KEY_TOKEN).takeIf { !obj.isNull(KEY_TOKEN) }
            )
        } catch (_: Exception) {
            null
        }
    }
}
