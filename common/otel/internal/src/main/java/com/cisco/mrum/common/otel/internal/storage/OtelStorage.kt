/*
 * Copyright 2024 Splunk Inc.
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

package com.cisco.mrum.common.otel.internal.storage

import com.smartlook.sdk.common.storage.preferences.IPreferences

class OtelStorage private constructor(
    private val preferences: IPreferences
) {

    fun writeBaseUrl(value: String) {
        preferences.putString(BASE_URL, value).apply()
    }

    fun deleteBaseUrl() {
        preferences.remove(BASE_URL)
    }

    fun readBaseUrl(): String? = preferences.getString(BASE_URL)

    fun writeDeviceId(value: String) {
        preferences.putString(DEVICE_ID, value).apply()
    }

    fun readDeviceId(): String? = preferences.getString(DEVICE_ID)

    companion object {

        private const val BASE_URL = "LOG_BASE_URL"
        private const val DEVICE_ID = "DEVICE_ID"

        private var instanceInternal: OtelStorage? = null
        fun obtainInstance(preferences: IPreferences): OtelStorage {
            if (instanceInternal == null)
                instanceInternal = OtelStorage(preferences)

            return instanceInternal!!
        }
    }
}
