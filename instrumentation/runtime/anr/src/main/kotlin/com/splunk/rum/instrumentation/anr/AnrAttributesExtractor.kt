/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.anr

import io.opentelemetry.api.common.AttributesBuilder

/**
 * Extracts additional attributes to attach to an ANR (application not responding) event.
 *
 * Implementations must not throw; the reporter isolates failures, but extractors run on the
 * watchdog thread while the main thread is stalled, so they should be fast and allocation-light.
 */
fun interface AnrAttributesExtractor {

    /**
     * Adds attributes derived from the main thread's [stackTrace] to [attributes].
     */
    fun extract(attributes: AttributesBuilder, stackTrace: Array<StackTraceElement>)
}
