package com.splunk.rum.common.otel

internal object RumConstants {
    const val CRASH_INSTRUMENTATION_SCOPE_NAME = "io.opentelemetry.crash"
    const val DEFAULT_LOG_EVENT_NAME = "splunk.log"
    const val LOG_BODY_ATTRIBUTE = "body"
}