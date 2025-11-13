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

package com.splunk.rum.integration.agent.api

import com.splunk.rum.integration.agent.api.internal.SplunkRumAgentCore
import java.util.concurrent.atomic.AtomicReference

interface IState {
    val appName: String
    val status: Status
    val appVersion: String
    val endpointConfiguration: EndpointConfiguration?
    val deploymentEnvironment: String
    val isDebugLoggingEnabled: Boolean
    val instrumentedProcessName: String?
    val deferredUntilForeground: Boolean
}

class State internal constructor(
    agentConfiguration: AgentConfiguration,
    private val endpointRef: AtomicReference<EndpointConfiguration?>
) : IState {
    override val appName: String = agentConfiguration.appName
    override val appVersion: String = "0.0.0"
    override val status: Status = if (SplunkRumAgentCore.isRunning) {
        Status.Running
    } else {
        Status.NotRunning.SampledOut
    }
    override val endpointConfiguration: EndpointConfiguration?
        get() = endpointRef.get()
    override val deploymentEnvironment: String = agentConfiguration.deploymentEnvironment
    override val isDebugLoggingEnabled: Boolean = agentConfiguration.enableDebugLogging
    override val instrumentedProcessName: String? = agentConfiguration.instrumentedProcessName
    override val deferredUntilForeground: Boolean = agentConfiguration.deferredUntilForeground
}

class Noop(notRunningCause: Status.NotRunning = Status.NotRunning.NotInstalled) : IState {
    override val appName: String = ""
    override val status: Status = notRunningCause
    override val endpointConfiguration: EndpointConfiguration? = null
    override val appVersion: String = "0.0.0"
    override val deploymentEnvironment: String = ""
    override val isDebugLoggingEnabled: Boolean = false
    override val instrumentedProcessName: String?
        get() = null
    override val deferredUntilForeground: Boolean = false
}
