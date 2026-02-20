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
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.EndpointConfiguration
import com.splunk.rum.integration.agent.api.SplunkRum
import com.splunk.rum.integration.agent.api.session.SessionConfiguration
import com.splunk.rum.integration.agent.api.spaninterceptor.toMutableSpanData
import com.splunk.rum.integration.agent.api.user.UserConfiguration
import com.splunk.rum.integration.agent.api.user.UserTrackingMode
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import com.splunk.rum.integration.httpurlconnection.auto.HttpURLModuleConfiguration
import com.splunk.rum.integration.lifecycle.LifecycleModuleConfiguration
import com.splunk.rum.integration.lifecycle.model.LifecycleAction
import com.splunk.rum.integration.navigation.NavigationModuleConfiguration
import com.splunk.rum.integration.okhttp3.auto.OkHttp3AutoModuleConfiguration
import com.splunk.rum.integration.okhttp3.manual.OkHttp3ManualModuleConfiguration
import com.splunk.rum.integration.sessionreplay.SessionReplayModuleConfiguration
import com.splunk.rum.integration.sessionreplay.api.RenderingMode
import com.splunk.rum.integration.sessionreplay.api.SessionReplay
import com.splunk.rum.integration.sessionreplay.extension.sessionReplay
import com.splunk.rum.integration.slowrendering.SlowRenderingModuleConfiguration
import io.opentelemetry.sdk.trace.data.SpanData
import java.time.Duration

/**
 * Application class responsible for initializing and configuring the Splunk RUM agent
 * and associated monitoring modules.
 */
class App : Application() {

    /**
     * Agent configurations
     */

    /**
     *
     * Configuration of the RUM backend endpoint.
     *
     * Realm and access token are injected at build time via Gradle properties:
     * - `splunkRealm`
     * - `splunkRumAccessToken`
     *
     * These should be defined in your global Gradle properties file (`~/.gradle/gradle.properties`)
     * to keep sensitive data out of the codebase.
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
    private val endpointConfiguration = EndpointConfiguration(
        realm = BuildConfig.SPLUNK_REALM,
        rumAccessToken = BuildConfig.SPLUNK_RUM_ACCESS_TOKEN
    )

    private val globalAttributes = MutableAttributes().also { attributes ->
        attributes["test.user.email"] = "test.user@example.com"
        attributes["test.user.name"] = "Test User"
        attributes["test.user.plan"] = "premium"
        attributes["test.user.age"] = 29L
        attributes["test.user.is_verified"] = true
        attributes["test.user.signup_source"] = "referral"
        attributes["test.user.last_login_method"] = "google_oauth"
    }

    private val spanInterceptor: ((SpanData) -> SpanData?) = { spanData ->
        val mutableSpanData = spanData.toMutableSpanData()

        // Span filtering and modification can be done here using [mutableSpanData]

        mutableSpanData
    }

    private val userConfiguration = UserConfiguration(
        trackingMode = UserTrackingMode.ANONYMOUS_TRACKING
    )

    private val sessionConfiguration = SessionConfiguration(
        samplingRate = 1.0
    )

    /**
     * Module configurations
     */

    private val httpURLModuleConfiguration = HttpURLModuleConfiguration(
        isEnabled = false,
        capturedRequestHeaders = listOf("Host", "Accept"),
        capturedResponseHeaders = listOf("Date", "Content-Type", "Content-Length")
    )

    private val okHttp3AutoModuleConfiguration = OkHttp3AutoModuleConfiguration(
        isEnabled = false,
        capturedRequestHeaders = listOf("User-Agent", "Accept"),
        capturedResponseHeaders = listOf("Date", "Content-Type", "Content-Length")
    )

    private val okHttp3ManualModuleConfiguration = OkHttp3ManualModuleConfiguration(
        isEnabled = true,
        capturedRequestHeaders = listOf("Content-Type", "Accept"),
        capturedResponseHeaders = listOf("Server", "Content-Type", "Content-Length")
    )

    private val slowRenderingModuleConfiguration = SlowRenderingModuleConfiguration(
        isEnabled = true,
        interval = Duration.ofMillis(500)
    )

    private val lifecycleModuleConfiguration = LifecycleModuleConfiguration(
        isEnabled = true
        // Uncomment below allowedEvents to configure event filtration
//        allowedEvents = setOf(
//            LifecycleAction.CREATED,
//            LifecycleAction.RESUMED,
//            LifecycleAction.DESTROYED
//        )
    )

    private val navigationModuleConfiguration = NavigationModuleConfiguration()

    private val sessionReplayModuleConfiguration = SessionReplayModuleConfiguration(
        isEnabled = true,
        samplingRate = 0.5f
    )

    override fun onCreate() {
        super.onCreate()

        val moduleConfigurations = arrayOf(
            httpURLModuleConfiguration,
            okHttp3AutoModuleConfiguration,
            okHttp3ManualModuleConfiguration,
            slowRenderingModuleConfiguration,
            lifecycleModuleConfiguration,
            navigationModuleConfiguration,
            sessionReplayModuleConfiguration
        )

        val agentConfiguration = AgentConfiguration(
            endpoint = endpointConfiguration,
            appName = APP_NAME,
            appVersion = APP_VERSION,
            deploymentEnvironment = DEPLOYMENT_ENVIRONMENT,
            enableDebugLogging = ENABLE_DEBUG_LOGGING,
            globalAttributes = globalAttributes,
            spanInterceptor = spanInterceptor,
            user = userConfiguration,
            session = sessionConfiguration
        )

        val agent = SplunkRum.install(this, agentConfiguration, *moduleConfigurations)

        configureAndStartSessionReplay(agent.sessionReplay)
    }

    private fun configureAndStartSessionReplay(sessionReplay: SessionReplay) {
        sessionReplay.preferences.renderingMode = RenderingMode.NATIVE
        sessionReplay.start()
    }

    companion object {
        private const val APP_NAME = "Splunk OTel Android"
        private const val APP_VERSION = "1.0.0-test"
        private const val DEPLOYMENT_ENVIRONMENT = "test"
        private const val ENABLE_DEBUG_LOGGING = true
    }
}
