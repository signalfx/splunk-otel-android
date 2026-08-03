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
import android.net.Network
import android.net.NetworkRequest
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class CurrentNetworkProviderImplTest {
    private val detector = mock(NetworkDetector::class.java)
    private val connectivityManager = mock(ConnectivityManager::class.java)
    private val request = mock(NetworkRequest::class.java)

    @Test
    fun detectsInitialNetworkAndRegistersCallback() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        `when`(detector.detectCurrentNetwork()).thenReturn(wifi)

        val provider = createProvider()

        assertEquals(wifi, provider.currentNetwork)
        verify(connectivityManager).registerNetworkCallback(
            any(NetworkRequest::class.java),
            any(ConnectivityManager.NetworkCallback::class.java)
        )
    }

    @Test
    @Config(sdk = [24])
    fun usesDefaultNetworkCallbackOnApi24AndNewer() {
        `when`(detector.detectCurrentNetwork()).thenReturn(CurrentNetworkProvider.UNKNOWN_NETWORK)
        var requestCreations = 0

        CurrentNetworkProviderImpl(detector, connectivityManager) {
            requestCreations++
            request
        }

        verify(connectivityManager).registerDefaultNetworkCallback(
            any(ConnectivityManager.NetworkCallback::class.java)
        )
        verify(connectivityManager, never()).registerNetworkCallback(
            any(NetworkRequest::class.java),
            any(ConnectivityManager.NetworkCallback::class.java)
        )
        assertEquals(0, requestCreations)
    }

    @Test
    fun refreshesAndNotifiesListenerWhenNetworkBecomesAvailable() {
        val unknown = CurrentNetworkProvider.UNKNOWN_NETWORK
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, subType = "LTE")
        `when`(detector.detectCurrentNetwork()).thenReturn(unknown, cellular)
        val provider = createProvider()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }

        registeredCallback().onAvailable(mock(Network::class.java))

        assertEquals(cellular, provider.currentNetwork)
        assertEquals(listOf(cellular), observed)
    }

    @Test
    fun publishesNoNetworkWhenActiveNetworkIsLost() {
        `when`(detector.detectCurrentNetwork())
            .thenReturn(CurrentNetwork(NetworkState.TRANSPORT_WIFI))
        val provider = createProvider()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }

        registeredCallback().onLost(mock(Network::class.java))

        assertEquals(CurrentNetworkProvider.NO_NETWORK, provider.currentNetwork)
        assertEquals(listOf(CurrentNetworkProvider.NO_NETWORK), observed)
    }

    @Test
    fun detectorFailureFallsBackToUnknownNetwork() {
        `when`(detector.detectCurrentNetwork()).thenThrow(IllegalStateException("unavailable"))

        val provider = createProvider()

        assertEquals(CurrentNetworkProvider.UNKNOWN_NETWORK, provider.currentNetwork)
    }

    @Test
    fun removedListenerIsNotNotified() {
        `when`(detector.detectCurrentNetwork())
            .thenReturn(CurrentNetwork(NetworkState.TRANSPORT_WIFI))
        val provider = createProvider()
        val observed = mutableListOf<CurrentNetwork>()
        val listener = NetworkChangeListener { observed += it }
        provider.addNetworkChangeListener(listener)
        provider.removeNetworkChangeListener(listener)

        registeredCallback().onLost(mock(Network::class.java))

        assertEquals(emptyList<CurrentNetwork>(), observed)
    }

    @Test
    fun closeUnregistersCallbackAndClearsListeners() {
        `when`(detector.detectCurrentNetwork())
            .thenReturn(CurrentNetwork(NetworkState.TRANSPORT_WIFI))
        val provider = createProvider()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }
        val callback = registeredCallback()

        provider.close()
        callback.onLost(mock(Network::class.java))

        verify(connectivityManager).unregisterNetworkCallback(callback)
        assertEquals(emptyList<CurrentNetwork>(), observed)
    }

    @Test
    fun repeatedCloseOnlyUnregistersCallbackOnce() {
        `when`(detector.detectCurrentNetwork()).thenReturn(CurrentNetworkProvider.UNKNOWN_NETWORK)
        val provider = createProvider()
        val callback = registeredCallback()

        provider.close()
        provider.close()

        verify(connectivityManager, times(1)).unregisterNetworkCallback(callback)
    }

    @Test
    fun unregisterFailureIsLoggedAndDoesNotEscapeClose() {
        `when`(detector.detectCurrentNetwork()).thenReturn(CurrentNetworkProvider.UNKNOWN_NETWORK)
        val provider = createProvider()
        val callback = registeredCallback()
        doThrow(IllegalArgumentException("not registered"))
            .`when`(connectivityManager)
            .unregisterNetworkCallback(callback)

        provider.close()

        assertTrue(
            ShadowLog.getLogsForTag("CurrentNetworkProvider").any {
                it.msg == "Failed to unregister network callbacks."
            }
        )
    }

    @Test
    fun failedRegistrationDoesNotPreventCurrentNetworkAccess() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        `when`(detector.detectCurrentNetwork()).thenReturn(wifi)
        doThrow(SecurityException("denied"))
            .`when`(connectivityManager)
            .registerNetworkCallback(
                any(NetworkRequest::class.java),
                any(ConnectivityManager.NetworkCallback::class.java)
            )

        val provider = createProvider()

        assertEquals(wifi, provider.currentNetwork)
        verify(
            connectivityManager,
            never()
        ).unregisterNetworkCallback(any(ConnectivityManager.NetworkCallback::class.java))
    }

    private fun createProvider(): CurrentNetworkProviderImpl =
        CurrentNetworkProviderImpl(detector, connectivityManager) { request }

    private fun registeredCallback(): ConnectivityManager.NetworkCallback {
        val captor = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback::class.java)
        verify(connectivityManager).registerNetworkCallback(any(NetworkRequest::class.java), captor.capture())
        return captor.value
    }
}
