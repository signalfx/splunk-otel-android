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

package com.splunk.rum.integration.okhttp3.manual

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration

/**
 * OkHttp manual module configuration.
 *
 * @property isEnabled Whether the module is enabled.
 * @property capturedRequestHeaders The list of request headers to capture.
 * @property capturedResponseHeaders The list of response headers to capture.
 */
data class OkHttp3ManualModuleConfiguration @JvmOverloads constructor(
    val isEnabled: Boolean = false,
    val capturedRequestHeaders: List<String> = emptyList(),
    val capturedResponseHeaders: List<String> = emptyList()
) : ModuleConfiguration {

    override val name: String = "okHttp3-manual"

    override val attributes: List<Pair<String, String>> = listOf(
        "requestHeaders" to capturedRequestHeaders.joinToString(", "),
        "responseHeaders" to capturedResponseHeaders.joinToString(", ")
    )
}
