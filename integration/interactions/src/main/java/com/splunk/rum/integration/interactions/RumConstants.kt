package com.splunk.rum.integration.interactions

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val COMPONENT_INTERACTIONS = "ui"

    // Interaction types
    const val INTERACTIONS_EVENT_NAME = "action"
    const val INTERACTIONS_ACTION_FOCUS = "focus"
    const val INTERACTIONS_ACTION_SOFT_KEYBOARD = "soft_keyboard"
    const val INTERACTIONS_ACTION_PHONE_BUTTON = "phone_button"
    const val INTERACTIONS_ACTION_DOUBLE_TAP = "double_tap"
    const val INTERACTIONS_ACTION_LONG_PRESS = "long_press"
    const val INTERACTIONS_ACTION_PINCH = "pinch"
    const val INTERACTIONS_ACTION_RAGE_TAP = "rage_tap"
    const val INTERACTIONS_ACTION_ROTATION = "rotation"
    const val INTERACTIONS_ACTION_TAP = "tap"

    // Attribute keys
    val INTERACTIONS_ACTION_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("action.name")
    val INTERACTIONS_TARGET_TYPE_KEY: AttributeKey<String> = AttributeKey.stringKey("target.type")
    val INTERACTIONS_TARGET_XPATH_KEY: AttributeKey<String> = AttributeKey.stringKey("target_xpath")
    val INTERACTIONS_TARGET_ELEMENT_KEY: AttributeKey<String> = AttributeKey.stringKey("target_element")
}