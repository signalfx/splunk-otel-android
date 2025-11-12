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

import android.content.Context
import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.common.otel.internal.RumConstants.NAVIGATION_NAME
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.navigation.screen.ScreenTrackerIntegration
import io.opentelemetry.android.instrumentation.InstallationContext
import java.time.Instant

internal object NavigationModuleIntegration : ModuleIntegration<NavigationModuleConfiguration>(
    defaultModuleConfiguration = NavigationModuleConfiguration()
) {

    private const val TAG = "NavigationIntegration"

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        if (moduleConfiguration.isEnabled) {
            Navigation.instance.listener = navigationListener
        }

        if (moduleConfiguration.isAutomatedTrackingEnabled) {
            ScreenTrackerIntegration.attach(context)
        }
    }

    private val navigationListener = object : Navigation.Listener {
        override fun onScreenNameChanged(screenName: String) {
            Logger.d(TAG, "onScreenNameChanged(screenName: $screenName)")

            val provider = SplunkOpenTelemetrySdk.instance?.sdkTracerProvider ?: return

            ScreenNameTracker.screenName = screenName

            @Suppress("NewApi") // Requires API 26 or core library desugaring
            val timeNow = Instant.now()

            val screenSpan = provider.get(RumConstants.RUM_TRACER_NAME)
                .spanBuilder(NAVIGATION_NAME)
                .setAttribute(RumConstants.COMPONENT_KEY, "ui")
                .setStartTimestamp(timeNow)
                .startSpan()

            screenSpan.end(timeNow)
        }
    }
}
