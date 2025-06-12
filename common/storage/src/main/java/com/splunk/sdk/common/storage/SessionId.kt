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

import org.json.JSONObject

data class SessionId(val id: String, val validFrom: Long) {
    companion object {
        fun fromJSONObject(jsonObject: JSONObject): SessionId = SessionId(
            id = jsonObject.getString(ID),
            validFrom = jsonObject.getLong(VALID_FROM)
        )
    }
}

fun SessionId.toJsonObject(): JSONObject = JSONObject()
    .put(ID, id)
    .put(VALID_FROM, validFrom)

private const val ID = "id"
private const val VALID_FROM = "validFrom"
