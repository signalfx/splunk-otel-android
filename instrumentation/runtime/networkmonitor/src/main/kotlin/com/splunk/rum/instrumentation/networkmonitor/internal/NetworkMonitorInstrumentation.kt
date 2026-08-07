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
import android.util.Log
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateGate
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

    /** Starts network monitoring and foreground-only `network.change` event emission. */
    fun install(application: Application, openTelemetry: OpenTelemetry) {
        if (!installed.compareAndSet(false, true)) {
            return
        }

        val currentNetworkProvider = currentNetworkProviderFactory(application)
        if (currentNetworkProvider == null) {
            Log.w(TAG, "ConnectivityManager unavailable. Network monitoring will not be installed.")
            installed.set(false)
            return
        }

        val applicationStateGate = NetworkApplicationStateGate()
        AppStateObserver.listeners += applicationStateGate
        AppStateObserver.attach(application)

        val eventEmitter = NetworkChangeEventEmitter(
            openTelemetry.logsBridge[INSTRUMENTATION_SCOPE],
            applicationStateGate
        )
        currentNetworkProvider.addNetworkChangeListener { currentNetwork ->
            val attributes = CurrentNetworkAttributes.extract(currentNetwork)
            eventEmitter.emit(attributes)
            notifyAttributeListeners(attributes)
        }
        currentNetworkProvider.start { currentNetwork ->
            notifyAttributeListeners(CurrentNetworkAttributes.extract(currentNetwork))
        }
    }

    private fun notifyAttributeListeners(attributes: Attributes) {
        attributeListeners.forEach { listener ->
            try {
                listener(attributes)
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Network change listener failed.", exception)
            }
        }
    }

    private companion object {
        const val TAG = "NetworkMonitor"
        const val INSTRUMENTATION_SCOPE = "com.splunk.rum.network"
    }
}
