/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.rum.integration.agent.internal.processor

import android.os.SystemClock
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.toInstant
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.common.module.toSplunkString
import com.splunk.rum.integration.agent.internal.AgentIntegration.Companion.modules
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import java.time.Instant
import java.util.concurrent.TimeUnit

class AppStartSpanProcessor : SpanProcessor {

    private var isInitializationReported = false

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        // Hardcoded, temporary solution (hopefully)
        if (!isInitializationReported && span.name == RumConstants.APP_START_NAME) {
            isInitializationReported = true
            reportInitialization(span)
        }
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {}

    override fun isEndRequired(): Boolean = true

    private fun reportInitialization(appStartSpan: Span) {
        val provider =
            SplunkOpenTelemetrySdk.instance?.sdkTracerProvider
                ?: throw IllegalStateException("Unable to report initialization")

        val modules = modules.values

        val firstInitialization =
            modules.minByOrNull { it.initialization?.startTimestamp ?: Long.MAX_VALUE }?.initialization
                ?: throw IllegalStateException("Module initialization did not started")
        val startTimestamp =
            firstInitialization.startTimestamp + SystemClock.elapsedRealtime() - firstInitialization.startElapsed

        val span = provider.get(RumConstants.RUM_TRACER_NAME)
            .spanBuilder("SplunkRum.initialize")
            .setParent(Context.current().with(appStartSpan))
            .setStartTimestamp(startTimestamp.toInstant())
            .startSpan()

        // Actual screen.name as set by SplunkInternalGlobalAttributeSpanProcessor is overwritten here to set it to
        // "unknown" to ensure App Start event doesn't show up under a screen on UI
        span.setAttribute(RumConstants.COMPONENT_KEY, "appstart")
            .setAttribute(RumConstants.SCREEN_NAME_KEY, RumConstants.DEFAULT_SCREEN_NAME)

        val resources = modules.joinToString(",", "[", "]") {
            it.configuration?.toSplunkString()
                ?: "${it.name}.enabled:true"
        }

        span.setAttribute("config_settings", resources)

        for (module in modules) {
            if (module.initialization == null) {
                throw IllegalStateException("Module '${module.name}' initialization has not been started")
            }

            if (module.initialization.endElapsed == null) {
                throw IllegalStateException("Module '${module.name}' is not initialized")
            }

            span.addEvent(
                "${module.name}_initialized",
                module.initialization.run {
                    endElapsed!! - startElapsed
                },
                TimeUnit.MILLISECONDS
            )
        }

        @Suppress("NewApi") // Requires API 26 or core library desugaring
        span.end(Instant.now())
    }
}
