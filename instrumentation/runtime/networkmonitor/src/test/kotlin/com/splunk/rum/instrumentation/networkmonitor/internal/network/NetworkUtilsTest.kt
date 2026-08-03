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
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class NetworkUtilsTest {
    @Test
    @Config(sdk = [22])
    fun phoneStatePermissionIsImplicitBeforeRuntimePermissions() {
        assertTrue(hasPhoneStatePermission(mock(Context::class.java)))
    }

    @Test
    @Config(sdk = [23])
    fun readPhoneStatePermissionAllowsTelephonyAccess() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        assertTrue(hasPhoneStatePermission(context))
    }

    @Test
    @Config(sdk = [33])
    fun basicPhoneStatePermissionAllowsTelephonyAccess() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(context.checkSelfPermission(android.Manifest.permission.READ_BASIC_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        assertTrue(hasPhoneStatePermission(context))
    }

    @Test
    @Config(sdk = [33])
    fun deniedPhoneStatePermissionsPreventTelephonyAccess() {
        val context = mock(Context::class.java)
        `when`(context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(context.checkSelfPermission(android.Manifest.permission.READ_BASIC_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        assertFalse(hasPhoneStatePermission(context))
    }

    @Test
    fun detectsTelephonyFeature() {
        val context = mock(Context::class.java)
        val packageManager = mock(PackageManager::class.java)
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true)

        assertTrue(hasTelephonyFeature(context))
    }

    @Test
    fun missingPackageManagerDoesNotReportTelephonyFeature() {
        val context = mock(Context::class.java)
        `when`(context.packageManager).thenReturn(null)

        assertFalse(hasTelephonyFeature(context))
    }

    @Test
    fun mapsKnownAndUnknownNetworkTypes() {
        val expectedNames = mapOf(
            TelephonyManager.NETWORK_TYPE_GPRS to "GPRS",
            TelephonyManager.NETWORK_TYPE_EDGE to "EDGE",
            TelephonyManager.NETWORK_TYPE_UMTS to "UMTS",
            TelephonyManager.NETWORK_TYPE_HSDPA to "HSDPA",
            TelephonyManager.NETWORK_TYPE_HSUPA to "HSUPA",
            TelephonyManager.NETWORK_TYPE_HSPA to "HSPA",
            TelephonyManager.NETWORK_TYPE_HSPAP to "HSPAP",
            TelephonyManager.NETWORK_TYPE_GSM to "GSM",
            TelephonyManager.NETWORK_TYPE_TD_SCDMA to "TD_SCDMA",
            TelephonyManager.NETWORK_TYPE_CDMA to "CDMA",
            TelephonyManager.NETWORK_TYPE_1xRTT to "1xRTT",
            TelephonyManager.NETWORK_TYPE_EVDO_0 to "EVDO_0",
            TelephonyManager.NETWORK_TYPE_EVDO_A to "EVDO_A",
            TelephonyManager.NETWORK_TYPE_EVDO_B to "EVDO_B",
            TelephonyManager.NETWORK_TYPE_EHRPD to "EHRPD",
            TelephonyManager.NETWORK_TYPE_IDEN to "IDEN",
            TelephonyManager.NETWORK_TYPE_LTE to "LTE",
            TelephonyManager.NETWORK_TYPE_IWLAN to "IWLAN",
            TelephonyManager.NETWORK_TYPE_NR to "NR"
        )

        expectedNames.forEach { (networkType, expectedName) ->
            assertEquals(expectedName, getNetworkTypeName(networkType))
        }
        assertEquals("UNKNOWN", getNetworkTypeName(Int.MAX_VALUE))
    }
}
