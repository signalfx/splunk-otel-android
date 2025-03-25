package com.splunk.rum.integration.agent.api

import com.splunk.rum.integration.agent.api.internal.SplunkRumAgentCore

interface IState {
    val appName: String
    val state: Status
    val endpointConfiguration: EndpointConfiguration
    val deploymentEnvironment: String?
    val isDebugLoggingEnabled: Boolean
    val sessionSamplingRate: Double
    val instrumentedProcessName: String?
}

class State internal constructor(agentConfiguration: AgentConfiguration) : IState {
    override val appName: String = agentConfiguration.appName
    override val state: Status = if (SplunkRumAgentCore.isRunning) Status.Running else Status.NotRunning(cause = Status.NotRunning.Cause.SampledOut)
    override val endpointConfiguration: EndpointConfiguration = agentConfiguration.endpoint
    override val deploymentEnvironment: String? = agentConfiguration.deploymentEnvironment
    override val isDebugLoggingEnabled: Boolean = agentConfiguration.enableDebugLogging
    override val sessionSamplingRate: Double = agentConfiguration.sessionSamplingRate.coerceIn(0.0, 1.0)
    override val instrumentedProcessName: String? = agentConfiguration.instrumentedProcessName
}


class Noop(notRunningCause: Status.NotRunning.Cause = Status.NotRunning.Cause.NotInstalled) : IState {
    override val appName: String = ""
    override val state: Status = Status.NotRunning(notRunningCause)
    override val endpointConfiguration: EndpointConfiguration = EndpointConfiguration("")
    override val deploymentEnvironment: String = ""
    override val isDebugLoggingEnabled: Boolean = false
    override val sessionSamplingRate: Double = 1.0
    override val instrumentedProcessName: String?
        get() = null
}