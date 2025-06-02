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

package com.splunk.rum.integration.lifecycle

import android.app.Application
import android.content.Context
import android.os.Build
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.lifecycle.screen.VisibleScreenTracker
import com.splunk.rum.integration.lifecycle.tracer.activity.ActivityTracerManager
import com.splunk.rum.integration.lifecycle.tracer.activity.callback.ActivityCallback21
import com.splunk.rum.integration.lifecycle.tracer.activity.callback.ActivityCallback29
import com.splunk.rum.integration.lifecycle.tracer.fragment.FragmentTracerManager
import com.splunk.rum.integration.lifecycle.tracer.fragment.activity.FragmentActivityCallback21
import com.splunk.rum.integration.lifecycle.tracer.fragment.activity.FragmentActivityCallback29
import com.splunk.rum.integration.lifecycle.tracer.fragment.callback.FragmentCallback
import com.splunk.sdk.common.otel.SplunkOpenTelemetrySdk
import io.opentelemetry.api.trace.Tracer

internal object LifecycleModuleIntegration : ModuleIntegration<LifecycleModuleConfiguration>(
    defaultModuleConfiguration = LifecycleModuleConfiguration()
) {

    override fun onAttach(context: Context) {
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
