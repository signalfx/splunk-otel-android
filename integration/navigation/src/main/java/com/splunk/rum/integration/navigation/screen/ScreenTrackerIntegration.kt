package com.splunk.rum.integration.navigation.screen

import android.app.Application
import android.content.Context
import android.os.Build
import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.integration.navigation.tracer.activity.ActivityTracerManager
import com.splunk.rum.integration.navigation.tracer.activity.callback.ActivityCallback21
import com.splunk.rum.integration.navigation.tracer.activity.callback.ActivityCallback29
import com.splunk.rum.integration.navigation.tracer.fragment.FragmentTracerManager
import com.splunk.rum.integration.navigation.tracer.fragment.activity.FragmentActivityCallback21
import com.splunk.rum.integration.navigation.tracer.fragment.activity.FragmentActivityCallback29
import com.splunk.rum.integration.navigation.tracer.fragment.callback.FragmentCallback
import io.opentelemetry.api.trace.Tracer

internal object ScreenTrackerIntegration {

    fun attach(context: Context) {
        Logger.d("ScreenTrackerIntegration", "attach")
        val tracer = SplunkOpenTelemetrySdk.instance?.getTracer("io.opentelemetry.lifecycle") ?: return
        val application = context.applicationContext as Application

        registerActivityLifecycle(application, tracer)
        registerFragmentLifecycle(application, tracer)
    }

    private fun registerActivityLifecycle(application: Application, tracer: Tracer) {
        Logger.d("ScreenTrackerIntegration", "registerActivityLifecycle")
        val tracerManager = ActivityTracerManager(tracer, null)

        val activityCallback = if (Build.VERSION.SDK_INT >= 29) {
            ActivityCallback29(tracerManager)
        } else {
            ActivityCallback21(tracerManager)
        }

        application.registerActivityLifecycleCallbacks(activityCallback)
    }

    private fun registerFragmentLifecycle(application: Application, tracer: Tracer) {
        Logger.d("ScreenTrackerIntegration", "registerFragmentLifecycle")
        val tracerManager = FragmentTracerManager(tracer)
        val callback = FragmentCallback(tracerManager)

        val fragmentObserver = if (Build.VERSION.SDK_INT >= 29) {
            FragmentActivityCallback29(callback)
        } else {
            FragmentActivityCallback21(callback)
        }

        application.registerActivityLifecycleCallbacks(fragmentObserver)
    }
}
