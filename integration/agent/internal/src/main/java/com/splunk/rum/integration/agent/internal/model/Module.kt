package com.splunk.rum.integration.agent.internal.model

import com.splunk.rum.integration.agent.module.ModuleConfiguration

internal data class Module(
    val name: String,
    val configuration: ModuleConfiguration? = null,
    val initialization: Initialization? = null
) {
    data class Initialization(
        val startTimestamp: Long,
        val startElapsed: Long,
        val endElapsed: Long?
    )
}
