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

package com.splunk.rum.integration.agent.internal.extension

import com.splunk.sdk.common.utils.extensions.map
import com.splunk.sdk.common.utils.extensions.toJSONArray
import com.splunk.rum.integration.agent.internal.config.RemoteModuleConfiguration
import org.json.JSONArray
import org.json.JSONObject

fun List<RemoteModuleConfiguration>.find(name: String): RemoteModuleConfiguration? {
    return find { it.name == name }
}

internal fun List<RemoteModuleConfiguration>.serializeToString(): String {
    val array = JSONArray()

    for (config in this)
        array.put(JSONObject()
            .put("name", config.name)
            .put("config", config.config)
        )

    return array.toString()
}

internal fun RemoteModuleConfiguration.serializeToString(): String {
    return JSONObject()
        .put("name", name)
        .put("config", config)
        .toString()
}

internal fun String.deserializeToRemoteModuleConfigurations(): List<RemoteModuleConfiguration> {
    return toJSONArray().map { array, index ->
        val item = array.getJSONObject(index)

        RemoteModuleConfiguration(
            name = item.getString("name"),
            config = item.getJSONObject("config")
        )
    }
}

internal fun String.deserializeToRemoteModuleConfiguration(): RemoteModuleConfiguration {
    val jsonObject = JSONObject(this)
    return RemoteModuleConfiguration(
        name = jsonObject.getString("name"),
        config = jsonObject.getJSONObject("config")
    )
}
