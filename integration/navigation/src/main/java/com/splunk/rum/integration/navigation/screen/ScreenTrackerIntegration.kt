package com.splunk.rum.integration.navigation.screen

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
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
import java.lang.ref.WeakReference

internal object ScreenTrackerIntegration {

    private lateinit var activityTracerManager: ActivityTracerManager
    private lateinit var fragmentTracerManager: FragmentTracerManager

    /**
     * Hooks activity and fragment lifecycle callbacks to produce navigation telemetry.
     *
     * Note: `attach` assumes it is invoked from `Application.onCreate`. In hybrid setups
     * where installation happens later, the current activity may already exist; passing
     * `currentActivity` lets us backfill tracing for that activity.
     */
    fun attach(context: Context, currentActivity: WeakReference<Activity>?) {
        Logger.d("ScreenTrackerIntegration", "attach")
        val tracer = SplunkOpenTelemetrySdk.instance?.getTracer("io.opentelemetry.lifecycle") ?: return
        val application = context.applicationContext as Application

        registerActivityLifecycle(application, tracer)
        registerFragmentLifecycle(application, tracer)
        currentActivity?.get()?.let { activity ->
            activityTracerManager.startActivityCreation(activity)
            if (activity is FragmentActivity) {
                val callback = FragmentCallback(fragmentTracerManager)
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(callback, true)
            }
        }
    }

    private fun registerActivityLifecycle(application: Application, tracer: Tracer) {
        Logger.d("ScreenTrackerIntegration", "registerActivityLifecycle")
        activityTracerManager = ActivityTracerManager(tracer, null)

        val activityCallback = if (Build.VERSION.SDK_INT >= 29) {
            ActivityCallback29(activityTracerManager)
        } else {
            ActivityCallback21(activityTracerManager)
        }

        application.registerActivityLifecycleCallbacks(activityCallback)
    }

    private fun registerFragmentLifecycle(application: Application, tracer: Tracer) {
        Logger.d("ScreenTrackerIntegration", "registerFragmentLifecycle")
        fragmentTracerManager = FragmentTracerManager(tracer)
        val callback = FragmentCallback(fragmentTracerManager)

        val fragmentObserver = if (Build.VERSION.SDK_INT >= 29) {
            FragmentActivityCallback29(callback)
        } else {
            FragmentActivityCallback21(callback)
        }

        application.registerActivityLifecycleCallbacks(fragmentObserver)
    }
}
