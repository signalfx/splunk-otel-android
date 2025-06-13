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
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import com.splunk.rum.integration.anr.AnrModuleConfiguration
import com.splunk.rum.integration.crash.CrashModuleConfiguration
import com.splunk.rum.integration.httpurlconnection.auto.HttpURLModuleConfiguration
import com.splunk.rum.integration.interactions.InteractionsModuleConfiguration
import com.splunk.rum.integration.navigation.NavigationModuleConfiguration
import com.splunk.rum.integration.networkmonitor.NetworkMonitorModuleConfiguration
import com.splunk.rum.integration.okhttp3.auto.OkHttp3AutoModuleConfiguration
import com.splunk.rum.integration.okhttp3.manual.OkHttp3ManualModuleConfiguration
import com.splunk.rum.integration.sessionreplay.extension.sessionReplay
import com.splunk.rum.integration.slowrendering.SlowRenderingModuleConfiguration
import java.time.Duration

class App : Application() {

    /**
     * BuildConfig.SPLUNK_REALM and BuildConfig.SPLUNK_RUM_ACCESS_TOKEN
     *
     * These values are injected at build time from your personal Gradle global properties.
     * This allows sensitive data (like tokens and keys) to be kept *out* of the codebase.
     *
     * To configure:
     * 1. Open (or create) the global properties file:
     *      ~/.gradle/gradle.properties
     *
     * 2. Add the following lines:
     *      splunkRealm=yourRealmValue
     *      splunkRumAccessToken=yourAccessToken
     *
     * 3. Sync Gradle and rebuild the project.
     *
     * ❗️If these properties are missing, empty String will be used for both realm and accessToken. ❗️
     */
    private val realm = BuildConfig.SPLUNK_REALM
    private val rumAccessToken = BuildConfig.SPLUNK_RUM_ACCESS_TOKEN

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

        val agent = SplunkRum.install(
            application = this,
            agentConfiguration = AgentConfiguration(
                endpoint = EndpointConfiguration(
                    realm = realm,
                    rumAccessToken = rumAccessToken
                ),
                appName = "Android demo app",
                enableDebugLogging = true,
                globalAttributes = globalAttributes,
                deploymentEnvironment = "test",
                deferredUntilForeground = true
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
                    isEnabled = true
                ),
                AnrModuleConfiguration(
                    isEnabled = true
                ),
                HttpURLModuleConfiguration(
                    isEnabled = true,
                    capturedRequestHeaders = listOf("Host", "Accept"),
                    capturedResponseHeaders = listOf("Date", "Content-Type", "Content-Length")
                ),
                OkHttp3AutoModuleConfiguration(
                    isEnabled = true,
                    capturedRequestHeaders = listOf("User-Agent", "Accept"),
                    capturedResponseHeaders = listOf("Date", "Content-Type", "Content-Length")
                ),
                OkHttp3ManualModuleConfiguration(
                    capturedRequestHeaders = listOf("Content-Type", "Accept"),
                    capturedResponseHeaders = listOf("Server", "Content-Type", "Content-Length")
                ),
                NetworkMonitorModuleConfiguration(
                    isEnabled = true
                ),
                SlowRenderingModuleConfiguration(
                    isEnabled = true,
                    interval = Duration.ofMillis(500)
                )
            )
        )

        agent.sessionReplay.preferences.renderingMode = RenderingMode.NATIVE
        agent.sessionReplay.start()
    }
}
