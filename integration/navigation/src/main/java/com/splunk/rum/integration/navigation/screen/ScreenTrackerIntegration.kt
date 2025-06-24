package com.splunk.rum.integration.navigation.screen

import android.app.Application
import android.content.Context
import android.os.Build
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
        val tracer = SplunkOpenTelemetrySdk.instance?.getTracer("io.opentelemetry.lifecycle") ?: return
        val application = context.applicationContext as Application

        val visibleScreenTracker = VisibleScreenTracker(application)

        registerActivityLifecycle(application, tracer, visibleScreenTracker)
        registerFragmentLifecycle(application, tracer, visibleScreenTracker)
    }

    private fun registerActivityLifecycle(
        application: Application,
        tracer: Tracer,
        visibleScreenTracker: VisibleScreenTracker
    ) {
        val tracerManager = ActivityTracerManager(tracer, visibleScreenTracker, null)

        val activityCallback = if (Build.VERSION.SDK_INT >= 29) {
            ActivityCallback29(tracerManager)
        } else {
            ActivityCallback21(tracerManager)
        }

        application.registerActivityLifecycleCallbacks(activityCallback)
    }

    private fun registerFragmentLifecycle(
        application: Application,
        tracer: Tracer,
        visibleScreenTracker: VisibleScreenTracker
    ) {
        val tracerManager = FragmentTracerManager(tracer, visibleScreenTracker)
        val callback = FragmentCallback(tracerManager)

        val fragmentObserver = if (Build.VERSION.SDK_INT >= 29) {
            FragmentActivityCallback29(callback)
        } else {
            FragmentActivityCallback21(callback)
        }

        application.registerActivityLifecycleCallbacks(fragmentObserver)
    }
}
