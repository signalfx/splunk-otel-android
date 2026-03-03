package com.splunk.rum.integration.customtracking

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val ERROR_TRUE_VALUE = "true"
    const val COMPONENT_CUSTOM_EVENT = "custom-event"
    const val COMPONENT_CUSTOM_WORKFLOW = "custom-workflow"

    // Attribute keys
    val WORKFLOW_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("workflow.name")
}