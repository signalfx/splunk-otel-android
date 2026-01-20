package com.splunk.rum.integration.crash

import android.app.Application
import android.content.Context
import com.splunk.android.common.utils.AppStateObserver
import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.android.instrumentation.crash.CrashDetails
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import java.util.concurrent.atomic.AtomicBoolean

internal class CrashAttributesExtractor(context: Context) : AttributesExtractor<CrashDetails, Void> {

    private val crashHappened = AtomicBoolean(false)
    private val appStateObserver = AppStateObserver()
    private var appState: String? = null

    init {
        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    override fun onStart(attributes: AttributesBuilder, parentContext: OtelContext, crashDetails: CrashDetails) {
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
        attributes.put(RumConstants.ERROR_KEY, "true")
        appState?.let { attributes.put(RumConstants.APP_STATE_KEY, it) }
    }

    override fun onEnd(
        attributes: AttributesBuilder,
        context: OtelContext,
        crashDetails: CrashDetails,
        unused: Void?,
        error: Throwable?
    ) {
    }

    private inner class AppStateObserverListener : AppStateObserver.Listener {

        override fun onAppStarted() {
            appState = RumConstants.APP_STATE_CREATED
        }

        override fun onAppForegrounded() {
            appState = RumConstants.APP_STATE_FOREGROUND
        }

        override fun onAppBackgrounded() {
            appState = RumConstants.APP_STATE_BACKGROUND
        }

        override fun onAppClosed() {
            appState = RumConstants.APP_STATE_BACKGROUND
        }
    }
}
