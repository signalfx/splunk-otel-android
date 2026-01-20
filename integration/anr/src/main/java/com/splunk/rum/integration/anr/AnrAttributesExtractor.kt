package com.splunk.rum.integration.anr

import android.app.Application
import android.content.Context
import com.splunk.android.common.utils.AppStateObserver
import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor

internal class AnrAttributesExtractor(context: Context) : AttributesExtractor<Array<StackTraceElement>, Void> {

    private val appStateObserver = AppStateObserver()
    private var appState: String? = null

    init {
        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    override fun onStart(
        attributes: AttributesBuilder,
        parentContext: OtelContext,
        stackTrace: Array<StackTraceElement>
    ) {
        attributes.put(RumConstants.COMPONENT_KEY, RumConstants.COMPONENT_ERROR)
        attributes.put(RumConstants.ERROR_KEY, "true")
        appState?.let { attributes.put(RumConstants.APP_STATE_KEY, it) }
    }

    override fun onEnd(
        attributes: AttributesBuilder,
        context: OtelContext,
        stackTrace: Array<StackTraceElement>,
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
