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
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import com.splunk.rum.instrumentation.networkmonitor.internal.model.NetworkState
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class CurrentNetworkProviderImplTest {
    private val detector = mock(NetworkDetector::class.java)
    private val connectivityManager = mock(ConnectivityManager::class.java)
    private val request = mock(NetworkRequest::class.java)
    private val initialDetectionExecutor = QueuingExecutorService()
    private val networkObservationExecutor = ImmediateExecutorService()

    init {
        Logger.clearLogs()
    }

    @Test
    fun detectsInitialNetworkOffTheCallingThreadAndRegistersCallback() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val observedInitialNetworks = mutableListOf<CurrentNetwork>()
        `when`(detector.detectNetwork()).thenReturn(observation(wifi))

        val provider = createProvider { observedInitialNetworks += it }

        assertEquals(CurrentNetworkProvider.UNKNOWN_NETWORK, provider.currentNetwork)
        verify(detector, never()).detectNetwork()
        verify(connectivityManager).registerNetworkCallback(
            any(NetworkRequest::class.java),
            any(ConnectivityManager.NetworkCallback::class.java)
        )

        initialDetectionExecutor.runAll()

        assertEquals(wifi, provider.currentNetwork)
        assertEquals(listOf(wifi), observedInitialNetworks)
        verify(detector).detectNetwork()
    }

    @Test
    @Config(sdk = [24])
    fun usesDefaultNetworkCallbackOnApi24AndNewer() {
        `when`(detector.detectNetwork()).thenReturn(observation(CurrentNetworkProvider.UNKNOWN_NETWORK))
        var requestCreations = 0

        val provider = CurrentNetworkProviderImpl(
            detector,
            connectivityManager,
            initialDetectionExecutor,
            {
                requestCreations++
                request
            },
            networkObservationExecutor
        )
        provider.start {}

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
    fun repeatedInitialCallbacksForTheSameActiveNetworkDoNotEmitEvents() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val activeNetwork = mock(Network::class.java)
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi, activeNetwork, activeNetworkIdentityKnown = true))
        `when`(connectivityManager.activeNetwork).thenReturn(activeNetwork)
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }
        val callback = registeredCallback()

        callback.onAvailable(mock(Network::class.java))
        callback.onAvailable(mock(Network::class.java))

        assertEquals(emptyList<CurrentNetwork>(), observed)
    }

    @Test
    @Config(sdk = [22])
    fun legacyCallbacksUseNetworkDetailsToDetectARealTransition() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, subType = "LTE")
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi), observation(wifi), observation(cellular))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }
        val callback = registeredCallback()

        callback.onAvailable(mock(Network::class.java))
        callback.onAvailable(mock(Network::class.java))

        assertEquals(listOf(cellular), observed)
    }

    @Test
    @Config(sdk = [24])
    fun laterDefaultNetworkCallbackEmitsOnlyForAnActiveNetworkTransition() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, subType = "LTE")
        val initialNetwork = mock(Network::class.java)
        val laterNetwork = mock(Network::class.java)
        val initialCapabilities = mock(NetworkCapabilities::class.java)
        val laterCapabilities = mock(NetworkCapabilities::class.java)
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.detectNetwork(initialNetwork))
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(initialNetwork, initialCapabilities))
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.detectNetwork(laterNetwork))
            .thenReturn(observation(cellular, laterNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(laterNetwork, laterCapabilities))
            .thenReturn(observation(cellular, laterNetwork, activeNetworkIdentityKnown = true))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }
        val callback = registeredDefaultCallback()

        callback.onAvailable(initialNetwork)
        callback.onCapabilitiesChanged(initialNetwork, initialCapabilities)
        callback.onAvailable(laterNetwork)
        callback.onCapabilitiesChanged(laterNetwork, laterCapabilities)

        assertEquals(listOf(cellular), observed)
        verify(detector, times(1)).detectNetwork()
        verify(detector, never()).observeNetwork(laterNetwork, laterCapabilities)
    }

    @Test
    @Config(sdk = [26])
    fun repeatedCapabilityCallbacksForOneNetworkProcessOnlyTheFirstObservation() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val activeNetwork = mock(Network::class.java)
        val firstCapabilities = mock(NetworkCapabilities::class.java)
        val latestCapabilities = mock(NetworkCapabilities::class.java)
        val observationExecutor = QueuingExecutorService()
        val observedInitialNetworks = mutableListOf<CurrentNetwork>()
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi, activeNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(activeNetwork, firstCapabilities))
            .thenReturn(observation(wifi, activeNetwork, activeNetworkIdentityKnown = true))
        val provider = CurrentNetworkProviderImpl(
            detector,
            connectivityManager,
            initialDetectionExecutor,
            { request },
            observationExecutor
        )
        provider.start { observedInitialNetworks += it }
        initialDetectionExecutor.runAll()
        val callback = registeredDefaultCallback()

        callback.onAvailable(activeNetwork)
        callback.onCapabilitiesChanged(activeNetwork, firstCapabilities)
        callback.onCapabilitiesChanged(activeNetwork, latestCapabilities)
        observationExecutor.runAll()

        verify(detector).observeNetwork(activeNetwork, firstCapabilities)
        verify(detector, never()).observeNetwork(activeNetwork, latestCapabilities)
        assertEquals(listOf(wifi), observedInitialNetworks)
    }

    @Test
    @Config(sdk = [26])
    fun capabilityDataChangeForTheSameNetworkUpdatesAttributesWithoutEmittingEvent() {
        val initialCellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, subType = "LTE")
        val updatedCellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, subType = "NR")
        val activeNetwork = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)
        val observedInitialNetworks = mutableListOf<CurrentNetwork>()
        val observedChanges = mutableListOf<CurrentNetwork>()
        `when`(detector.detectNetwork())
            .thenReturn(observation(initialCellular, activeNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(activeNetwork, capabilities))
            .thenReturn(observation(updatedCellular, activeNetwork, activeNetworkIdentityKnown = true))
        val provider = createProvider { observedInitialNetworks += it }
        provider.addNetworkChangeListener { observedChanges += it }
        initialDetectionExecutor.runAll()

        val callback = registeredDefaultCallback()
        callback.onAvailable(activeNetwork)
        callback.onCapabilitiesChanged(activeNetwork, capabilities)

        assertEquals(listOf(initialCellular, updatedCellular), observedInitialNetworks)
        assertEquals(emptyList<CurrentNetwork>(), observedChanges)
    }

    @Test
    @Config(sdk = [26])
    fun pendingObservationsForDifferentNetworksRemainOrdered() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR)
        val initialNetwork = mock(Network::class.java)
        val replacementNetwork = mock(Network::class.java)
        val initialCapabilities = mock(NetworkCapabilities::class.java)
        val replacementCapabilities = mock(NetworkCapabilities::class.java)
        val observationExecutor = QueuingExecutorService()
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(initialNetwork, initialCapabilities))
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(replacementNetwork, replacementCapabilities))
            .thenReturn(observation(cellular, replacementNetwork, activeNetworkIdentityKnown = true))
        val provider = CurrentNetworkProviderImpl(
            detector,
            connectivityManager,
            initialDetectionExecutor,
            { request },
            observationExecutor
        )
        provider.start {}
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }
        val callback = registeredDefaultCallback()

        callback.onAvailable(initialNetwork)
        callback.onCapabilitiesChanged(initialNetwork, initialCapabilities)
        callback.onAvailable(replacementNetwork)
        callback.onCapabilitiesChanged(replacementNetwork, replacementCapabilities)
        observationExecutor.runAll()

        assertEquals(listOf(cellular), observed)
        verify(detector).observeNetwork(initialNetwork, initialCapabilities)
        verify(detector).observeNetwork(replacementNetwork, replacementCapabilities)
    }

    @Test
    @Config(sdk = [26])
    fun pendingNetworkLossIsNotCoalescedWithItsNetworkObservation() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR)
        val initialNetwork = mock(Network::class.java)
        val replacementNetwork = mock(Network::class.java)
        val replacementCapabilities = mock(NetworkCapabilities::class.java)
        val observationExecutor = QueuingExecutorService()
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(replacementNetwork, replacementCapabilities))
            .thenReturn(observation(cellular, replacementNetwork, activeNetworkIdentityKnown = true))
        val provider = CurrentNetworkProviderImpl(
            detector,
            connectivityManager,
            initialDetectionExecutor,
            { request },
            observationExecutor
        )
        provider.start {}
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }
        val callback = registeredDefaultCallback()

        callback.onAvailable(replacementNetwork)
        callback.onCapabilitiesChanged(replacementNetwork, replacementCapabilities)
        callback.onLost(replacementNetwork)
        observationExecutor.runAll()

        assertEquals(listOf(cellular, CurrentNetworkProvider.NO_NETWORK), observed)
    }

    @Test
    @Config(sdk = [26])
    fun api26LosingThePreviousDefaultDoesNotPublishNoNetworkBeforeReplacementCapabilities() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR)
        val initialNetwork = mock(Network::class.java)
        val replacementNetwork = mock(Network::class.java)
        val initialCapabilities = mock(NetworkCapabilities::class.java)
        val replacementCapabilities = mock(NetworkCapabilities::class.java)
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.detectNetwork(initialNetwork))
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(initialNetwork, initialCapabilities))
            .thenReturn(observation(wifi, initialNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.detectNetwork(replacementNetwork))
            .thenReturn(observation(cellular, replacementNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(replacementNetwork, replacementCapabilities))
            .thenReturn(observation(cellular, replacementNetwork, activeNetworkIdentityKnown = true))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }
        val callback = registeredDefaultCallback()

        callback.onAvailable(initialNetwork)
        callback.onCapabilitiesChanged(initialNetwork, initialCapabilities)
        callback.onAvailable(replacementNetwork)
        callback.onLost(initialNetwork)

        assertEquals(emptyList<CurrentNetwork>(), observed)

        callback.onCapabilitiesChanged(replacementNetwork, replacementCapabilities)

        assertEquals(listOf(cellular), observed)
        assertEquals(cellular, provider.currentNetwork)
    }

    @Test
    fun refreshesAndNotifiesListenerWhenNetworkBecomesAvailable() {
        val unknown = CurrentNetworkProvider.UNKNOWN_NETWORK
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, subType = "LTE")
        `when`(detector.detectNetwork()).thenReturn(observation(unknown), observation(cellular))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }

        registeredCallback().onAvailable(mock(Network::class.java))

        assertEquals(cellular, provider.currentNetwork)
        assertEquals(listOf(cellular), observed)
    }

    @Test
    fun publishesNoNetworkWhenActiveNetworkIsLost() {
        `when`(detector.detectNetwork())
            .thenReturn(
                observation(CurrentNetwork(NetworkState.TRANSPORT_WIFI)),
                observation(CurrentNetworkProvider.NO_NETWORK)
            )
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observed += it }

        registeredCallback().onLost(mock(Network::class.java))

        assertEquals(CurrentNetworkProvider.NO_NETWORK, provider.currentNetwork)
        assertEquals(listOf(CurrentNetworkProvider.NO_NETWORK), observed)
    }

    @Test
    fun detectorFailureFallsBackToUnknownNetwork() {
        `when`(detector.detectNetwork()).thenThrow(IllegalStateException("unavailable"))

        val provider = createProvider()
        initialDetectionExecutor.runAll()

        assertEquals(CurrentNetworkProvider.UNKNOWN_NETWORK, provider.currentNetwork)
    }

    @Test
    fun removedListenerIsNotNotified() {
        `when`(detector.detectNetwork())
            .thenReturn(observation(CurrentNetwork(NetworkState.TRANSPORT_WIFI)))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        val listener = NetworkChangeListener { observed += it }
        provider.addNetworkChangeListener(listener)
        provider.removeNetworkChangeListener(listener)

        registeredCallback().onLost(mock(Network::class.java))

        assertEquals(emptyList<CurrentNetwork>(), observed)
    }

    @Test
    fun closeUnregistersCallbackAndClearsListeners() {
        `when`(detector.detectNetwork())
            .thenReturn(observation(CurrentNetwork(NetworkState.TRANSPORT_WIFI)))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
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
        `when`(detector.detectNetwork()).thenReturn(observation(CurrentNetworkProvider.UNKNOWN_NETWORK))
        val provider = createProvider()
        val callback = registeredCallback()

        provider.close()
        provider.close()

        verify(connectivityManager, times(1)).unregisterNetworkCallback(callback)
    }

    @Test
    fun closeDuringRegistrationUnregistersCallbackAndDoesNotScheduleInitialDetection() {
        val provider = CurrentNetworkProviderImpl(
            detector,
            connectivityManager,
            initialDetectionExecutor,
            { request },
            networkObservationExecutor
        )
        doAnswer {
            provider.close()
            null
        }.`when`(connectivityManager).registerNetworkCallback(
            any(NetworkRequest::class.java),
            any(ConnectivityManager.NetworkCallback::class.java)
        )

        provider.start {}

        verify(connectivityManager).unregisterNetworkCallback(
            any(ConnectivityManager.NetworkCallback::class.java)
        )
        verify(detector, never()).detectNetwork()
    }

    @Test
    @Config(sdk = [26])
    fun observationCompletedAfterCloseIsNotPublishedOrNotified() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR)
        val activeNetwork = mock(Network::class.java)
        val capabilities = mock(NetworkCapabilities::class.java)
        lateinit var provider: CurrentNetworkProviderImpl
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi, activeNetwork, activeNetworkIdentityKnown = true))
        `when`(detector.observeNetwork(activeNetwork, capabilities)).thenAnswer {
            provider.close()
            observation(cellular, activeNetwork, activeNetworkIdentityKnown = true)
        }
        provider = createProvider()
        initialDetectionExecutor.runAll()
        val observedChanges = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { observedChanges += it }

        val callback = registeredDefaultCallback()
        callback.onAvailable(activeNetwork)
        callback.onCapabilitiesChanged(activeNetwork, capabilities)

        assertEquals(wifi, provider.currentNetwork)
        assertEquals(emptyList<CurrentNetwork>(), observedChanges)
    }

    @Test
    fun unregisterFailureIsLoggedAndDoesNotEscapeClose() {
        `when`(detector.detectNetwork()).thenReturn(observation(CurrentNetworkProvider.UNKNOWN_NETWORK))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val callback = registeredCallback()
        doThrow(IllegalArgumentException("not registered"))
            .`when`(connectivityManager)
            .unregisterNetworkCallback(callback)

        provider.close()

        assertTrue(
            Logger.logs.any {
                it.tag == "CurrentNetworkProvider" && it.message == "Failed to unregister network callbacks."
            }
        )
    }

    @Test
    fun failingNetworkListenerDoesNotBlockOtherListeners() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR)
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi), observation(cellular))
        val provider = createProvider()
        initialDetectionExecutor.runAll()
        val observed = mutableListOf<CurrentNetwork>()
        provider.addNetworkChangeListener { throw IllegalStateException("listener failure") }
        provider.addNetworkChangeListener { observed += it }

        registeredCallback().onAvailable(mock(Network::class.java))

        assertEquals(listOf(cellular), observed)
        assertTrue(
            Logger.logs.any {
                it.tag == "CurrentNetworkProvider" && it.message == "Network change listener failed."
            }
        )
    }

    @Test
    fun failedRegistrationDoesNotPreventCurrentNetworkAccess() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        `when`(detector.detectNetwork()).thenReturn(observation(wifi))
        doThrow(SecurityException("denied"))
            .`when`(connectivityManager)
            .registerNetworkCallback(
                any(NetworkRequest::class.java),
                any(ConnectivityManager.NetworkCallback::class.java)
            )

        val provider = createProvider()

        initialDetectionExecutor.runAll()

        assertEquals(wifi, provider.currentNetwork)
        verify(
            connectivityManager,
            never()
        ).unregisterNetworkCallback(any(ConnectivityManager.NetworkCallback::class.java))
    }

    @Test
    fun rejectedObservationDoesNotEscapeAndLaterObservationCanBeProcessed() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        `when`(detector.detectNetwork())
            .thenReturn(observation(CurrentNetworkProvider.UNKNOWN_NETWORK), observation(wifi))
        val observed = mutableListOf<CurrentNetwork>()
        val provider = CurrentNetworkProviderImpl(
            detector,
            connectivityManager,
            initialDetectionExecutor,
            { request },
            ImmediateExecutorService(rejectNext = true)
        )
        provider.start {}
        initialDetectionExecutor.runAll()
        provider.addNetworkChangeListener { observed += it }

        val callback = registeredCallback()
        callback.onAvailable(mock(Network::class.java))
        callback.onAvailable(mock(Network::class.java))

        assertEquals(listOf(wifi), observed)
        assertTrue(
            Logger.logs.any {
                it.tag == "CurrentNetworkProvider" &&
                    it.message == "Dropped network observation because it could not be queued."
            }
        )
        provider.close()
    }

    @Test
    fun callbackBeforeInitialDetectionPreventsTheInitialNetworkTrip() {
        val cellular = CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, subType = "LTE")
        val observedInitialNetworks = mutableListOf<CurrentNetwork>()
        val observedChanges = mutableListOf<CurrentNetwork>()
        `when`(detector.detectNetwork())
            .thenReturn(observation(cellular), observation(CurrentNetworkProvider.NO_NETWORK))
        val provider = createProvider { observedInitialNetworks += it }
        provider.addNetworkChangeListener { observedChanges += it }
        val callback = registeredCallback()

        callback.onAvailable(mock(Network::class.java))
        initialDetectionExecutor.runAll()

        assertEquals(cellular, provider.currentNetwork)
        assertEquals(listOf(cellular), observedInitialNetworks)
        assertEquals(emptyList<CurrentNetwork>(), observedChanges)
        verify(detector, times(1)).detectNetwork()

        callback.onLost(mock(Network::class.java))

        assertEquals(listOf(CurrentNetworkProvider.NO_NETWORK), observedChanges)
        verify(detector, times(2)).detectNetwork()
    }

    @Test
    fun callbackDuringInitialDetectionPreventsItsResultFromReplacingCallbackState() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        var detectionCount = 0
        val observedInitialNetworks = mutableListOf<CurrentNetwork>()
        val observedChanges = mutableListOf<CurrentNetwork>()
        val provider = createProvider { observedInitialNetworks += it }
        provider.addNetworkChangeListener { observedChanges += it }
        val callback = registeredCallback()
        `when`(detector.detectNetwork()).thenAnswer {
            detectionCount++
            if (detectionCount == 1) {
                callback.onLost(mock(Network::class.java))
                observation(wifi)
            } else {
                observation(CurrentNetworkProvider.NO_NETWORK)
            }
        }

        initialDetectionExecutor.runAll()

        assertEquals(CurrentNetworkProvider.NO_NETWORK, provider.currentNetwork)
        assertEquals(listOf(CurrentNetworkProvider.NO_NETWORK), observedInitialNetworks)
        assertEquals(emptyList<CurrentNetwork>(), observedChanges)
    }

    @Test
    fun callbackAfterInitialCommitReconcilesInitialAttributesToTheLatestState() {
        val wifi = CurrentNetwork(NetworkState.TRANSPORT_WIFI)
        `when`(detector.detectNetwork())
            .thenReturn(observation(wifi), observation(CurrentNetworkProvider.NO_NETWORK))
        val provider = CurrentNetworkProviderImpl(
            detector,
            connectivityManager,
            initialDetectionExecutor,
            { request },
            networkObservationExecutor
        )
        val observedInitialNetworks = mutableListOf<CurrentNetwork>()
        lateinit var callback: ConnectivityManager.NetworkCallback
        provider.start { network ->
            observedInitialNetworks += network
            if (observedInitialNetworks.size == 1) {
                callback.onLost(mock(Network::class.java))
            }
        }
        callback = registeredCallback()

        initialDetectionExecutor.runAll()

        assertEquals(
            listOf(wifi, CurrentNetworkProvider.NO_NETWORK),
            observedInitialNetworks
        )
        assertEquals(CurrentNetworkProvider.NO_NETWORK, provider.currentNetwork)
    }

    private fun createProvider(
        initialNetworkStateListener: NetworkChangeListener = NetworkChangeListener {}
    ): CurrentNetworkProviderImpl = CurrentNetworkProviderImpl(
        detector,
        connectivityManager,
        initialDetectionExecutor,
        { request },
        networkObservationExecutor
    ).also { provider -> provider.start(initialNetworkStateListener) }

    private fun observation(
        currentNetwork: CurrentNetwork,
        activeNetworkIdentity: Network? = null,
        activeNetworkIdentityKnown: Boolean = false
    ) = NetworkObservation(currentNetwork, activeNetworkIdentity, activeNetworkIdentityKnown)

    private fun registeredCallback(): ConnectivityManager.NetworkCallback {
        val captor = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback::class.java)
        verify(connectivityManager).registerNetworkCallback(any(NetworkRequest::class.java), captor.capture())
        return captor.value
    }

    private fun registeredDefaultCallback(): ConnectivityManager.NetworkCallback {
        val captor = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback::class.java)
        verify(connectivityManager).registerDefaultNetworkCallback(captor.capture())
        return captor.value
    }

    private class QueuingExecutorService : AbstractExecutorService() {
        private val tasks = ArrayDeque<Runnable>()
        private var shutdown = false

        override fun execute(command: Runnable) {
            if (shutdown) {
                throw RejectedExecutionException()
            }
            tasks.addLast(command)
        }

        override fun shutdown() {
            shutdown = true
        }

        override fun shutdownNow(): List<Runnable> {
            shutdown = true
            return buildList {
                while (tasks.isNotEmpty()) {
                    add(tasks.removeFirst())
                }
            }
        }

        override fun isShutdown(): Boolean = shutdown

        override fun isTerminated(): Boolean = shutdown && tasks.isEmpty()

        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = isTerminated

        fun runAll() {
            while (tasks.isNotEmpty()) {
                tasks.removeFirst().run()
            }
        }
    }

    private class ImmediateExecutorService(private var rejectNext: Boolean = false) : AbstractExecutorService() {
        private var shutdown = false

        override fun execute(command: Runnable) {
            if (shutdown) {
                throw RejectedExecutionException()
            }
            if (rejectNext) {
                rejectNext = false
                throw RejectedExecutionException("queue full")
            }
            command.run()
        }

        override fun shutdown() {
            shutdown = true
        }

        override fun shutdownNow(): List<Runnable> {
            shutdown = true
            return emptyList()
        }

        override fun isShutdown(): Boolean = shutdown

        override fun isTerminated(): Boolean = shutdown

        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = shutdown
    }
}
