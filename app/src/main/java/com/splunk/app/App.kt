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

package com.splunk.app

import android.app.Application
import com.cisco.android.instrumentation.recording.core.api.RenderingMode
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.EndpointConfiguration
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.api.attributes.MutableAttributes
import com.splunk.rum.integration.anr.AnrModuleConfiguration
import com.splunk.rum.integration.crash.CrashModuleConfiguration
import com.splunk.rum.integration.interactions.InteractionsModuleConfiguration
import com.splunk.rum.integration.navigation.NavigationModuleConfiguration
import com.splunk.rum.integration.sessionreplay.extension.sessionReplay
import com.splunk.rum.integration.httpurlconnection.auto.HttpURLModuleConfiguration
import com.splunk.rum.integration.okhttp3.auto.OkHttp3ModuleConfiguration
import com.splunk.rum.integration.networkmonitor.NetworkMonitorModuleConfiguration
import com.splunk.rum.integration.slowrendering.SlowRenderingModuleConfiguration
import java.time.Duration

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // TODO: Reenable with the bridge support
        // BridgeManager.bridgeInterfaces += TomasBridgeInterface()

        val globalAttributes = MutableAttributes()
        // Uncomment the following to test global attributes
        // globalAttributes["session.id"] = "wrong"
        // globalAttributes["name"] = "John Doe"
        // globalAttributes["age"] = 32
        // globalAttributes["email"] = "john.doe@example.com"
        // globalAttributes["isValid"] = true

        // Uncomment the following to test install with legacy SplunkRum builder
        // val agent = SplunkRum.builder()
        //   .setRealm("lab0")
        //   .setRumAccessToken("1CucSUVwF5f2hNyuHwKNfw")
        //   .setApplicationName("Android demo app")
        //   .setDeploymentEnvironment("test")
        //   .setGlobalAttributes(Attributes.of(AttributeKey.stringKey("legacyGlobalAttributesKey"), "legacyGlobalAttributesVal"))
        //   .disableANRReporting()
        //   .disableCrashReporting()
        //   .disableSlowRenderingDetection()
        //   .setSlowRenderingDetectionPollInterval(Duration.ofMillis(500))
        //   .build(this)

        val agent = SplunkRum.install(
            application = this,
            agentConfiguration = AgentConfiguration(
                endpoint = EndpointConfiguration(
                    realm = "lab0",
                    rumAccessToken = "GHnFoSZy5Fr9u9EL_8yKkQ"
                ),
                appName = "DavidK-App",
                enableDebugLogging = true,
                globalAttributes = globalAttributes,
                deploymentEnvironment = "test",
                deferredUntilForeground = true,
            ),
            moduleConfigurations = arrayOf(
                InteractionsModuleConfiguration(
                    isEnabled = true
                ),
                NavigationModuleConfiguration(
                    isEnabled = true,
                    isFragmentTrackingEnabled = false,
                    isActivityTrackingEnabled = false
                ),
                CrashModuleConfiguration(
                    isEnabled = false
                ),
                AnrModuleConfiguration(
                    isEnabled = false
                ),
                HttpURLModuleConfiguration(
                    isEnabled = false
                ),
                OkHttp3ModuleConfiguration(
                    isEnabled = false
                ),
                NetworkMonitorModuleConfiguration(
                    isEnabled = false
                ),
                SlowRenderingModuleConfiguration(
                    isEnabled = true,
                    interval = Duration.ofMillis(500)
                ),
            )
        )

        agent.sessionReplay.preferences.renderingMode = RenderingMode.NATIVE
        agent.sessionReplay.start()
    }
}
