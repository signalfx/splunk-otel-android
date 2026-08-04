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

package com.splunk.rum.instrumentation.networkmonitor.internal

import android.app.Application
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateGate
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import com.splunk.rum.instrumentation.networkmonitor.internal.network.CurrentNetworkProvider
import com.splunk.rum.instrumentation.networkmonitor.internal.network.NetworkChangeListener
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.RETURNS_SELF
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class NetworkMonitorInstrumentationTest {
    private val application = mock(Application::class.java)
    private val openTelemetry = mock(OpenTelemetry::class.java)
    private val loggerProvider = mock(LoggerProvider::class.java)
    private val logger = mock(Logger::class.java)
    private val logRecordBuilder = mock(LogRecordBuilder::class.java, RETURNS_SELF)

    init {
        `when`(openTelemetry.logsBridge).thenReturn(loggerProvider)
        `when`(loggerProvider.get("com.splunk.rum.network")).thenReturn(logger)
        `when`(logger.logRecordBuilder()).thenReturn(logRecordBuilder)
    }

    @Test
    fun addingListenerReturnsSameInstrumentationInstance() {
        val instrumentation = NetworkMonitorInstrumentation()

        assertSame(instrumentation, instrumentation.addNetworkChangeListener {})
    }

    @Test
    fun notifiesAttributeListenerAndEmitsNetworkChange() {
        val provider = FakeCurrentNetworkProvider()
        val observedTypes = mutableListOf<String?>()
        val instrumentation = instrumentationWith(provider)
            .addNetworkChangeListener { observedTypes += it[NETWORK_CONNECTION_TYPE] }

        instrumentation.install(application, openTelemetry)
        provider.publish(CurrentNetwork(NetworkState.TRANSPORT_WIFI))

        assertEquals(listOf("wifi"), observedTypes)
        verify(logRecordBuilder).emit()
    }

    @Test
    fun backgroundSuppressesEventButNotAttributeListener() {
        val provider = FakeCurrentNetworkProvider()
        val observedTypes = mutableListOf<String?>()
        val instrumentation = instrumentationWith(provider)
            .addNetworkChangeListener { observedTypes += it[NETWORK_CONNECTION_TYPE] }

        instrumentation.install(application, openTelemetry)
        val gate = AppStateObserver.listeners.filterIsInstance<NetworkApplicationStateGate>().last()
        gate.onAppBackgrounded()
        provider.publish(CurrentNetwork(NetworkState.TRANSPORT_CELLULAR))

        assertEquals(listOf("cell"), observedTypes)
        verify(logRecordBuilder, never()).emit()
    }

    @Test
    fun repeatedInstallIsIdempotent() {
        val provider = FakeCurrentNetworkProvider()
        var providerCreations = 0
        val instrumentation = NetworkMonitorInstrumentation().apply {
            currentNetworkProviderFactory = {
                providerCreations++
                provider
            }
        }

        instrumentation.install(application, openTelemetry)
        instrumentation.install(application, openTelemetry)
        provider.publish(CurrentNetwork(NetworkState.TRANSPORT_WIFI))

        assertEquals(1, providerCreations)
        verify(logRecordBuilder).emit()
    }

    @Test
    fun unavailableConnectivityManagerSkipsInstallationAndLogsWarning() {
        var providerCreations = 0
        val instrumentation = NetworkMonitorInstrumentation().apply {
            currentNetworkProviderFactory = {
                providerCreations++
                null
            }
        }

        instrumentation.install(application, openTelemetry)
        instrumentation.install(application, openTelemetry)

        assertEquals(2, providerCreations)
        assertTrue(
            ShadowLog.getLogsForTag("NetworkMonitor").any {
                it.msg == "ConnectivityManager unavailable. Network monitoring will not be installed."
            }
        )
    }

    @Test
    fun oneFailingAttributeListenerDoesNotBlockOtherListenerOrEvent() {
        val provider = FakeCurrentNetworkProvider()
        val observed = mutableListOf<String?>()
        val instrumentation = instrumentationWith(provider)
            .addNetworkChangeListener { throw IllegalStateException("listener failure") }
            .addNetworkChangeListener { observed += it[NETWORK_CONNECTION_TYPE] }

        instrumentation.install(application, openTelemetry)
        provider.publish(CurrentNetwork(NetworkState.TRANSPORT_VPN))

        assertEquals(listOf("vpn"), observed)
        verify(logRecordBuilder).emit()
        assertTrue(
            ShadowLog.getLogsForTag("NetworkMonitor").any {
                it.msg == "Network change listener failed."
            }
        )
    }

    private fun instrumentationWith(provider: FakeCurrentNetworkProvider) = NetworkMonitorInstrumentation().apply {
        currentNetworkProviderFactory = { provider }
    }

    private class FakeCurrentNetworkProvider : CurrentNetworkProvider {
        private val listeners = mutableListOf<NetworkChangeListener>()
        override var currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK

        override fun refreshNetworkStatus(): CurrentNetwork = currentNetwork

        override fun addNetworkChangeListener(listener: NetworkChangeListener) {
            listeners += listener
        }

        override fun removeNetworkChangeListener(listener: NetworkChangeListener) {
            listeners -= listener
        }

        override fun close() {
            listeners.clear()
        }

        fun publish(network: CurrentNetwork) {
            currentNetwork = network
            listeners.forEach { it.onNetworkChange(network) }
        }
    }
}
