/*
 * Copyright 2026 Splunk Inc.
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CurrentNetworkProviderTest {
    @Test
    fun createsProviderFromApplicationConnectivityService() {
        val context = mock(Context::class.java)
        val applicationContext = mock(Context::class.java)
        val connectivityManager = mock(ConnectivityManager::class.java)
        `when`(context.applicationContext).thenReturn(applicationContext)
        `when`(applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)

        val provider = CurrentNetworkProvider.create(context)

        assertNotNull(provider)
        verify(applicationContext).getSystemService(Context.CONNECTIVITY_SERVICE)
        verify(context, never()).getSystemService(Context.CONNECTIVITY_SERVICE)
        provider?.close()
    }

    @Test
    fun unavailableConnectivityManagerReturnsNoProvider() {
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null)

        val provider = CurrentNetworkProvider.create(context)

        assertNull(provider)
    }
}
