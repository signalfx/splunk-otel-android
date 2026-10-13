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
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork

/** A single network read containing both its telemetry data and runtime identity. */
internal data class NetworkObservation(
    val currentNetwork: CurrentNetwork,
    val activeNetworkIdentity: Network?,
    // True means null is a known "no active network"; false means this API cannot provide identity.
    val activeNetworkIdentityKnown: Boolean
)

internal interface NetworkDetector {
    fun detectNetwork(): NetworkObservation

    /** Reads capabilities for a callback-provided network off the framework callback thread. */
    fun detectNetwork(network: Network): NetworkObservation

    /** Builds an observation from callback data for one specific network. */
    fun observeNetwork(network: Network, capabilities: NetworkCapabilities): NetworkObservation

    companion object {
        fun create(context: Context, connectivityManager: ConnectivityManager): NetworkDetector =
            NetworkDetectorImpl(context, connectivityManager)
    }
}
