/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.networkmonitor.internal

import android.app.Application
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateGate
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.network.CurrentNetworkProvider
import com.splunk.rum.instrumentation.networkmonitor.internal.telemetry.CurrentNetworkAttributes
import com.splunk.rum.instrumentation.networkmonitor.internal.telemetry.NetworkChangeEventEmitter
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runtime implementation of network change monitoring.
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class NetworkMonitorInstrumentation {
    private val attributeListeners = CopyOnWriteArrayList<(Attributes) -> Unit>()
    private val installed = AtomicBoolean()
    private var applicationStateGate: NetworkApplicationStateGate? = null

    internal var currentNetworkProviderFactory: (Application) -> CurrentNetworkProvider? =
        { application -> CurrentNetworkProvider.create(application) }

    /**
     * Adds a listener that receives the complete network attributes whenever the active network changes.
     *
     * Listeners continue to receive changes while the application is backgrounded. Only `network.change`
     * telemetry emission is foreground-gated.
     */
    fun addNetworkChangeListener(listener: (Attributes) -> Unit): NetworkMonitorInstrumentation =
        apply { attributeListeners.add(listener) }

    /**
     * Starts observing application state before agent installation can begin.
     *
     * The network module is attached from a ContentProvider, while the agent may install later. Preparing
     * the gate here ensures that an Activity foreground transition is not missed during that gap.
     */
    fun attachApplicationStateGate(application: Application) {
        ensureApplicationStateGate(application)
    }

    private fun ensureApplicationStateGate(application: Application): NetworkApplicationStateGate {
        applicationStateGate?.let { return it }

        val gate = NetworkApplicationStateGate()
        applicationStateGate = gate
        try {
            AppStateObserver.listeners += gate
            AppStateObserver.attach(application)
        } catch (exception: Exception) {
            AppStateObserver.listeners.remove(gate)
            applicationStateGate = null
            throw exception
        }

        return gate
    }

    /** Stops observing application state when network monitoring is not installed. */
    fun detachApplicationStateGate() {
        applicationStateGate?.let { AppStateObserver.listeners.remove(it) }
        applicationStateGate = null
    }

    /** Starts network monitoring and foreground-only `network.change` event emission. */
    fun install(application: Application, openTelemetry: OpenTelemetry) {
        if (!installed.compareAndSet(false, true)) {
            return
        }

        var currentNetworkProvider: CurrentNetworkProvider? = null
        try {
            currentNetworkProvider = currentNetworkProviderFactory(application)
            if (currentNetworkProvider == null) {
                Logger.w(TAG, "ConnectivityManager unavailable. Network monitoring will not be installed.")
                detachApplicationStateGate()
                installed.set(false)
                return
            }

            val stateGate = ensureApplicationStateGate(application)

            val eventEmitter = NetworkChangeEventEmitter(
                openTelemetry.logsBridge[INSTRUMENTATION_SCOPE]
            )
            currentNetworkProvider.addNetworkChangeListener {
                    currentNetwork,
                    isAppForegrounded,
                    networkChangeTimestampMillis
                ->
                emitEventAndNotifyListeners(
                    currentNetwork,
                    isAppForegrounded,
                    networkChangeTimestampMillis,
                    eventEmitter
                )
            }
            currentNetworkProvider.start({ currentNetwork ->
                notifyListeners(CurrentNetworkAttributes.extract(currentNetwork))
            }) { stateGate.isAppForegrounded }
        } catch (exception: Exception) {
            Logger.w(TAG, "Failed to install network monitoring.", exception)
            detachApplicationStateGate()
            try {
                currentNetworkProvider?.close()
            } catch (cleanupException: RuntimeException) {
                Logger.w(TAG, "Failed to clean up network monitoring after installation failure.", cleanupException)
            }
            installed.set(false)
        }
    }

    private fun emitEventAndNotifyListeners(
        currentNetwork: CurrentNetwork,
        isAppForegrounded: Boolean,
        networkChangeTimestampMillis: Long,
        eventEmitter: NetworkChangeEventEmitter
    ) {
        val attributes = CurrentNetworkAttributes.extract(currentNetwork)
        eventEmitter.emit(
            attributes,
            isAppForegrounded,
            networkChangeTimestampMillis
        )
        notifyListeners(attributes)
    }

    private fun notifyListeners(attributes: Attributes) {
        attributeListeners.forEach { listener ->
            try {
                listener(attributes)
            } catch (exception: RuntimeException) {
                Logger.w(TAG, "Network change listener failed.", exception)
            }
        }
    }

    private companion object {
        const val TAG = "NetworkMonitor"
        const val INSTRUMENTATION_SCOPE = "com.splunk.rum.network"
    }
}
