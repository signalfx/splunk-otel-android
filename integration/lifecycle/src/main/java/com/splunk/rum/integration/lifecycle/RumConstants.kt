package com.splunk.rum.integration.lifecycle

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val COMPONENT_UI_LIFECYCLE = "ui"
    const val UI_LIFECYCLE_LOG_NAME = "device.app.ui.lifecycle"

    // UI lifecycle types
    const val UI_LIFECYCLE_ACTIVITY_TYPE = "Activity"
    const val UI_LIFECYCLE_FRAGMENT_TYPE = "Fragment"

    // Attribute keys
    val ELEMENT_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.type")
    val ELEMENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.name")
    val ELEMENT_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.id")
    val LIFECYCLE_ACTION_KEY: AttributeKey<String> = AttributeKey.stringKey("device.app.ui.element.lifecycle.action")
}