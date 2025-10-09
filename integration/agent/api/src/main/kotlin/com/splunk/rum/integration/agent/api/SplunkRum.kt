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
import android.os.Build
import android.webkit.WebView
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.agent.api.SplunkRum.Companion.install
import com.splunk.rum.integration.agent.api.SplunkRum.Companion.instance
import com.splunk.rum.integration.agent.api.internal.SplunkRumAgentCore
import com.splunk.rum.integration.agent.api.session.ISession
import com.splunk.rum.integration.agent.api.session.Session
import com.splunk.rum.integration.agent.api.session.SessionState
import com.splunk.rum.integration.agent.api.subprocess.SubprocessDetector
import com.splunk.rum.integration.agent.api.user.User
import com.splunk.rum.integration.agent.api.user.toInternal
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.session.NoOpSplunkSessionManager
import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.integration.agent.internal.user.NoOpUserManager
import com.splunk.rum.integration.agent.internal.user.UserManager
import com.splunk.rum.utils.LegacyAPIReflectionUtils
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.trace.Span
import java.util.function.Consumer
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * The [SplunkRum] class is responsible for initializing and providing access to the RUM agent.
 * Agent is initialized through the [install] method.
 *
 * @param user Represents the user associated with the RUM session.
 * @param globalAttributes Represents the global attributes configured for the agent.
 */
class SplunkRum private constructor(
    agentConfiguration: AgentConfiguration,
    userManager: IUserManager,
    sessionManager: ISplunkSessionManager,
    val openTelemetry: OpenTelemetry,
    val state: IState = State(agentConfiguration),
    val session: ISession = Session(SessionState(agentConfiguration.session, sessionManager)),
    val user: User = User(userManager),
    val globalAttributes: MutableAttributes = MutableAttributes(agentConfiguration.globalAttributes)
) {

    @Deprecated("Use property session.state.sessionId", ReplaceWith("session.state.sessionId"))
    fun getRumSessionId(): String = session.state.id

    /**
     * Set an attribute in the global attributes that will be appended to every span and event.
     *
     * <p>Note: If this key is the same as an existing key in the global attributes, it will replace
     * the existing value.
     *
     * <p>If you attempt to set a value to null or use a null key, this call will be ignored.
     *
     * @param key The {@link AttributeKey} for the attribute.
     * @param value The value of the attribute, which must match the generic type of the key.
     * @param <T> The generic type of the value.
     */
    @Deprecated(
        message = "Use globalAttributes property directly",
        replaceWith = ReplaceWith("globalAttributes[key] = value")
    )
    fun <T> setGlobalAttribute(key: AttributeKey<T>, value: T) {
        @Suppress("UNCHECKED_CAST")
        value?.let { globalAttributes[key as AttributeKey<Any>] = it as Any }
    }

    /**
     * Update the global set of attributes that will be appended to every span and event.
     *
     * <p>Note: this operation performs an atomic update. The passed function should be free from
     * side effects, since it may be called multiple times in case of thread contention.
     *
     * @param attributesUpdater A function which will update the current set of attributes, by
     *     operating on a {@link AttributesBuilder} from the current set.
     */
    @Deprecated(
        message = "Use globalAttributes.update() method",
        replaceWith = ReplaceWith("globalAttributes.update { attributesUpdater.accept(this) }")
    )
    fun updateGlobalAttributes(attributesUpdater: Consumer<AttributesBuilder>) {
        globalAttributes.update { attributesUpdater.accept(this) }
    }

    /**
     * This method will enable Splunk Browser-based RUM to integrate with the current Android RUM
     * Session. It injects a javascript object named "SplunkRumNative" into your WebView which
     * exposes the Android Session ID to the browser-based RUM javascript implementation.
     *
     * @param webView The WebView to inject the javascript object into.
     */
    @Deprecated(
        message = "Use SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(webView)",
        replaceWith = ReplaceWith("SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(webView)")
    )
    fun integrateWithBrowserRum(webView: WebView) {
        LegacyAPIReflectionUtils.invokeOnCompanionInstance<Unit>(
            className = "com.splunk.rum.integration.webview.WebViewNativeBridge",
            methodName = "integrateWithBrowserRum",
            parameterTypes = arrayOf(WebView::class.java),
            args = arrayOf(webView)
        )
    }

    /**
     * Add a custom event to RUM monitoring. This can be useful to capture business events, or
     * simply add instrumentation to your application.
     *
     * <p>This event will be turned into a Span and sent to the RUM ingest along with other,
     * auto-generated spans.
     *
     * @param name The name of the event.
     * @param attributes Any {@link Attributes} to associate with the event.
     */
    @Deprecated(
        message = "Use SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(webView)",
        replaceWith = ReplaceWith("SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(webView)")
    )
    fun addRumEvent(name: String, attributes: Attributes) {
        LegacyAPIReflectionUtils.invokeOnCompanionInstance<Unit>(
            className = "com.splunk.rum.integration.customtracking.CustomTracking",
            methodName = "trackCustomEvent",
            parameterTypes = arrayOf(String::class.java, Attributes::class.java),
            args = arrayOf(name, attributes)
        )
    }

    /**
     * Start a Span to time a named workflow.
     *
     * @param workflowName The name of the workflow to start.
     * @return A {@link Span} that has been started.
     */
    @Deprecated(
        message = "Use SplunkRum.instance.customTracking.trackWorkflow(workflowName)",
        replaceWith = ReplaceWith("SplunkRum.instance.customTracking.trackWorkflow(workflowName)")
    )
    fun startWorkflow(workflowName: String): Span? = LegacyAPIReflectionUtils.invokeOnCompanionInstance<Span>(
        className = "com.splunk.rum.integration.customtracking.CustomTracking",
        methodName = "trackWorkflow",
        parameterTypes = arrayOf(String::class.java),
        args = arrayOf(workflowName)
    )

    /**
     * Add a custom exception to RUM monitoring. This can be useful for tracking custom error
     * handling in your application.
     *
     * <p>This event will be turned into a Span and sent to the RUM ingest along with other,
     * auto-generated spans.
     *
     * @param throwable A {@link Throwable} associated with this event.
     * @param attributes Any {@link Attributes} to associate with the event.
     */
    @Deprecated(
        message = "Use SplunkRum.instance.customTracking.trackException(throwable, attributes)",
        replaceWith = ReplaceWith("SplunkRum.instance.customTracking.trackException(throwable, attributes)")
    )
    @JvmOverloads
    fun addRumException(throwable: Throwable, attributes: Attributes = Attributes.empty()) {
        LegacyAPIReflectionUtils.invokeOnCompanionInstance<Unit>(
            className = "com.splunk.rum.integration.customtracking.CustomTracking",
            methodName = "trackException",
            parameterTypes = arrayOf(Throwable::class.java, Attributes::class.java),
            args = arrayOf(throwable, attributes)
        )
    }

    /**
     * Wrap the provided [OkHttpClient] with OpenTelemetry and RUM instrumentation. Since
     * [Call.Factory] is the primary useful interface implemented by the OkHttpClient, this
     * should be a drop-in replacement for any usages of OkHttpClient.
     *
     * @param client The [OkHttpClient] to wrap with OpenTelemetry and RUM instrumentation.
     * @return A [Call.Factory] implementation.
     */
    @Deprecated(
        "Use SplunkRum.buildOkHttpCallFactory(client)",
        ReplaceWith("SplunkRum.buildOkHttpCallFactory(client)")
    )
    fun createRumOkHttpCallFactory(client: OkHttpClient): Call.Factory =
        LegacyAPIReflectionUtils.invokeOnCompanionInstance<Call.Factory>(
            className = "com.splunk.rum.integration.okhttp3.manual.OkHttpManualInstrumentation",
            methodName = "buildOkHttpCallFactory",
            parameterTypes = arrayOf(OkHttpClient::class.java),
            args = arrayOf(client)
        ) ?: throw IllegalStateException()

    companion object {
        private val noop = SplunkRum(
            openTelemetry = OpenTelemetry.noop(),
            agentConfiguration = AgentConfiguration.noop,
            state = Noop(),
            userManager = NoOpUserManager,
            sessionManager = NoOpSplunkSessionManager
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
            if (instanceInternal != null) {
                return instance
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Logger.w(TAG, "install() - Unsupported Android version")

                return SplunkRum(
                    openTelemetry = OpenTelemetry.noop(),
                    agentConfiguration = AgentConfiguration.noop,
                    state = Noop(
                        notRunningCause = Status.NotRunning.UnsupportedOsVersion
                    ),
                    userManager = NoOpUserManager,
                    sessionManager = NoOpSplunkSessionManager
                )
            }

            val isSubprocess = SubprocessDetector.isSubprocess(
                applicationId = agentConfiguration.instrumentedProcessName
            )

            if (isSubprocess && agentConfiguration.instrumentedProcessName != null) {
                Logger.d(TAG, "install() - Subprocess detected exiting")

                return SplunkRum(
                    openTelemetry = OpenTelemetry.noop(),
                    agentConfiguration = AgentConfiguration.noop,
                    state = Noop(
                        notRunningCause = Status.NotRunning.Subprocess
                    ),
                    userManager = NoOpUserManager,
                    sessionManager = NoOpSplunkSessionManager
                )
            }

            val userManager = UserManager(agentConfiguration.user.trackingMode.toInternal())

            val sessionManager = AgentIntegration.obtainInstance(application).sessionManager

            val openTelemetry = SplunkRumAgentCore.install(
                application,
                agentConfiguration,
                userManager,
                sessionManager,
                moduleConfigurations.toList()
            )

            instanceInternal = SplunkRum(
                agentConfiguration = agentConfiguration,
                openTelemetry = openTelemetry,
                userManager = userManager,
                sessionManager = sessionManager
            )

            return instance
        }

        /**
         * Creates a new [SplunkRumBuilder], used to set up a [SplunkRum] instance.
         */
        @JvmStatic
        @Deprecated(
            "Use SplunkRum.install()",
            ReplaceWith("install", "com.splunk.rum.integration.agent.api.SplunkRumBuilder")
        )
        fun builder(): SplunkRumBuilder = SplunkRumBuilder()

        /**
         * Returns true if the Splunk RUM library has been successfully initialized.
         */
        @JvmStatic
        @Deprecated(
            "Use SplunkRum.instance.state.status == Status.Running",
            ReplaceWith(
                "instance.state.status == Status.Running",
                "com.splunk.rum.integration.agent.api.SplunkRum.Companion.instance"
            )
        )
        fun isInitialized(): Boolean = instance.state.status == Status.Running

        /**
         * Initialize a no-op version of the SplunkRum API, including the instance of OpenTelemetry that is available.
         * This can be useful for testing, or configuring your app without RUM enabled, but still using the APIs.
         */
        @JvmStatic
        @Deprecated(
            "Use SplunkRum.instance without calling SplunkRum.install() to get noop instance",
            ReplaceWith("SplunkRum.install()", "com.splunk.rum.integration.agent.api.SplunkRum.install")
        )
        fun noop(): SplunkRum = noop
    }
}
