package com.splunk.sdk.common.otel.internal

import io.opentelemetry.api.common.AttributeKey

object RumConstants {
    const val RUM_TRACER_NAME: String = "SplunkRum"
    val WORKFLOW_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("workflow.name")
}