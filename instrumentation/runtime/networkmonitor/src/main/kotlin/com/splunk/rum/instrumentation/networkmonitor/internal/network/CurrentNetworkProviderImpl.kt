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

package com.splunk.rum.instrumentation.networkmonitor.internal.network

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.splunk.rum.common.utils.thread.NamedThreadFactory
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class CurrentNetworkProviderImpl(
    private val networkDetector: NetworkDetector,
    private val connectivityManager: ConnectivityManager,
    private val initialDetectionExecutor: ExecutorService = createInitialDetectionExecutor(),
    private val createNetworkMonitoringRequest: () -> NetworkRequest = ::createNetworkMonitoringRequest
) : CurrentNetworkProvider {
    override val currentNetwork: CurrentNetwork
        get() = networkSnapshot.get().network

    private val callbackRef = AtomicReference<NetworkCallback>()
    private val listeners = CopyOnWriteArrayList<NetworkChangeListener>()
    private val started = AtomicBoolean()
    private val closed = AtomicBoolean()
    private val networkSnapshot = AtomicReference(
        NetworkSnapshot(callbackObserved = false, network = CurrentNetworkProvider.UNKNOWN_NETWORK)
    )

    override fun start(initialNetworkListener: NetworkChangeListener) {
        if (!started.compareAndSet(false, true) || closed.get()) {
            return
        }

        val initialSnapshot = networkSnapshot.get()
        try {
            registerNetworkCallbacks(createNetworkMonitoringRequest)
        } catch (exception: Exception) {
            Log.w(
                TAG,
                "Failed to register network callbacks. Network monitoring is disabled.",
                exception
            )
        }
        detectInitialNetwork(initialSnapshot, initialNetworkListener)
    }

    override fun refreshNetworkStatus(): CurrentNetwork {
        val detectedNetwork = detectCurrentNetwork()
        val currentSnapshot = networkSnapshot.get()
        networkSnapshot.set(currentSnapshot.copy(network = detectedNetwork))
        return detectedNetwork
    }

    private fun detectCurrentNetwork(): CurrentNetwork = try {
        networkDetector.detectCurrentNetwork()
    } catch (exception: Exception) {
        Log.w(TAG, "Failed to detect the current network.", exception)
        CurrentNetworkProvider.UNKNOWN_NETWORK
    }

    override fun addNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    override fun removeNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
    }

    override fun close() {
        closed.set(true)
        callbackRef.getAndSet(null)?.let(::unregisterNetworkCallback)
        initialDetectionExecutor.shutdownNow()
        listeners.clear()
    }

    private fun detectInitialNetwork(initialSnapshot: NetworkSnapshot, initialNetworkListener: NetworkChangeListener) {
        try {
            initialDetectionExecutor.execute {
                try {
                    if (closed.get() || networkSnapshot.get() !== initialSnapshot) {
                        return@execute
                    }

                    val detectedNetwork = detectCurrentNetwork()
                    if (!closed.get()) {
                        val initialStatePublished = networkSnapshot.compareAndSet(
                            initialSnapshot,
                            initialSnapshot.copy(network = detectedNetwork)
                        )
                        if (initialStatePublished) {
                            initialNetworkListener.onNetworkChange(detectedNetwork)

                            // A callback based network detection can happen immediately. Reapply its latest
                            // state to attributes without emitting another network change event.
                            val latestSnapshot = networkSnapshot.get()
                            if (latestSnapshot.callbackObserved) {
                                initialNetworkListener.onNetworkChange(latestSnapshot.network)
                            }
                        }
                    }
                } finally {
                    initialDetectionExecutor.shutdown()
                }
            }
        } catch (exception: RejectedExecutionException) {
            if (!closed.get()) {
                Log.w(TAG, "Failed to schedule initial network detection.", exception)
            }
        }
    }

    private fun invalidateInitialDetection() {
        val currentSnapshot = networkSnapshot.get()
        networkSnapshot.set(currentSnapshot.copy(callbackObserved = true))
    }

    private fun registerNetworkCallbacks(createNetworkMonitoringRequest: () -> NetworkRequest) {
        val callback = ConnectionMonitor()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            connectivityManager.registerNetworkCallback(createNetworkMonitoringRequest(), callback)
        }
        callbackRef.set(callback)
        if (closed.get() && callbackRef.compareAndSet(callback, null)) {
            unregisterNetworkCallback(callback)
        }
    }

    private fun unregisterNetworkCallback(callback: NetworkCallback) {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Failed to unregister network callbacks.", exception)
        }
    }

    private fun notifyListeners(activeNetwork: CurrentNetwork) {
        listeners.forEach { it.onNetworkChange(activeNetwork) }
    }

    private inner class ConnectionMonitor : NetworkCallback() {
        override fun onAvailable(network: Network) {
            invalidateInitialDetection()
            val activeNetwork = refreshNetworkStatus()
            Log.d(TAG, "onAvailable: currentNetwork=$activeNetwork")
            notifyListeners(activeNetwork)
        }

        override fun onLost(network: Network) {
            invalidateInitialDetection()
            val noNetwork = CurrentNetworkProvider.NO_NETWORK
            val currentSnapshot = networkSnapshot.get()
            networkSnapshot.set(currentSnapshot.copy(network = noNetwork))
            Log.d(TAG, "onLost: currentNetwork=$noNetwork")
            notifyListeners(noNetwork)
        }
    }

    private companion object {
        private const val TAG = "CurrentNetworkProvider"

        fun createInitialDetectionExecutor(): ExecutorService =
            Executors.newSingleThreadExecutor(NamedThreadFactory("SplunkNetworkMonitor"))

        fun createNetworkMonitoringRequest(): NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()
    }

    private data class NetworkSnapshot(val callbackObserved: Boolean, val network: CurrentNetwork)
}
