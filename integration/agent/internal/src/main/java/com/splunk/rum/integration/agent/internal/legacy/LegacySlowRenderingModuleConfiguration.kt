package com.splunk.rum.integration.agent.internal.legacy

import com.splunk.rum.integration.agent.module.ModuleConfiguration

@Deprecated("Only to support legacy API, can be removed with legacy API.")
class LegacySlowRenderingModuleConfiguration(val isEnabled: Boolean = true) : ModuleConfiguration {

    override val name: String = "slowrendering"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString()
    )
}