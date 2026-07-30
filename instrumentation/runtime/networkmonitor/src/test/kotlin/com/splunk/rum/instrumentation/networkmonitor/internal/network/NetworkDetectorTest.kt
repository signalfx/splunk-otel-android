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

@file:Suppress("DEPRECATION")

package com.splunk.rum.instrumentation.networkmonitor.internal.network

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NetworkDetectorTest {
    private val context = mock(Context::class.java)
    private val connectivityManager = mock(ConnectivityManager::class.java)
    private val detector = NetworkDetector.create(context, connectivityManager)

    @Test
    fun noActiveNetworkIsUnavailable() {
        `when`(connectivityManager.activeNetwork).thenReturn(null)

        assertEquals(NetworkState.NO_NETWORK_AVAILABLE, detector.detectCurrentNetwork().state)
    }

    @Test
    fun missingCapabilitiesIsUnknown() {
        val network = mock(Network::class.java)
        `when`(connectivityManager.activeNetwork).thenReturn(network)
        `when`(connectivityManager.getNetworkCapabilities(network)).thenReturn(null)

        assertEquals(NetworkState.TRANSPORT_UNKNOWN, detector.detectCurrentNetwork().state)
    }

    @Test
    fun detectsEachSupportedCapabilityTransport() {
        assertTransport(NetworkCapabilities.TRANSPORT_CELLULAR, NetworkState.TRANSPORT_CELLULAR)
        assertTransport(NetworkCapabilities.TRANSPORT_WIFI, NetworkState.TRANSPORT_WIFI)
        assertTransport(NetworkCapabilities.TRANSPORT_VPN, NetworkState.TRANSPORT_VPN)
        assertTransport(NetworkCapabilities.TRANSPORT_ETHERNET, NetworkState.TRANSPORT_WIRED)
    }

    @Test
    fun unsupportedCapabilityTransportIsUnknown() {
        assertTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH, NetworkState.TRANSPORT_UNKNOWN)
    }

    @Test
    fun missingTelephonyServiceLogsUnavailableSubtype() {
        val currentNetwork = detectCellular(context)

        assertNull(currentNetwork.subType)
        assertLogContains("Cannot determine network subtype: telephony service unavailable.")
    }

    @Test
    fun missingTelephonyFeatureLogsUnavailableSubtype() {
        val telephonyContext = mock(Context::class.java)
        val telephonyManager = mock(TelephonyManager::class.java)
        val packageManager = mock(PackageManager::class.java)
        `when`(telephonyContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager)
        `when`(telephonyContext.packageManager).thenReturn(packageManager)
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(false)

        val currentNetwork = detectCellular(telephonyContext)

        assertNull(currentNetwork.subType)
        assertLogContains("Cannot determine network subtype: telephony feature unavailable.")
    }

    @Test
    fun missingPhoneStatePermissionLogsUnavailableSubtype() {
        val telephonyContext = mock(Context::class.java)
        val telephonyManager = mock(TelephonyManager::class.java)
        val packageManager = mock(PackageManager::class.java)
        `when`(telephonyContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager)
        `when`(telephonyContext.packageManager).thenReturn(packageManager)
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true)
        `when`(telephonyContext.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        val currentNetwork = detectCellular(telephonyContext)

        assertNull(currentNetwork.subType)
        assertLogContains("Cannot determine network subtype: read phone state permission unavailable.")
    }

    @Test
    fun cellularNetworkIncludesCurrentRadioType() {
        val (telephonyContext, telephonyManager) = contextWithTelephonyAccess()
        `when`(telephonyManager.dataNetworkType).thenReturn(TelephonyManager.NETWORK_TYPE_LTE)

        val currentNetwork = detectCellular(telephonyContext)

        assertEquals("LTE", currentNetwork.subType)
    }

    @Test
    fun securityFailureWhileReadingRadioTypeOmitsSubtype() {
        val (telephonyContext, telephonyManager) = contextWithTelephonyAccess()
        `when`(telephonyManager.dataNetworkType).thenThrow(SecurityException("denied"))

        val currentNetwork = detectCellular(telephonyContext)

        assertNull(currentNetwork.subType)
        assertLogContains("SecurityException when accessing network type.")
    }

    @Test
    fun runtimeFailureWhileReadingRadioTypeOmitsSubtype() {
        val (telephonyContext, telephonyManager) = contextWithTelephonyAccess()
        `when`(telephonyManager.dataNetworkType).thenThrow(IllegalStateException("unavailable"))

        val currentNetwork = detectCellular(telephonyContext)

        assertNull(currentNetwork.subType)
        assertLogContains("Failed to access network type.")
    }

    @Test
    @Config(sdk = [23])
    fun api23UsesCapabilitiesInsteadOfLegacyApi() {
        val network = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)
        `when`(connectivityManager.activeNetwork).thenReturn(network)
        `when`(connectivityManager.getNetworkCapabilities(network)).thenReturn(capabilities)
        `when`(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)

        val currentNetwork = detector.detectCurrentNetwork()

        assertEquals(NetworkState.TRANSPORT_WIFI, currentNetwork.state)
        verify(connectivityManager, never()).activeNetworkInfo
    }

    @Test
    @Config(sdk = [22])
    fun legacyApiReportsNoNetworkWhenNetworkInfoIsMissing() {
        `when`(connectivityManager.activeNetworkInfo).thenReturn(null)

        assertEquals(NetworkState.NO_NETWORK_AVAILABLE, detector.detectCurrentNetwork().state)
    }

    @Test
    @Config(sdk = [22])
    fun legacyApiDetectsMobileNetworkAndSubtype() {
        val networkInfo = mock(NetworkInfo::class.java)
        `when`(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)
        `when`(networkInfo.type).thenReturn(ConnectivityManager.TYPE_MOBILE)
        `when`(networkInfo.subtypeName).thenReturn("HSPA")

        val network = detector.detectCurrentNetwork()

        assertEquals(NetworkState.TRANSPORT_CELLULAR, network.state)
        assertEquals("HSPA", network.subType)
    }

    @Test
    @Config(sdk = [22])
    fun legacyUnsupportedTransportIsUnknown() {
        val networkInfo = mock(NetworkInfo::class.java)
        `when`(connectivityManager.activeNetworkInfo).thenReturn(networkInfo)
        `when`(networkInfo.type).thenReturn(ConnectivityManager.TYPE_BLUETOOTH)

        assertEquals(NetworkState.TRANSPORT_UNKNOWN, detector.detectCurrentNetwork().state)
    }

    private fun assertTransport(transport: Int, expected: NetworkState) {
        val network = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)
        `when`(connectivityManager.activeNetwork).thenReturn(network)
        `when`(connectivityManager.getNetworkCapabilities(network)).thenReturn(capabilities)
        `when`(capabilities.hasTransport(transport)).thenReturn(true)

        assertEquals(expected, detector.detectCurrentNetwork().state)
    }

    private fun detectCellular(detectorContext: Context) =
        NetworkDetector.create(detectorContext, connectivityManager).run {
            val network = mock(Network::class.java)
            val capabilities = mock(NetworkCapabilities::class.java)
            `when`(connectivityManager.activeNetwork).thenReturn(network)
            `when`(connectivityManager.getNetworkCapabilities(network)).thenReturn(capabilities)
            `when`(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)
            detectCurrentNetwork()
        }

    private fun contextWithTelephonyAccess(): Pair<Context, TelephonyManager> {
        val telephonyContext = mock(Context::class.java)
        val telephonyManager = mock(TelephonyManager::class.java)
        val packageManager = mock(PackageManager::class.java)
        `when`(telephonyContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager)
        `when`(telephonyContext.packageManager).thenReturn(packageManager)
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true)
        `when`(telephonyContext.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        return telephonyContext to telephonyManager
    }

    private fun assertLogContains(message: String) {
        assertTrue(
            "Expected log message: $message",
            ShadowLog.getLogsForTag("NetworkDetector").any { it.msg == message }
        )
    }
}
