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

package com.splunk.rum.integration.agent.api

import android.app.Application
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.api.SplunkRum.Companion.install
import com.splunk.rum.integration.agent.api.attributes.MutableAttributes
import com.splunk.rum.integration.agent.internal.legacy.LegacyAnrModuleConfiguration
import com.splunk.rum.integration.agent.internal.legacy.LegacyCrashModuleConfiguration
import com.splunk.rum.integration.agent.internal.legacy.LegacySlowRenderingModuleConfiguration
import io.opentelemetry.api.common.Attributes
import java.net.URL
import java.util.function.Consumer

@Deprecated("Use SplunkRum.install()")
class SplunkRumBuilder {

    private var accessToken: String? = null
    private var appName: String? = null
    private var deploymentEnvironment: String? = null
    private var realm: String? = null
    private var beaconEndpoint: String? = null
    private var enableDebug: Boolean = false
    private var globalAttributes: MutableAttributes = MutableAttributes()
    private var sessionBasedSampling = 1.0
    private var spanFilter: Consumer<SpanFilterBuilder>? = null
    private var instrumentedProcessName: String? = null
    private var deferredUntilForeground: Boolean = false
    private var maxUsageMegabytes: Int = 25
    private var crashReportingEnabled: Boolean = true
    private var anrReportingEnabled: Boolean = true
    private var slowRenderingDetectionEnabled: Boolean = true

    fun setRumAccessToken(token: String): SplunkRumBuilder {
        accessToken = token
        return this
    }

    fun setApplicationName(name: String): SplunkRumBuilder {
        appName = name
        return this
    }

    fun setDeploymentEnvironment(environment: String): SplunkRumBuilder {
        deploymentEnvironment = environment
        return this
    }

    fun setRealm(realm: String): SplunkRumBuilder {
        if (beaconEndpoint != null)
            throw IllegalStateException("setRealm can not be set when setBeaconEndpoint was called")

        this.realm = realm
        return this
    }

    fun setBeaconEndpoint(endpoint: String): SplunkRumBuilder {
        if (beaconEndpoint != null)
            throw IllegalStateException("setBeaconEndpoint can not be set when setRealm was called")

        beaconEndpoint = endpoint
        return this
    }

    fun enableDebug(debug: Boolean): SplunkRumBuilder {
        enableDebug = debug
        return this
    }

    fun setGlobalAttributes(attributes: Attributes): SplunkRumBuilder {
        globalAttributes.setAll(attributes)
        return this
    }

    fun filterSpans(spanFilter: Consumer<SpanFilterBuilder>): SplunkRumBuilder {
        this.spanFilter = spanFilter
        return this
    }

    fun enableSessionBasedSampling(ratio: Double): SplunkRumBuilder {
        if (ratio < 0)
            Logger.w(TAG, "enableSessionBasedSampling(ratio: $ratio) - ratio can not be lower then 0")
        else if (ratio > 1)
            Logger.w(TAG, "enableSessionBasedSampling(ratio: $ratio) - ratio can not be greater then 1")
        else
            sessionBasedSampling = ratio

        return this
    }

    fun disableSubprocessInstrumentation(applicationId: String): SplunkRumBuilder {
        instrumentedProcessName = applicationId
        return this
    }

    fun enableBackgroundInstrumentationDeferredUntilForeground(enable: Boolean): SplunkRumBuilder {
        deferredUntilForeground = enable

        return this
    }

    @Deprecated("This is no longer supported")
    fun enableDiskBuffering(enable: Boolean): SplunkRumBuilder {
        return this
    }

    @Deprecated("This is no longer supported")
    fun limitDiskUsageMegabytes(maxUsageMegabytes: Int): SplunkRumBuilder {
        return this
    }

    @Deprecated("CrashReporting is now controlled by the CrashModuleConfiguration")
    fun disableCrashReporting(): SplunkRumBuilder {
        crashReportingEnabled = false
        return this
    }

    @Deprecated("ANRReporting is now controlled by the ANRModuleConfiguration")
    fun disableANRReporting(): SplunkRumBuilder {
        anrReportingEnabled = false
        return this
    }

    @Deprecated("Slow Rendering Detection is now controlled by the SlowRenderingModuleConfiguration")
    fun disableSlowRenderingDetection(): SplunkRumBuilder {
        slowRenderingDetectionEnabled = false
        return this
    }

    fun build(application: Application): SplunkRum {
        val realm = realm
        val beaconEndpoint = beaconEndpoint

        val endpointConfiguration = when {
            realm != null -> EndpointConfiguration(
                realm = realm, accessToken ?: throw IllegalStateException("rumAccessToken was not set")
            )

            beaconEndpoint != null -> EndpointConfiguration(
                traces = URL(beaconEndpoint)
            )

            else ->
                throw IllegalStateException("setRealm() or setBeaconEndpoint() was not called")
        }


        val agent = install(
            application,
            agentConfiguration = AgentConfiguration(
                endpoint = endpointConfiguration,
                appName = appName ?: throw IllegalStateException("applicationName was not set"),
                deploymentEnvironment = deploymentEnvironment ?: throw IllegalStateException("deploymentEnvironment was not set"),
                enableDebugLogging = enableDebug,
                sessionSamplingRate = sessionBasedSampling,
                globalAttributes = globalAttributes,
                instrumentedProcessName = instrumentedProcessName,
                deferredUntilForeground = deferredUntilForeground,
                spanInterceptor = this.spanFilter?.let {
                    val spanFilterBuilder = SpanFilterBuilder()
                    it.accept(spanFilterBuilder)
                    spanFilterBuilder.toSpanInterceptor()
                },
            ),
            moduleConfigurations = arrayOf(
                LegacyCrashModuleConfiguration(
                    isEnabled = crashReportingEnabled
                ),
                LegacyAnrModuleConfiguration(
                    isEnabled = anrReportingEnabled
                ),
                LegacySlowRenderingModuleConfiguration(
                    isEnabled = slowRenderingDetectionEnabled
                )
            )
        )

        // TODO limitDiskUsageMegabytes

        return agent
    }

    private companion object {
        const val TAG = "SplunkRumBuilder"
    }
}
