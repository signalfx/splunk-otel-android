package com.splunk.rum.integration.anr

import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor

internal class AnrAttributesExtractor : AttributesExtractor<Array<StackTraceElement>, Void> {

    override fun onStart(
        attributes: AttributesBuilder,
        parentContext: OtelContext,
        stackTrace: Array<StackTraceElement>
    ) {
        attributes.put(RumConstants.COMPONENT_KEY, RumConstants.COMPONENT_ERROR)
        attributes.put(RumConstants.ERROR_KEY, "true")

    }

    override fun onEnd(
        attributes: AttributesBuilder,
        context: OtelContext,
        stackTrace: Array<StackTraceElement>,
        unused: Void?,
        error: Throwable?
    ) {
    }
}
