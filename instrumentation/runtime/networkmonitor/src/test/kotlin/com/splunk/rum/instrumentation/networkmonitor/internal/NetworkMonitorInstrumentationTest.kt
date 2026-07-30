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
import com.splunk.rum.instrumentation.networkmonitor.internal.lifecycle.NetworkApplicationStateGate
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import com.splunk.rum.instrumentation.networkmonitor.internal.network.CurrentNetworkProvider
import com.splunk.rum.instrumentation.networkmonitor.internal.network.NetworkChangeListener
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE
import java.io.Closeable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers.RETURNS_SELF
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
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
    private val logRecordBuilder = mock(ExtendedLogRecordBuilder::class.java, RETURNS_SELF)

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
        lateinit var gate: NetworkApplicationStateGate
        val observedTypes = mutableListOf<String?>()
        val instrumentation = instrumentationWith(provider).apply {
            applicationStateObserverFactory = { _, stateGate ->
                gate = stateGate
                Closeable {}
            }
            addNetworkChangeListener { observedTypes += it[NETWORK_CONNECTION_TYPE] }
        }

        instrumentation.install(application, openTelemetry)
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
            applicationStateObserverFactory = { _, _ -> Closeable {} }
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
        var observerCreations = 0
        val instrumentation = NetworkMonitorInstrumentation().apply {
            currentNetworkProviderFactory = {
                providerCreations++
                null
            }
            applicationStateObserverFactory = { _, _ ->
                observerCreations++
                Closeable {}
            }
        }

        instrumentation.install(application, openTelemetry)
        instrumentation.install(application, openTelemetry)

        assertEquals(2, providerCreations)
        assertEquals(0, observerCreations)
        assertTrue(
            ShadowLog.getLogsForTag("NetworkMonitor").any {
                it.msg == "ConnectivityManager unavailable. Network monitoring will not be installed."
            }
        )
    }

    @Test
    fun closeStopsProviderAndLifecycleObserver() {
        val provider = FakeCurrentNetworkProvider()
        var observerClosed = false
        val instrumentation = instrumentationWith(provider).apply {
            applicationStateObserverFactory = { _, _ ->
                Closeable { observerClosed = true }
            }
        }

        instrumentation.install(application, openTelemetry)
        instrumentation.close()

        assertEquals(1, provider.closeCount)
        assertTrue(observerClosed)
    }

    @Test
    fun closeBeforeInstallDoesNothing() {
        var providerCreations = 0
        val instrumentation = NetworkMonitorInstrumentation().apply {
            currentNetworkProviderFactory = {
                providerCreations++
                FakeCurrentNetworkProvider()
            }
        }

        instrumentation.close()

        assertEquals(0, providerCreations)
    }

    @Test
    fun repeatedCloseOnlyStopsResourcesOnce() {
        val provider = FakeCurrentNetworkProvider()
        var observerCloseCount = 0
        val instrumentation = instrumentationWith(provider).apply {
            applicationStateObserverFactory = { _, _ ->
                Closeable { observerCloseCount++ }
            }
        }

        instrumentation.install(application, openTelemetry)
        instrumentation.close()
        instrumentation.close()

        assertEquals(1, provider.closeCount)
        assertEquals(1, observerCloseCount)
    }

    @Test
    fun canInstallAgainAfterClose() {
        val provider = FakeCurrentNetworkProvider()
        val instrumentation = instrumentationWith(provider)

        instrumentation.install(application, openTelemetry)
        provider.publish(CurrentNetwork(NetworkState.TRANSPORT_WIFI))
        instrumentation.close()
        instrumentation.install(application, openTelemetry)
        provider.publish(CurrentNetwork(NetworkState.TRANSPORT_VPN))

        assertEquals(1, provider.closeCount)
        verify(logRecordBuilder, times(2)).emit()
    }

    @Test
    fun lifecycleObserverFailureLeavesNetworkEventsEnabled() {
        val provider = FakeCurrentNetworkProvider()
        val instrumentation = instrumentationWith(provider).apply {
            applicationStateObserverFactory = { _, _ ->
                throw IllegalStateException("observer unavailable")
            }
        }

        instrumentation.install(application, openTelemetry)
        provider.publish(CurrentNetwork(NetworkState.TRANSPORT_WIFI))

        verify(logRecordBuilder).emit()
        assertTrue(
            ShadowLog.getLogsForTag("NetworkMonitor").any {
                it.msg ==
                    "Failed to observe application foreground/background state. Network change events will remain enabled."
            }
        )
    }

    @Test
    fun providerCloseFailureDoesNotPreventLifecycleObserverClose() {
        val provider = FakeCurrentNetworkProvider(closeFailure = IllegalStateException("close failed"))
        var observerClosed = false
        val instrumentation = instrumentationWith(provider).apply {
            applicationStateObserverFactory = { _, _ ->
                Closeable { observerClosed = true }
            }
        }

        instrumentation.install(application, openTelemetry)
        instrumentation.close()

        assertTrue(observerClosed)
        assertTrue(
            ShadowLog.getLogsForTag("NetworkMonitor").any {
                it.msg == "Failed to stop network monitoring."
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
        applicationStateObserverFactory = { _, _ -> Closeable {} }
    }

    private class FakeCurrentNetworkProvider(private val closeFailure: RuntimeException? = null) :
        CurrentNetworkProvider {
        private val listeners = mutableListOf<NetworkChangeListener>()
        override var currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK
        var closeCount = 0

        override fun refreshNetworkStatus(): CurrentNetwork = currentNetwork

        override fun addNetworkChangeListener(listener: NetworkChangeListener) {
            listeners += listener
        }

        override fun removeNetworkChangeListener(listener: NetworkChangeListener) {
            listeners -= listener
        }

        override fun close() {
            closeCount++
            listeners.clear()
            closeFailure?.let { throw it }
        }

        fun publish(network: CurrentNetwork) {
            currentNetwork = network
            listeners.forEach { it.onNetworkChange(network) }
        }
    }
}
