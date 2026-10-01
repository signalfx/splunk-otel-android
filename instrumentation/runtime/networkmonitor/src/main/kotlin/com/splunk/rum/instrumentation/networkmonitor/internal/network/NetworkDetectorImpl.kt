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

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.instrumentation.networkmonitor.internal.model.Carrier
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState

internal class NetworkDetectorImpl(private val context: Context, private val connectivityManager: ConnectivityManager) :
    NetworkDetector {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val carrierFinder = CarrierFinder(context, telephonyManager)

    override fun detectNetwork(): NetworkObservation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        detectUsingCapabilities()
    } else {
        detectUsingLegacyApi()
    }

    override fun detectNetwork(network: Network): NetworkObservation {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkObservation(
                currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK,
                activeNetworkIdentity = network,
                activeNetworkIdentityKnown = true
            )
        return NetworkObservation(
            currentNetwork = currentNetworkFromCapabilities(capabilities),
            activeNetworkIdentity = network,
            activeNetworkIdentityKnown = true
        )
    }

    override fun observeNetwork(network: Network, capabilities: NetworkCapabilities): NetworkObservation =
        NetworkObservation(
            currentNetwork = currentNetworkFromCapabilities(capabilities),
            activeNetworkIdentity = network,
            activeNetworkIdentityKnown = true
        )

    @RequiresApi(Build.VERSION_CODES.M)
    private fun detectUsingCapabilities(): NetworkObservation {
        // Keep the handle used to read capabilities so the provider can compare identity later.
        val activeNetworkIdentity = connectivityManager.activeNetwork
            ?: return NetworkObservation(
                currentNetwork = CurrentNetworkProvider.NO_NETWORK,
                activeNetworkIdentity = null,
                activeNetworkIdentityKnown = true
            )
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetworkIdentity)
            ?: return NetworkObservation(
                currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK,
                activeNetworkIdentity = activeNetworkIdentity,
                activeNetworkIdentityKnown = true
            )

        return NetworkObservation(
            currentNetwork = currentNetworkFromCapabilities(capabilities),
            activeNetworkIdentity = activeNetworkIdentity,
            activeNetworkIdentityKnown = true
        )
    }

    private fun currentNetworkFromCapabilities(capabilities: NetworkCapabilities): CurrentNetwork = when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
            buildNetwork(NetworkState.TRANSPORT_CELLULAR, includeCarrierSubtype = true)
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
            buildNetwork(NetworkState.TRANSPORT_WIFI)
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
            buildNetwork(NetworkState.TRANSPORT_VPN)
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
            buildNetwork(NetworkState.TRANSPORT_WIRED)
        else -> CurrentNetworkProvider.UNKNOWN_NETWORK
    }

    @Suppress("DEPRECATION")
    private fun detectUsingLegacyApi(): NetworkObservation {
        // API 21-22 expose activeNetworkInfo only, so identity comparison is unavailable.
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
            ?: return NetworkObservation(
                currentNetwork = CurrentNetworkProvider.NO_NETWORK,
                activeNetworkIdentity = null,
                activeNetworkIdentityKnown = false
            )
        if (!activeNetworkInfo.isConnected) {
            return NetworkObservation(
                currentNetwork = CurrentNetworkProvider.NO_NETWORK,
                activeNetworkIdentity = null,
                activeNetworkIdentityKnown = false
            )
        }
        val state = when (activeNetworkInfo.type) {
            ConnectivityManager.TYPE_MOBILE -> NetworkState.TRANSPORT_CELLULAR
            ConnectivityManager.TYPE_WIFI -> NetworkState.TRANSPORT_WIFI
            ConnectivityManager.TYPE_VPN -> NetworkState.TRANSPORT_VPN
            ConnectivityManager.TYPE_ETHERNET -> NetworkState.TRANSPORT_WIRED
            else -> return NetworkObservation(
                currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK,
                activeNetworkIdentity = null,
                activeNetworkIdentityKnown = false
            )
        }
        return NetworkObservation(
            currentNetwork = buildCurrentNetwork(
                state,
                if (state == NetworkState.TRANSPORT_CELLULAR) carrierFinder.get() else null,
                activeNetworkInfo.subtypeName
            ),
            activeNetworkIdentity = null,
            activeNetworkIdentityKnown = false
        )
    }

    private fun buildNetwork(state: NetworkState, includeCarrierSubtype: Boolean = false): CurrentNetwork =
        buildCurrentNetwork(
            state,
            if (includeCarrierSubtype) carrierFinder.get() else null,
            if (includeCarrierSubtype) findSubtype() else null
        )

    @Suppress("MissingPermission")
    private fun findSubtype(): String? {
        if (telephonyManager == null) {
            Logger.w(TAG, "Cannot determine network subtype: telephony service unavailable.")
            return null
        }
        return try {
            if (!hasTelephonyRadioAccessFeature(context)) {
                Logger.w(TAG, "Cannot determine network subtype: telephony radio access feature unavailable.")
                null
            } else if (!hasPhoneStatePermission(context)) {
                Logger.w(TAG, "Cannot determine network subtype: read phone state permission unavailable.")
                null
            } else {
                val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    telephonyManager.dataNetworkType
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.networkType
                }
                getNetworkTypeName(networkType)
            }
        } catch (exception: SecurityException) {
            Logger.w(TAG, "SecurityException when accessing network type.", exception)
            null
        } catch (exception: RuntimeException) {
            Logger.w(TAG, "Failed to access network type.", exception)
            null
        }
    }

    private fun buildCurrentNetwork(state: NetworkState, carrier: Carrier?, subtype: String?): CurrentNetwork =
        CurrentNetwork(state = state, carrier = carrier, subType = subtype)

    private companion object {
        private const val TAG = "NetworkDetector"
    }
}
