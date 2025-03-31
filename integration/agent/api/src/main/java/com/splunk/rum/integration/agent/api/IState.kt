package com.splunk.rum.integration.agent.api

import com.splunk.rum.integration.agent.api.internal.SplunkRumAgentCore

interface IState {
    val status: Status
    val appName: String
    val appVersion: String
    val endpointConfiguration: EndpointConfiguration
    val deploymentEnvironment: String
    val isDebugLoggingEnabled: Boolean
    val sessionSamplingRate: Double
}

class State internal constructor(agentConfiguration: AgentConfiguration) : IState {
    override val appName: String = agentConfiguration.appName
    override val appVersion: String = "0.0.0"
    override val status: Status = if (SplunkRumAgentCore.isRunning) Status.Running else Status.NotRunning(cause = Status.NotRunning.Cause.SampledOut)
    override val endpointConfiguration: EndpointConfiguration = agentConfiguration.endpoint
    override val deploymentEnvironment: String = agentConfiguration.deploymentEnvironment
    override val isDebugLoggingEnabled: Boolean = agentConfiguration.enableDebugLogging
    override val sessionSamplingRate: Double = agentConfiguration.sessionSamplingRate.coerceIn(0.0, 1.0)
}

object Noop : IState {
    override val appName: String = ""
    override val appVersion: String = "0.0.0"
    override val status: Status = Status.NotRunning(cause = Status.NotRunning.Cause.NotInstalled)
    override val endpointConfiguration: EndpointConfiguration = EndpointConfiguration("", "")
    override val deploymentEnvironment: String = ""
    override val isDebugLoggingEnabled: Boolean = false
    override val sessionSamplingRate: Double = 1.0
}