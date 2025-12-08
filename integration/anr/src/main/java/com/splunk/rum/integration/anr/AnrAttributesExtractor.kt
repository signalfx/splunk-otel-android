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
    private var isForeground = false

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

        val appState = if (isForeground) RumConstants.APP_STATE_FOREGROUND else RumConstants.APP_STATE_BACKGROUND
        attributes.put(RumConstants.APP_STATE_KEY, appState)
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
            isForeground = true
        }

        override fun onAppForegrounded() {
            isForeground = true
        }

        override fun onAppBackgrounded() {
            isForeground = false
        }

        override fun onAppClosed() {
            isForeground = false
        }
    }
}
