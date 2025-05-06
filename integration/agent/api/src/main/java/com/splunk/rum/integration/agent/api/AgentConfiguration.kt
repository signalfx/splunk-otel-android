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

import com.splunk.rum.integration.agent.api.attributes.MutableAttributes
import com.splunk.rum.integration.agent.api.user.UserConfiguration
import io.opentelemetry.sdk.trace.data.SpanData

/**
 * TODO: Fill in the documentation.
 *
 * @property spanInterceptor A function to intercept and optionally modify spans before they are exported.
 * Can return `null` to drop a span. Default is `null` (no interception).
 *
 * If span interception and modification are required, consider using the [toMutableSpanData] extension method:
 * ```kotlin
 * val mutableSpanData: MutableSpanData = spanData.toMutableSpanData()
 * ```
 * This method allows you to convert a `SpanData` instance into a `MutableSpanData`, enabling you to modify span attributes,
 * status, and other properties as needed during the interception process.
 */
data class AgentConfiguration(
    val endpoint: EndpointConfiguration,
    var appName: String,
    var deploymentEnvironment: String,
    var appVersion: String? = null,
    val enableDebugLogging: Boolean = false,
    val sessionSamplingRate: Double = 1.0, // TODO move to session
    val globalAttributes: MutableAttributes = MutableAttributes(),
    val spanInterceptor: ((SpanData) -> SpanData?)? = null,
    val user: UserConfiguration = UserConfiguration(),
    val session: Any? = null, // TODO,
    val instrumentedProcessName: String? = null,
    val deferredUntilForeground: Boolean = false,
) {
    internal companion object {
        val noop = AgentConfiguration(
            endpoint = EndpointConfiguration(),
            appName = "",
            deploymentEnvironment = ""
        )
    }
}
