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

package com.splunk.rum.instrumentation.crash

import android.content.Context
import io.opentelemetry.api.OpenTelemetry
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Entry point for installing crash reporting. Register extra [CrashAttributesExtractor]s via
 * [addAttributesExtractor] before [install]; a [RuntimeDetailsExtractor] is added automatically.
 * [install] is idempotent process-wide: only the first call installs a handler.
 */
class CrashReporterInstrumentation {

    private val additionalExtractors = mutableListOf<CrashAttributesExtractor>()

    /** Adds a [CrashAttributesExtractor] that enriches emitted crash events. */
    fun addAttributesExtractor(extractor: CrashAttributesExtractor): CrashReporterInstrumentation {
        additionalExtractors.add(extractor)
        return this
    }

    /** Installs the crash reporting uncaught exception handler. No-ops if already installed. */
    fun install(context: Context, openTelemetry: OpenTelemetry) {
        // Process-wide guard: the handler and battery receiver are global, so a per-instance guard
        // would let a second instance chain another handler and leak another receiver.
        if (!installed.compareAndSet(false, true)) {
            return
        }
        val extractors = additionalExtractors + RuntimeDetailsExtractor.create(context.applicationContext)
        CrashReporter(openTelemetry, extractors).install()
    }

    internal companion object {
        internal val installed = AtomicBoolean(false)
    }
}
