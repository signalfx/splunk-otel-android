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
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.lifecycle.callback.ActivityLifecycleCallback21
import com.splunk.rum.integration.lifecycle.callback.ActivityLifecycleCallback29
import com.splunk.rum.integration.lifecycle.callback.FragmentActivityCallback21
import com.splunk.rum.integration.lifecycle.callback.FragmentActivityCallback29
import com.splunk.rum.integration.lifecycle.callback.FragmentLifecycleCallback
import io.opentelemetry.android.instrumentation.InstallationContext

/**
 * Module integration for capturing UI lifecycle events (Activity and Fragment lifecycle transitions).
 *
 * This module registers lifecycle callbacks and emits OpenTelemetry events for all lifecycle
 * transitions, providing detailed visibility into the Activity and Fragment lifecycle.
 *
 * Pattern copied from: navigation/screen/ScreenTrackerIntegration.kt (lines 32-73)
 */
internal object LifecycleModuleIntegration : ModuleIntegration<LifecycleModuleConfiguration>(
    defaultModuleConfiguration = LifecycleModuleConfiguration()
) {

    private const val TAG = "LifecycleModuleIntegration"

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall() called")

        if (!moduleConfiguration.isEnabled) {
            Logger.w(TAG, "Lifecycle module is disabled")
            super.onInstall(context, oTelInstallationContext, moduleConfigurations)
            return
        }

        Logger.d(TAG, "Lifecycle module is enabled. Registering lifecycle callbacks.")

        val application = context.applicationContext as Application
        val emitter = LifecycleEventEmitter()

        registerActivityLifecycle(application, emitter)
        registerFragmentLifecycle(application, emitter)

        super.onInstall(context, oTelInstallationContext, moduleConfigurations)
    }

    /**
     * Register Activity lifecycle callbacks.
     * Pattern from: ScreenTrackerIntegration.registerActivityLifecycle() (lines 48-59)
     */
    private fun registerActivityLifecycle(application: Application, emitter: LifecycleEventEmitter) {
        Logger.d(TAG, "registerActivityLifecycle")

        val activityCallback = if (Build.VERSION.SDK_INT >= 29) {
            ActivityLifecycleCallback29(emitter)
        } else {
            ActivityLifecycleCallback21(emitter)
        }

        application.registerActivityLifecycleCallbacks(activityCallback)
    }

    /**
     * Register Fragment lifecycle callbacks via Activity callback pattern.
     * Pattern from: ScreenTrackerIntegration.registerFragmentLifecycle() (lines 61-73)
     */
    private fun registerFragmentLifecycle(application: Application, emitter: LifecycleEventEmitter) {
        Logger.d(TAG, "registerFragmentLifecycle")

        val fragmentCallback = FragmentLifecycleCallback(emitter)

        val fragmentActivityCallback = if (Build.VERSION.SDK_INT >= 29) {
            FragmentActivityCallback29(fragmentCallback)
        } else {
            FragmentActivityCallback21(fragmentCallback)
        }

        application.registerActivityLifecycleCallbacks(fragmentActivityCallback)
    }
}
