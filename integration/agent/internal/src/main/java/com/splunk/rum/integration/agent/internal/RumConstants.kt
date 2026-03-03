package com.splunk.rum.integration.agent.internal

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {
    const val SESSION_START_EVENT_NAME = "session.start"

    // Attribute key
    val SESSION_RUM_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rumSessionId")
    val USER_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("user.anonymous_id")
    val SCRIPT_INSTANCE_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.scriptInstance")
    val APPLICATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("service.application_id")
    val APP_VERSION_CODE_KEY: AttributeKey<String> = AttributeKey.stringKey("service.version_code")
    val SPLUNK_BUILD_ID: AttributeKey<String> = AttributeKey.stringKey("splunk.build_id")
}