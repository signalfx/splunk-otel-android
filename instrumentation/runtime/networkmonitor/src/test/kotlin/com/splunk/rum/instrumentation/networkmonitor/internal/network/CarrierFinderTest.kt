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
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
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
@Config(sdk = [28])
class CarrierFinderTest {
    private val context = mock(Context::class.java)
    private val packageManager = mock(PackageManager::class.java)
    private val telephonyManager = mock(TelephonyManager::class.java)

    init {
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(true)
        `when`(context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
    }

    @Test
    fun extractsModernCarrierFields() {
        `when`(telephonyManager.simCarrierId).thenReturn(123)
        `when`(telephonyManager.simCarrierIdName).thenReturn("Example")
        `when`(telephonyManager.simOperator).thenReturn("310260")
        `when`(telephonyManager.simCountryIso).thenReturn("us")

        val carrier = CarrierFinder(context, telephonyManager).get()

        assertEquals(123, carrier?.id)
        assertEquals("Example", carrier?.name)
        assertEquals("310", carrier?.mobileCountryCode)
        assertEquals("260", carrier?.mobileNetworkCode)
        assertEquals("us", carrier?.isoCountryCode)
    }

    @Test
    @Config(sdk = [27])
    fun fallsBackToNetworkOperatorNameOnOlderApi() {
        `when`(telephonyManager.simOperatorName).thenReturn("")
        `when`(telephonyManager.networkOperatorName).thenReturn("Fallback")
        `when`(telephonyManager.simOperator).thenReturn("23415")

        val carrier = CarrierFinder(context, telephonyManager).get()

        assertEquals(-1, carrier?.id)
        assertEquals("Fallback", carrier?.name)
        assertEquals("234", carrier?.mobileCountryCode)
        assertEquals("15", carrier?.mobileNetworkCode)
    }

    @Test
    fun malformedOperatorDoesNotProducePartialCodes() {
        `when`(telephonyManager.simOperator).thenReturn("1234")

        val carrier = CarrierFinder(context, telephonyManager).get()

        assertNull(carrier?.mobileCountryCode)
        assertNull(carrier?.mobileNetworkCode)
    }

    @Test
    fun deviceWithoutTelephonyReturnsNoCarrier() {
        `when`(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)).thenReturn(false)

        assertNull(CarrierFinder(context, telephonyManager).get())
        assertLogContains("Cannot determine carrier details: telephony feature missing.")
    }

    @Test
    fun missingTelephonyServiceReturnsNoCarrier() {
        assertNull(CarrierFinder(context, null).get())
        assertLogContains("Cannot determine carrier details: telephony service unavailable.")
    }

    @Test
    fun missingPhoneStatePermissionUsesPermissionFreeCarrierFields() {
        `when`(context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(telephonyManager.simOperatorName).thenReturn("Fallback")
        `when`(telephonyManager.simOperator).thenReturn("310260")

        val carrier = CarrierFinder(context, telephonyManager).get()

        assertEquals(-1, carrier?.id)
        assertEquals("Fallback", carrier?.name)
        assertEquals("310", carrier?.mobileCountryCode)
        assertEquals("260", carrier?.mobileNetworkCode)
        verify(telephonyManager, never()).simCarrierId
        assertLogContains("Missing read phone state permission, using legacy carrier methods.")
    }

    @Test
    fun securityFailureReturnsNoCarrier() {
        `when`(telephonyManager.simCarrierIdName).thenThrow(SecurityException("denied"))

        assertNull(CarrierFinder(context, telephonyManager).get())
        assertLogContains("SecurityException when accessing carrier info.")
    }

    @Test
    fun runtimeFailureReturnsNoCarrier() {
        `when`(telephonyManager.simCarrierIdName).thenThrow(IllegalStateException("unavailable"))

        assertNull(CarrierFinder(context, telephonyManager).get())
        assertLogContains("Failed to access carrier info.")
    }

    private fun assertLogContains(message: String) {
        assertTrue(
            "Expected log message: $message",
            ShadowLog.getLogsForTag("CarrierFinder").any { it.msg == message }
        )
    }
}
