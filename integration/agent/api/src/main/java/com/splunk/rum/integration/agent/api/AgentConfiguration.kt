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

package com.splunk.rum.integration.agent.api

import com.splunk.rum.integration.agent.api.user.UserConfiguration
import io.opentelemetry.api.common.Attributes

data class AgentConfiguration(
    val rumAccessToken: String,
    val endpoint: EndpointConfiguration,
    val appName: String,
    var deploymentEnvironment: String? = null,
    var enableDebugLogging: Boolean = false,
    var sessionSamplingRate: Double = 1.0,
    var globalAttributes: Attributes? = null,
    var spanFilter: ((SpanFilterBuilder) -> Unit)? = null,
    var instrumentedProcessName: String? = null,
    var user: UserConfiguration = UserConfiguration()
) {
    internal companion object {
        val noop = AgentConfiguration(
            rumAccessToken = "",
            endpoint = EndpointConfiguration(),
            appName = "",
        )
    }
}
