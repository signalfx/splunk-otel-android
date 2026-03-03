package com.splunk.rum.integration.startup

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val COMPONENT_APP_START = "appstart"
    const val APP_START_SPAN_NAME = "AppStart"
    const val APP_START_INITIALIZE_SPAN_NAME = "SplunkRum.initialize"

    // Application start type
    const val APP_START_TYPE_COLD = "cold"
    const val APP_START_TYPE_WARM = "warm"
    const val APP_START_TYPE_HOT = "hot"

    // Attribute keys
    val APP_START_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("start.type")
    val APP_START_CONFIG_SETTINGS_KEY: AttributeKey<String> = AttributeKey.stringKey("config_settings")
}