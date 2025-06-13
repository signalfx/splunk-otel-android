package com.splunk.rum.integration.crash

import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.android.instrumentation.crash.CrashDetails
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import java.util.concurrent.atomic.AtomicBoolean

internal class CrashAttributesExtractor : AttributesExtractor<CrashDetails, Void> {

    private val crashHappened = AtomicBoolean(false)

    override fun onStart(attributes: AttributesBuilder, parentContext: Context, crashDetails: CrashDetails) {
        // Set component=crash only for the first error that arrives here
        // When multiple threads fail at roughly the same time (e.g. because of an OOM error),
        // the first error to arrive here is actually responsible for crashing the app; and all
        // the others that are captured before OS actually kills the process are just additional
        // info (component=error)
        val component = if (crashHappened.compareAndSet(false, true)) {
            RumConstants.COMPONENT_CRASH
        } else {
            RumConstants.COMPONENT_ERROR
        }
        attributes.put(RumConstants.COMPONENT_KEY, component)
    }

    override fun onEnd(
        attributes: AttributesBuilder,
        context: Context,
        crashDetails: CrashDetails,
        unused: Void?,
        error: Throwable?
    ) {
    }
}
