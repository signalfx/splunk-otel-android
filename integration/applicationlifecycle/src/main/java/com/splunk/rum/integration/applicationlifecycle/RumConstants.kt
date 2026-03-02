package com.splunk.rum.integration.applicationlifecycle

import io.opentelemetry.api.common.AttributeKey

object RumConstants {

    const val COMPONENT_APP_LIFECYCLE = "app-lifecycle"
    const val APP_LIFECYCLE_LOG_NAME = "device.app.lifecycle"

    // Application lifecycle states
    const val APP_STATE_CREATED = "created"
    const val APP_STATE_FOREGROUND = "foreground"
    const val APP_STATE_BACKGROUND = "background"

    // Attribute key
    val APP_STATE_KEY: AttributeKey<String> = AttributeKey.stringKey("android.app.state")
}