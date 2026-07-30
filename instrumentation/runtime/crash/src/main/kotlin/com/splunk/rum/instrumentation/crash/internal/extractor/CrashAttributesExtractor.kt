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

package com.splunk.rum.instrumentation.crash.internal.extractor

import io.opentelemetry.api.common.AttributesBuilder

/**
 * Extracts additional attributes to attach to a crash event.
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * Implementations must not throw; the crash reporter isolates failures, but extractors run while the
 * process is already terminating, so they should be fast and allocation-light.
 */
fun interface CrashAttributesExtractor {

    /**
     * Adds attributes derived from [crashDetails] to [attributes].
     */
    fun extract(attributes: AttributesBuilder, crashDetails: CrashDetails)
}
