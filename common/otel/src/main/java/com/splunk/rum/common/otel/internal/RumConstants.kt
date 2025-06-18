/*
 * Copyright 2025 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum.common.otel.internal

import io.opentelemetry.api.common.AttributeKey

object RumConstants {
    const val RUM_TRACER_NAME: String = "SplunkRum"
    const val COMPONENT_ERROR: String = "error"
    const val COMPONENT_CRASH: String = "crash"
    const val COMPONENT_HTTP: String = "http"
    const val SERVER_TIMING_HEADER = "server-timing"
    const val CRASH_INSTRUMENTATION_SCOPE_NAME = "io.opentelemetry.crash"

    val WORKFLOW_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("workflow.name")
    val COMPONENT_KEY: AttributeKey<String> = AttributeKey.stringKey("component")

    val APPLICATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("service.application_id")
    val APP_VERSION_CODE_KEY: AttributeKey<String> = AttributeKey.stringKey("service.version_code")

    val LINK_SPAN_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("link.spanId")
    val LINK_TRACE_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("link.traceId")

    val SPLUNK_BUILD_ID: AttributeKey<String> = AttributeKey.stringKey("splunk.build_id")

    val LOG_EVENT_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("event.name")
    const val DEFAULT_LOG_EVENT_NAME = "log"
}
