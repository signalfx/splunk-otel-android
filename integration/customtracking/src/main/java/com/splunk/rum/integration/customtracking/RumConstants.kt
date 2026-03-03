package com.splunk.rum.integration.customtracking

import io.opentelemetry.api.common.AttributeKey

object RumConstants {

    const val COMPONENT_CUSTOM_EVENT = "custom-event"

    // Attribute keys
    val WORKFLOW_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("workflow.name")
}