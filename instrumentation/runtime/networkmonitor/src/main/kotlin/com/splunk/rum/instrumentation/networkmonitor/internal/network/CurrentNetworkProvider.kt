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
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import java.io.Closeable

internal interface CurrentNetworkProvider : Closeable {
    val currentNetwork: CurrentNetwork

    fun refreshNetworkStatus(): CurrentNetwork

    fun addNetworkChangeListener(listener: NetworkChangeListener)

    fun removeNetworkChangeListener(listener: NetworkChangeListener)

    companion object {
        val NO_NETWORK: CurrentNetwork = CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE)
        val UNKNOWN_NETWORK: CurrentNetwork = CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN)

        fun create(context: Context): CurrentNetworkProvider? {
            val applicationContext = context.applicationContext ?: context
            val connectivityManager =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return null
            return CurrentNetworkProviderImpl(
                NetworkDetector.create(applicationContext, connectivityManager),
                connectivityManager
            )
        }
    }
}
