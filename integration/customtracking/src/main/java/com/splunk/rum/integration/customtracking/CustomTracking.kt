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

package com.splunk.rum.integration.customtracking

import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.createZeroLengthSpan
import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import java.time.Instant

class CustomTracking internal constructor() {

    /**
     * Add a custom event. This can be useful to capture business events.
     *
     * <p>This event will be turned into a Span and sent to the RUM ingest along with other,
     * auto-generated spans.
     *
     * @param name The name of the event.
     * @param attributes Any {@link Attributes} to associate with the event.
     */
    @JvmOverloads
    fun trackCustomEvent(name: String, attributes: Attributes = Attributes.empty()) {
        val tracer = getTracer() ?: return
        tracer.spanBuilder(name).setAllAttributes(attributes).createZeroLengthSpan()
    }

    /**
     * Start a Span to track a named workflow.
     *
     * @param workflowName The name of the workflow to start.
     * @return A {@link Span} that has been started.
     */
    fun trackWorkflow(workflowName: String): Span? {
        val tracer = getTracer() ?: return null
        return tracer.spanBuilder(workflowName)
            .setAttribute(RumConstants.WORKFLOW_NAME_KEY, workflowName)
            .startSpan()
    }

    /**
     * Add a custom exception to RUM monitoring. This can be useful for tracking custom error
     * handling in your application.
     *
     *
     * This event will be turned into a Span and sent to the RUM ingest along with other,
     * auto-generated spans.
     *
     * @param throwable A [Throwable] associated with this event.
     * @param attributes Any [Attributes] to associate with the event.
     */
    @JvmOverloads
    fun trackException(throwable: Throwable, attributes: Attributes? = null) {
        val tracer = getTracer() ?: return
        val spanBuilder = tracer.spanBuilder(throwable.javaClass.simpleName)
        attributes?.let {
            spanBuilder.setAllAttributes(it)
        }
        @Suppress("NewApi") // Requires API 26 or core library desugaring
        val timestamp = Instant.now()
        spanBuilder.setAttribute(RumConstants.COMPONENT_KEY, RumConstants.COMPONENT_ERROR)
            .setAttribute(RumConstants.ERROR_KEY, "true")
            .setStartTimestamp(timestamp)
            .startSpan()
            .recordException(throwable)
            .end(timestamp)
    }

    /**
     * Retrieves the Tracer instance for the application.
     *
     * @return A Tracer instance if available, or null if the OpenTelemetry instance is null.
     */
    private fun getTracer(): Tracer? =
        SplunkOpenTelemetrySdk.instance?.sdkTracerProvider?.get(RumConstants.RUM_TRACER_NAME).also {
            if (it == null) {
                Logger.e(
                    TAG,
                    "Opentelemetry instance is null. Cannot track custom events/workflow."
                )
            }
        }

    companion object {

        private const val TAG = "CustomTracking"

        /**
         * The instance of [CustomTracking].
         */
        @JvmStatic
        val instance by lazy { CustomTracking() }
    }
}
