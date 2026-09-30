/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.crash

import android.os.SystemClock
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.agent.common.otel.span.DurableSpanPersistenceCoordinator
import com.splunk.rum.common.logger.Logger
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.TimeUnit

/**
 * Library integration API for persisting a fatal React Native JavaScript error as a dedicated
 * crash span before React Native terminates the process.
 */
object ReactNativeFatalCrashReporter {

    /**
     * Creates and locally persists the fatal crash span, then arms native duplicate suppression.
     *
     * This method is intended for a blocking React Native bridge method. It performs no network
     * wait and returns `true` only when the exact span reached durable local storage and the
     * in-process suppression marker was armed.
     */
    @JvmStatic
    @JvmOverloads
    fun recordCrashSync(
        message: String,
        stacktrace: String?,
        attributes: Attributes = Attributes.empty(),
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
    ): Boolean {
        val sdk = SplunkOpenTelemetrySdk.instance ?: return false
        val provider = sdk.sdkTracerProvider ?: return false
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis.coerceAtLeast(0)
        var spanId: String? = null

        return try {
            val type = attributes.get(GlobalRumConstants.EXCEPTION_TYPE_KEY) ?: DEFAULT_EXCEPTION_TYPE
            val spanBuilder = provider.get(INSTRUMENTATION_SCOPE_NAME)
                .spanBuilder(SPAN_NAME)
                .setAllAttributes(attributes)
                .setAttribute(GlobalRumConstants.COMPONENT_KEY, GlobalRumConstants.COMPONENT_CRASH)
                .setAttribute(ERROR_KEY, true)
                .setAttribute(GlobalRumConstants.EXCEPTION_TYPE_KEY, type)
                .setAttribute(GlobalRumConstants.EXCEPTION_MESSAGE_KEY, message)

            stacktrace?.let {
                spanBuilder.setAttribute(GlobalRumConstants.EXCEPTION_STACKTRACE_KEY, it)
            }

            val span = spanBuilder.startSpan()
            spanId = span.spanContext.spanId
            if (!DurableSpanPersistenceCoordinator.register(spanId)) {
                span.end()
                return false
            }

            span.end()
            val remainingForFlush = deadline - SystemClock.elapsedRealtime()
            if (remainingForFlush <= 0) return false

            val flushResult = provider.forceFlush()
            flushResult.join(remainingForFlush, TimeUnit.MILLISECONDS)
            val remainingForAcknowledgement = deadline - SystemClock.elapsedRealtime()
            if (!flushResult.isSuccess || remainingForAcknowledgement < 0) return false

            val persisted = DurableSpanPersistenceCoordinator.await(
                spanId,
                remainingForAcknowledgement
            )
            if (persisted) {
                ReactNativeFatalCrashState.marker.arm(spanId)
            }
            persisted
        } catch (t: Throwable) {
            Logger.e(TAG, "Failed to persist React Native fatal crash", t)
            false
        } finally {
            spanId?.let(DurableSpanPersistenceCoordinator::cancel)
        }
    }

    private const val TAG = "ReactNativeFatalCrashReporter"
    private const val INSTRUMENTATION_SCOPE_NAME = "splunk-crash-report"
    private const val SPAN_NAME = "SplunkCrashReport"
    private const val DEFAULT_EXCEPTION_TYPE = "Error"
    private const val DEFAULT_TIMEOUT_MILLIS = 2_000L
    private val ERROR_KEY: AttributeKey<Boolean> = AttributeKey.booleanKey("error")
}
