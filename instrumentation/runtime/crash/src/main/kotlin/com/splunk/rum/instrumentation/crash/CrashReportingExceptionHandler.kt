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

import io.opentelemetry.sdk.logs.SdkLoggerProvider
import java.util.concurrent.TimeUnit

/**
 * Uncaught exception handler that reports the crash, flushes telemetry synchronously so it survives
 * the imminent process death, and then delegates to any previously installed handler.
 */
internal class CrashReportingExceptionHandler(
    private val crashSender: (CrashDetails) -> Unit,
    private val sdkLoggerProvider: SdkLoggerProvider?,
    private val existingHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            crashSender(CrashDetails(thread, throwable))

            // Do our best to make sure the crash makes it out of the VM before it dies.
            sdkLoggerProvider?.forceFlush()?.join(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            // Always delegate, even if reporting/flushing threw (e.g. OutOfMemoryError).
            existingHandler?.uncaughtException(thread, throwable)
        }
    }

    private companion object {
        private const val FLUSH_TIMEOUT_SECONDS = 10L
    }
}
