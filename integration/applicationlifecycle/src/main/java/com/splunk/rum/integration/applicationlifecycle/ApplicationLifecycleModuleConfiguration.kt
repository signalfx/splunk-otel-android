package com.splunk.rum.integration.applicationlifecycle

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration

/**
 * Application lifecycle module configuration.
 *
 * @property isEnabled Whether the module is enabled.
 */
data class ApplicationLifecycleModuleConfiguration @JvmOverloads constructor(val isEnabled: Boolean = true) : ModuleConfiguration {

    override val name: String = "applicationlifecycle"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString()
    )
}