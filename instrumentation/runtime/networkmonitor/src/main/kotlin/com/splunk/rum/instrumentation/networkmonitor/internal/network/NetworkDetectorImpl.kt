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
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.splunk.rum.instrumentation.networkmonitor.internal.model.Carrier
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState

internal class NetworkDetectorImpl(private val context: Context, private val connectivityManager: ConnectivityManager) :
    NetworkDetector {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val carrierFinder = CarrierFinder(context, telephonyManager)

    override fun detectCurrentNetwork(): CurrentNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        detectUsingCapabilities()
    } else {
        detectUsingLegacyApi()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun detectUsingCapabilities(): CurrentNetwork {
        val activeNetwork = connectivityManager.activeNetwork ?: return CurrentNetworkProvider.NO_NETWORK
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return CurrentNetworkProvider.UNKNOWN_NETWORK

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                buildNetwork(NetworkState.TRANSPORT_CELLULAR, includeSubtype = true)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                buildNetwork(NetworkState.TRANSPORT_WIFI)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
                buildNetwork(NetworkState.TRANSPORT_VPN)
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                buildNetwork(NetworkState.TRANSPORT_WIRED)
            else -> CurrentNetworkProvider.UNKNOWN_NETWORK
        }
    }

    @Suppress("DEPRECATION")
    private fun detectUsingLegacyApi(): CurrentNetwork {
        val activeNetwork = connectivityManager.activeNetworkInfo ?: return CurrentNetworkProvider.NO_NETWORK
        val state = when (activeNetwork.type) {
            ConnectivityManager.TYPE_MOBILE -> NetworkState.TRANSPORT_CELLULAR
            ConnectivityManager.TYPE_WIFI -> NetworkState.TRANSPORT_WIFI
            ConnectivityManager.TYPE_VPN -> NetworkState.TRANSPORT_VPN
            ConnectivityManager.TYPE_ETHERNET -> NetworkState.TRANSPORT_WIRED
            else -> return CurrentNetworkProvider.UNKNOWN_NETWORK
        }
        return buildCurrentNetwork(
            state,
            if (state == NetworkState.TRANSPORT_CELLULAR) carrierFinder.get() else null,
            activeNetwork.subtypeName
        )
    }

    private fun buildNetwork(state: NetworkState, includeSubtype: Boolean = false): CurrentNetwork =
        buildCurrentNetwork(
            state,
            if (state == NetworkState.TRANSPORT_CELLULAR) carrierFinder.get() else null,
            if (includeSubtype) findSubtype() else null
        )

    @Suppress("MissingPermission")
    private fun findSubtype(): String? {
        if (telephonyManager == null) {
            Log.w(TAG, "Cannot determine network subtype: telephony service unavailable.")
            return null
        }
        if (!hasTelephonyFeature(context)) {
            Log.w(TAG, "Cannot determine network subtype: telephony feature unavailable.")
            return null
        }
        if (!hasPhoneStatePermission(context)) {
            Log.w(TAG, "Cannot determine network subtype: read phone state permission unavailable.")
            return null
        }
        return try {
            val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                telephonyManager.dataNetworkType
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.networkType
            }
            getNetworkTypeName(networkType)
        } catch (exception: SecurityException) {
            Log.w(TAG, "SecurityException when accessing network type.", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Failed to access network type.", exception)
            null
        }
    }

    private fun buildCurrentNetwork(state: NetworkState, carrier: Carrier?, subtype: String?): CurrentNetwork =
        CurrentNetwork(state = state, carrier = carrier, subType = subtype)

    private companion object {
        private const val TAG = "NetworkDetector"
    }
}
