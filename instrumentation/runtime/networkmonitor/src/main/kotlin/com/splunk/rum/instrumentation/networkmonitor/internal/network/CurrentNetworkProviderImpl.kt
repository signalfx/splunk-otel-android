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
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

internal class CurrentNetworkProviderImpl(
    private val networkDetector: NetworkDetector,
    private val connectivityManager: ConnectivityManager,
    createNetworkMonitoringRequest: () -> NetworkRequest = ::createNetworkMonitoringRequest
) : CurrentNetworkProvider {
    @Volatile
    override var currentNetwork: CurrentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK
        private set

    private val callbackRef = AtomicReference<NetworkCallback>()
    private val listeners = CopyOnWriteArrayList<NetworkChangeListener>()

    init {
        refreshNetworkStatus()
        try {
            registerNetworkCallbacks(createNetworkMonitoringRequest)
        } catch (exception: Exception) {
            Log.w(
                TAG,
                "Failed to register network callbacks. Network monitoring is disabled.",
                exception
            )
        }
    }

    override fun refreshNetworkStatus(): CurrentNetwork {
        currentNetwork = try {
            networkDetector.detectCurrentNetwork()
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to detect the current network.", exception)
            CurrentNetworkProvider.UNKNOWN_NETWORK
        }
        return currentNetwork
    }

    override fun addNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    override fun removeNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
    }

    override fun close() {
        callbackRef.getAndSet(null)?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Failed to unregister network callbacks.", exception)
            }
        }
        listeners.clear()
    }

    private fun registerNetworkCallbacks(createNetworkMonitoringRequest: () -> NetworkRequest) {
        val callback = ConnectionMonitor()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            connectivityManager.registerNetworkCallback(createNetworkMonitoringRequest(), callback)
        }
        callbackRef.set(callback)
    }

    private fun notifyListeners(activeNetwork: CurrentNetwork) {
        listeners.forEach { it.onNetworkChange(activeNetwork) }
    }

    private inner class ConnectionMonitor : NetworkCallback() {
        override fun onAvailable(network: Network) {
            val activeNetwork = refreshNetworkStatus()
            Log.d(TAG, "onAvailable: currentNetwork=$activeNetwork")
            notifyListeners(activeNetwork)
        }

        override fun onLost(network: Network) {
            val noNetwork = CurrentNetworkProvider.NO_NETWORK
            currentNetwork = noNetwork
            Log.d(TAG, "onLost: currentNetwork=$noNetwork")
            notifyListeners(noNetwork)
        }
    }

    private companion object {
        private const val TAG = "CurrentNetworkProvider"

        fun createNetworkMonitoringRequest(): NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()
    }
}
