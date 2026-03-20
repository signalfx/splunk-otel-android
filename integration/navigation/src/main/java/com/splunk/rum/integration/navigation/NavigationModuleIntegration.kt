/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.rum.integration.navigation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.adapters.ActivityLifecycleCallbacksAdapter
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.navigation.automatic.NavigationEventEmitter
import com.splunk.rum.integration.navigation.automatic.ScreenChangeDetector
import com.splunk.rum.integration.navigation.automatic.callback.NavigationActivityCallback
import com.splunk.rum.integration.navigation.automatic.callback.NavigationFragmentActivityCallback21
import com.splunk.rum.integration.navigation.automatic.callback.NavigationFragmentActivityCallback29
import com.splunk.rum.integration.navigation.automatic.callback.NavigationFragmentCallback
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.common.Attributes
import java.lang.ref.WeakReference

/**
 * Module integration for capturing navigation events (screen changes).
 * When automated tracking is enabled, this module registers Activity and Fragment callbacks
 * to detect screen changes and emits OpenTelemetry device.app.ui.navigation events.
 */
internal object NavigationModuleIntegration : ModuleIntegration<NavigationModuleConfiguration>(
    defaultModuleConfiguration = NavigationModuleConfiguration()
) {

    private const val TAG = "NavigationModuleIntegration"
    private var currentActivityReference: WeakReference<Activity>? = null

    private val emitter = NavigationEventEmitter()
    private var screenChangeDetector: ScreenChangeDetector? = null

    private val activityLifecycleCallbacksAdapter = object : ActivityLifecycleCallbacksAdapter {
        override fun onActivityResumed(activity: Activity) {
            currentActivityReference = WeakReference(activity)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Navigation.instance.listener = navigationListener
        (context as Application).registerActivityLifecycleCallbacks(
            activityLifecycleCallbacksAdapter
        )
    }

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall")
        if (!moduleConfiguration.isEnabled) {
            Navigation.instance.listener = null
            (context as Application).unregisterActivityLifecycleCallbacks(activityLifecycleCallbacksAdapter)
            currentActivityReference = null
            return
        }

        if (moduleConfiguration.isAutomatedTrackingEnabled) {
            Logger.d(TAG, "Navigation module automated tracking enabled. Registering navigation callbacks.")

            val application = context.applicationContext as Application
            val detector = ScreenChangeDetector(emitter)
            screenChangeDetector = detector

            registerActivityLifecycle(application, detector)
            registerFragmentLifecycle(application, detector)

            // Seed detector with already visible activity for late/hybrid installs.
            currentActivityReference?.get()?.let { activity ->
                detector.onActivityResumed(activity)
                if (activity is FragmentActivity) {
                    val fragmentCallback = NavigationFragmentCallback(detector)
                    activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)
                }
            }
        }

        (context as Application).unregisterActivityLifecycleCallbacks(activityLifecycleCallbacksAdapter)
        currentActivityReference = null
    }

    override fun onPostInstall() {
        super.onPostInstall()
        if (!moduleConfiguration.isEnabled) return
        Logger.d(TAG, "onPostInstall() - processing cached events")
        emitter.processCachedEvents()
    }

    /**
     * Register Activity lifecycle callbacks.
     */
    private fun registerActivityLifecycle(application: Application, screenChangeDetector: ScreenChangeDetector) {
        Logger.d(TAG, "registerActivityLifecycle")
        val activityCallback = NavigationActivityCallback(screenChangeDetector)
        application.registerActivityLifecycleCallbacks(activityCallback)
    }

    /**
     * Register Fragment lifecycle callbacks via Activity callback pattern.
     */
    private fun registerFragmentLifecycle(application: Application, screenChangeDetector: ScreenChangeDetector) {
        Logger.d(TAG, "registerFragmentLifecycle")
        val fragmentCallback = NavigationFragmentCallback(screenChangeDetector)

        val fragmentActivityCallback = if (Build.VERSION.SDK_INT >= 29) {
            NavigationFragmentActivityCallback29(fragmentCallback)
        } else {
            NavigationFragmentActivityCallback21(fragmentCallback)
        }

        application.registerActivityLifecycleCallbacks(fragmentActivityCallback)
    }

    private val navigationListener = object : Navigation.Listener {
        override fun onScreenNameChanged(screenName: String, attributes: Attributes) {
            Logger.d(TAG, "onScreenNameChanged(screenName: $screenName, attributes: $attributes)")

            emitter.emitNavigationEvent(screenName, attributes)
            screenChangeDetector?.recordEmittedScreen(screenName)
        }
    }
}
