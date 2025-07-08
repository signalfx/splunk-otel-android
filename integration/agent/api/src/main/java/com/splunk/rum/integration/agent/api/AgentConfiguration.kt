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

import com.splunk.rum.integration.agent.api.session.SessionConfiguration
import com.splunk.rum.integration.agent.api.spaninterceptor.toMutableSpanData
import com.splunk.rum.integration.agent.api.user.UserConfiguration
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.SpanData

/**
 * Configuration for the Splunk RUM Agent.
 *
 * @property endpoint Configuration for the Splunk RUM Agent's endpoint.
 * @property appName The name of the application.
 * @property deploymentEnvironment The deployment environment of the application.
 * @property appVersion The version of the application.
 * @property enableDebugLogging Whether to enable debug logging.
 * @property sessionSamplingRate The sampling rate for sessions. Default is 1.0.
 * @property globalAttributes Global attributes to be added to all spans.
 * @property spanInterceptor A function to intercept and optionally modify spans before they are exported.
 * Can return `null` to drop a span. Default is `null` (no interception).
 *
 * If span interception and modification are required, consider using the [SpanData.toMutableSpanData] extension method:
 * ```kotlin
 * val mutableSpanData: MutableSpanData = spanData.toMutableSpanData()
 * ```
 * This method allows you to convert a `SpanData` instance into a `MutableSpanData`, enabling you to modify span attributes,
 * status, and other properties as needed during the interception process.
 * @property user Configuration for the user.
 * @property session Additional session information.
 * @property instrumentedProcessName The name of the instrumented process.
 * @property deferredUntilForeground Whether to defer tracing until the app is brought to the foreground.
 */
data class AgentConfiguration @JvmOverloads constructor(
    val endpoint: EndpointConfiguration,
    val appName: String,
    val deploymentEnvironment: String,
    val appVersion: String? = null,
    val enableDebugLogging: Boolean = false,
    val globalAttributes: Attributes = Attributes.empty(),
    val spanInterceptor: ((SpanData) -> SpanData?)? = null,
    val user: UserConfiguration = UserConfiguration(),
    val session: SessionConfiguration = SessionConfiguration(),
    val instrumentedProcessName: String? = null,
    val deferredUntilForeground: Boolean = false
) {
    internal companion object {
        val noop = AgentConfiguration(
            endpoint = EndpointConfiguration(),
            appName = "",
            deploymentEnvironment = ""
        )
    }
}
