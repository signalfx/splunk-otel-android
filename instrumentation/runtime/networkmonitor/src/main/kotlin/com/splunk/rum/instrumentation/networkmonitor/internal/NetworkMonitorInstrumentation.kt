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
import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateGate
import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateObserver
import com.splunk.rum.instrumentation.networkmonitor.internal.network.CurrentNetworkProvider
import com.splunk.rum.instrumentation.networkmonitor.internal.network.NetworkChangeListener
import com.splunk.rum.instrumentation.networkmonitor.internal.telemetry.CurrentNetworkAttributes
import com.splunk.rum.instrumentation.networkmonitor.internal.telemetry.NetworkChangeEventEmitter
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runtime implementation of network change monitoring.
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class NetworkMonitorInstrumentation : Closeable {
    private val attributeListeners = CopyOnWriteArrayList<(Attributes) -> Unit>()
    private val installed = AtomicBoolean()
    private var currentNetworkProvider: Closeable? = null
    private var applicationStateObserver: Closeable? = null

    internal var currentNetworkProviderFactory: (Application) -> CurrentNetworkProvider? =
        { application -> CurrentNetworkProvider.create(application) }
    internal var applicationStateObserverFactory: (Application, NetworkApplicationStateGate) -> Closeable =
        ::NetworkApplicationStateObserver

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
        applicationStateObserver = try {
            applicationStateObserverFactory(application, applicationStateGate)
        } catch (exception: RuntimeException) {
            Log.w(
                TAG,
                "Failed to observe application foreground/background state. Network change events will remain enabled.",
                exception
            )
            null
        }

        val eventEmitter = NetworkChangeEventEmitter(
            openTelemetry.logsBridge[INSTRUMENTATION_SCOPE],
            applicationStateGate
        )
        currentNetworkProvider.addNetworkChangeListener(
            NetworkChangeListener { currentNetwork ->
                val attributes = CurrentNetworkAttributes.extract(currentNetwork)
                attributeListeners.forEach { listener ->
                    try {
                        listener(attributes)
                    } catch (exception: RuntimeException) {
                        Log.w(TAG, "Network change listener failed.", exception)
                    }
                }
                eventEmitter.emit(attributes)
            }
        )
        this.currentNetworkProvider = currentNetworkProvider
    }

    /** Stops network and application-state callbacks. */
    override fun close() {
        if (!installed.compareAndSet(true, false)) {
            return
        }
        closeSafely(currentNetworkProvider, "Failed to stop network monitoring.")
        closeSafely(applicationStateObserver, "Failed to stop application-state monitoring.")
        currentNetworkProvider = null
        applicationStateObserver = null
    }

    private fun closeSafely(closeable: Closeable?, message: String) {
        try {
            closeable?.close()
        } catch (exception: RuntimeException) {
            Log.w(TAG, message, exception)
        }
    }

    private companion object {
        private const val TAG = "NetworkMonitor"
        private const val INSTRUMENTATION_SCOPE = "com.splunk.rum.network"
    }
}
