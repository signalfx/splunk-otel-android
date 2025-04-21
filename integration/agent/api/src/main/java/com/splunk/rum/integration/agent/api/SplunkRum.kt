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

package com.splunk.rum.integration.agent.api

import android.app.Application
import android.webkit.WebView
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.api.SplunkRum.Companion.install
import com.splunk.rum.integration.agent.api.SplunkRum.Companion.instance
import com.splunk.rum.integration.agent.api.attributes.MutableAttributes
import com.splunk.rum.integration.agent.api.internal.SplunkRumAgentCore
import com.splunk.rum.integration.agent.api.subprocess.SubprocessDetector
import com.splunk.rum.integration.agent.api.user.User
import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.integration.agent.internal.user.NoOpUserManager
import com.splunk.rum.integration.agent.internal.user.UserManager
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import java.util.function.Consumer

/**
 * The [SplunkRum] class is responsible for initializing and providing access to the RUM agent.
 * Agent is initialized through the [install] method.
 */
class SplunkRum private constructor(
    agentConfiguration: AgentConfiguration,
    userManager: IUserManager,
    val openTelemetry: OpenTelemetry,
    val state: IState = State(agentConfiguration),
    val session: ISession = Session(ISession.State())
) {
    val user: User = User(userManager)

    /**
     * Represents the global attributes configured for the agent.
     */
    val globalAttributes: MutableAttributes = agentConfiguration.globalAttributes

    @Deprecated("Use property session.state.sessionId", ReplaceWith("session.state.sessionId"))
    fun getRumSessionId(): String {
        return session.state.sessionId
    }

    @Deprecated("Use globalAttributes property")
    fun <T> setGlobalAttribute(key: AttributeKey<T>, value: T) {
        @Suppress("UNCHECKED_CAST")
        value?.let { globalAttributes[key as AttributeKey<Any>] = it as Any }
    }

    @Deprecated("Use globalAttributes property")
    fun updateGlobalAttributes(attributesUpdater: Consumer<AttributesBuilder>) {
        globalAttributes.update { attributesUpdater.accept(this) }
    }

    @Deprecated("Use webView.integrateWithBrowserRum(webView)")
    fun integrateWithBrowserRum(webView: WebView) {
        // TODO separate task
    }

    companion object {
        private val noop = SplunkRum(
            openTelemetry = OpenTelemetry.noop(),
            agentConfiguration = AgentConfiguration.noop,
            state = Noop(),
            userManager = NoOpUserManager
        )
        private var instanceInternal: SplunkRum? = null
        private const val TAG = "SplunkRum"

        /**
         * Provides access to the initialized instance of [SplunkRum].
         *
         * @return The single instance of [SplunkRum] that has been initialized.
         * @throws RuntimeException if the [install] method has not been called prior to accessing the instance.
         */
        @get:JvmStatic
        val instance: SplunkRum
            get() = instanceInternal ?: noop

        /**
         * Initializes the RUM agent with the provided application context and configurations.
         * This method must be called before accessing the [instance] property.
         *
         * If the RUM agent has already been initialized, this method returns the existing instance.
         *
         * @param application The application context used to initialize the RUM agent.
         * @param agentConfiguration Configuration parameters for the RUM agent.
         * @param moduleConfigurations An array of module configurations.
         * @return The initialized [SplunkRum] instance.
         */
        @JvmStatic
        fun install(
            application: Application,
            agentConfiguration: AgentConfiguration,
            vararg moduleConfigurations: ModuleConfiguration
        ): SplunkRum {
            if (instanceInternal != null)
                return instance


            val isSubprocess = SubprocessDetector.isSubprocess(applicationId = agentConfiguration.instrumentedProcessName)

            if (isSubprocess && agentConfiguration.instrumentedProcessName != null) {
                Logger.d(TAG, "install() - Subprocess detected exiting")

                return SplunkRum(
                    openTelemetry = OpenTelemetry.noop(), agentConfiguration = AgentConfiguration.noop,
                    state = Noop(
                        Status.NotRunning.Cause.Subprocess
                    ),
                    userManager = NoOpUserManager,
                )
            }

            val userManager = UserManager()

            val openTelemetry = SplunkRumAgentCore.install(application, agentConfiguration, userManager, moduleConfigurations.toList())

            instanceInternal = SplunkRum(agentConfiguration = agentConfiguration, openTelemetry = openTelemetry, userManager = UserManager())

            return instance
        }

        @JvmStatic
        @Deprecated("Use SplunkRum.install()", ReplaceWith("install", "com.splunk.rum.integration.agent.api.SplunkRumBuilder"))
        fun builder(): SplunkRumBuilder {
            return SplunkRumBuilder()
        }

        @JvmStatic
        @Deprecated("Use SplunkRum.instance.state.status == Status.Running", ReplaceWith("instance.state.status == Status.Running", "com.splunk.rum.integration.agent.api.SplunkRum.Companion.instance"))
        fun isInitialized(): Boolean {
            return instance.state.status == Status.Running
        }

        @JvmStatic
        @Deprecated("Use property noop")
        fun noop(): SplunkRum {
            return noop
        }
    }
}
