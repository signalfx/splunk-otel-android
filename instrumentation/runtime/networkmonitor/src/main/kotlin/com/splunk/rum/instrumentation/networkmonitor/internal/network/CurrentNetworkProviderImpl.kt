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
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.common.utils.thread.NamedThreadFactory
import com.splunk.rum.instrumentation.networkmonitor.internal.model.CurrentNetwork
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class CurrentNetworkProviderImpl(
    private val networkDetector: NetworkDetector,
    private val connectivityManager: ConnectivityManager,
    private val initialDetectionExecutor: ExecutorService = createInitialDetectionExecutor(),
    private val createNetworkMonitoringRequest: () -> NetworkRequest = ::createNetworkMonitoringRequest,
    private val networkObservationExecutor: ExecutorService = createNetworkObservationExecutor()
) : CurrentNetworkProvider {
    override val currentNetwork: CurrentNetwork
        get() = networkSnapshot.currentNetwork

    private val callbackRef = AtomicReference<NetworkCallback>()
    private val listeners = CopyOnWriteArrayList<NetworkChangeListener>()
    private val initialNetworkStateListenerRef = AtomicReference<NetworkChangeListener>()

    // A callback observed before the initial query publishes invalidates that query. The callback
    // path then owns baseline publication, even if its observation is later dropped by the bound.
    private val initialDetectionInvalidated = AtomicBoolean()
    private val started = AtomicBoolean()
    private val closed = AtomicBoolean()

    // The snapshot coordinates the background initial detection with callbacks that may arrive
    // immediately after registration. All fields must change together to avoid publishing stale state.
    private val networkSnapshotLock = Any()

    // Tracks the network most recently reported as the default network. It is retained after the
    // first capabilities callback so stale onLost callbacks can be ignored.
    private val pendingDefaultNetworkRef = AtomicReference<Network>()

    // API 26+ guarantees the first capabilities callback after onAvailable. The default-network
    // callback represents one active network at a time, so one atomic reference is sufficient to
    // identify the network still waiting for that callback.
    private val pendingCapabilityNetworkRef = AtomicReference<Network>()

    @Volatile
    private var networkSnapshot = NetworkSnapshot(
        isInitialNetworkStateEstablished = false,
        activeNetworkIdentityKnown = false,
        activeNetworkIdentity = null,
        currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK
    )

    // Public lifecycle and listener API.
    override fun start(initialNetworkStateListener: NetworkChangeListener) {
        if (!started.compareAndSet(false, true) || closed.get()) {
            return
        }

        initialNetworkStateListenerRef.set(initialNetworkStateListener)
        try {
            // Register before detecting so a network transition cannot be missed while detection runs.
            registerNetworkCallbacks(createNetworkMonitoringRequest)
        } catch (exception: Exception) {
            Logger.w(
                TAG,
                "Failed to register network callbacks. Network monitoring is disabled.",
                exception
            )
        }
        detectInitialNetwork(initialNetworkStateListener)
    }

    override fun addNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    override fun removeNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
    }

    override fun close() {
        closed.set(true)
        callbackRef.getAndSet(null)?.let(::unregisterNetworkCallback)
        shutdownExecutor(initialDetectionExecutor, "initial detection")
        shutdownExecutor(networkObservationExecutor, "network observation")
        pendingCapabilityNetworkRef.set(null)
        pendingDefaultNetworkRef.set(null)
        initialNetworkStateListenerRef.set(null)
        listeners.clear()
    }

    // One-time initialization establishes the baseline without emitting a network-change event.
    private fun detectInitialNetwork(initialNetworkStateListener: NetworkChangeListener) {
        try {
            if (closed.get() || initialDetectionInvalidated.get()) {
                return
            }
            initialDetectionExecutor.execute { runInitialNetworkDetection(initialNetworkStateListener) }
        } catch (exception: RuntimeException) {
            if (!closed.get()) {
                Logger.w(TAG, "Failed to schedule initial network detection.", exception)
            }
        }
    }

    private fun runInitialNetworkDetection(initialNetworkStateListener: NetworkChangeListener) {
        try {
            if (closed.get() || initialDetectionInvalidated.get()) {
                return
            }

            val observation = detectNetwork()
            if (closed.get() || !publishInitialObservation(observation)) {
                return
            }

            // The first active state establishes attributes only; it is not a transition.
            notifyListener(initialNetworkStateListener, observation.currentNetwork)

            // A callback may have completed while the initial listener ran. Reapply the latest
            // state so attributes cannot finish stale after a transition.
            val latestCurrentNetwork = networkSnapshot.currentNetwork
            if (latestCurrentNetwork != observation.currentNetwork) {
                notifyListener(initialNetworkStateListener, latestCurrentNetwork)
            }
        } catch (exception: RuntimeException) {
            Logger.w(TAG, "Failed to process initial network detection.", exception)
        } finally {
            // This executor is only for one-time initialization; ongoing callbacks use the
            // separate observation executor so initialization cannot delay transitions.
            try {
                initialDetectionExecutor.shutdown()
            } catch (exception: RuntimeException) {
                Logger.w(TAG, "Failed to shut down initial network detection.", exception)
            }
        }
    }

    private fun publishInitialObservation(observation: NetworkObservation): Boolean =
        synchronized(networkSnapshotLock) {
            if (initialDetectionInvalidated.get() || networkSnapshot.isInitialNetworkStateEstablished) {
                return@synchronized false
            }
            networkSnapshot = networkSnapshot.copy(
                isInitialNetworkStateEstablished = true,
                activeNetworkIdentityKnown = observation.activeNetworkIdentityKnown,
                activeNetworkIdentity = observation.activeNetworkIdentity,
                currentNetwork = observation.currentNetwork
            )
            true
        }

    // Framework callback registration and callback-to-queue translation.
    private fun registerNetworkCallbacks(createNetworkMonitoringRequest: () -> NetworkRequest) {
        val callback = ConnectionMonitor()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            connectivityManager.registerNetworkCallback(createNetworkMonitoringRequest(), callback)
        }
        callbackRef.set(callback)
        // close() can race with callback registration; unregister a callback published after close.
        if (closed.get() && callbackRef.compareAndSet(callback, null)) {
            unregisterNetworkCallback(callback)
        }
    }

    private fun unregisterNetworkCallback(callback: NetworkCallback) {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (exception: RuntimeException) {
            Logger.w(TAG, "Failed to unregister network callbacks.", exception)
        }
    }

    /** Converts framework callbacks into background observations without doing synchronous reads. */
    private inner class ConnectionMonitor : NetworkCallback() {
        override fun onAvailable(network: Network) {
            initialDetectionInvalidated.set(true)
            Logger.d(TAG, "onAvailable: network=$network")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                enqueueLegacyNetworkObservation()
                return
            }

            pendingDefaultNetworkRef.set(network)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // registerDefaultNetworkCallback tracks only the current best network.
                pendingCapabilityNetworkRef.set(network)
            } else {
                // API 24-25 do not guarantee a capability callback after onAvailable. Resolve
                // the supplied network off the framework callback thread.
                enqueueAvailableNetworkObservation(network)
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && consumePendingCapabilityNetwork(network)) {
                enqueueCallbackObservation(network, capabilities)
            }
        }

        override fun onLost(network: Network) {
            initialDetectionInvalidated.set(true)
            Logger.d(TAG, "onLost: network=$network")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                enqueueLegacyNetworkObservation()
                return
            }

            clearPendingNetwork(pendingCapabilityNetworkRef, network)
            clearPendingNetwork(pendingDefaultNetworkRef, network)
            enqueueDefaultNetworkLoss(network)
        }
    }

    private fun consumePendingCapabilityNetwork(network: Network): Boolean {
        val pendingNetwork = pendingCapabilityNetworkRef.get()
        return pendingNetwork == network && pendingCapabilityNetworkRef.compareAndSet(pendingNetwork, null)
    }

    private fun clearPendingNetwork(reference: AtomicReference<Network>, network: Network) {
        val pendingNetwork = reference.get()
        if (pendingNetwork == network) {
            reference.compareAndSet(pendingNetwork, null)
        }
    }

    // Background observation queue and processing.
    private fun enqueueLegacyNetworkObservation() {
        enqueueObservation(PendingObservation(network = null))
    }

    private fun enqueueCallbackObservation(network: Network, capabilities: NetworkCapabilities) {
        enqueueObservation(PendingObservation(network = network, capabilities = capabilities))
    }

    private fun enqueueAvailableNetworkObservation(network: Network) {
        enqueueObservation(PendingObservation(network = network))
    }

    private fun enqueueDefaultNetworkLoss(network: Network) {
        enqueueObservation(PendingObservation(network = network, isNetworkLost = true))
    }

    private fun enqueueObservation(observation: PendingObservation) {
        try {
            if (closed.get()) {
                return
            }
            networkObservationExecutor.execute {
                try {
                    if (!closed.get()) {
                        processPendingObservation(observation)
                    }
                } catch (exception: RuntimeException) {
                    Logger.w(TAG, "Failed to process network observation.", exception)
                }
            }
        } catch (exception: RuntimeException) {
            if (!closed.get()) {
                Logger.w(TAG, "Dropped network observation because it could not be queued.", exception)
            }
        }
    }

    private fun processPendingObservation(pendingObservation: PendingObservation) {
        if (pendingObservation.isNetworkLost) {
            pendingObservation.network?.let(::processDefaultNetworkLoss)
            return
        }

        val observation = when {
            pendingObservation.network == null -> detectNetwork()
            pendingObservation.capabilities != null -> observeCallbackNetwork(
                pendingObservation.network,
                pendingObservation.capabilities
            )
            else -> detectNetwork(pendingObservation.network)
        }
        // close() can race with detector work. Do not publish or notify from an observation that
        // completed after shutdown.
        if (closed.get()) {
            return
        }

        val publication = publishNetworkObservation(observation)
        notifyListeners(observation.currentNetwork, publication)
    }

    private fun processDefaultNetworkLoss(network: Network) {
        val pendingDefaultNetwork = pendingDefaultNetworkRef.get()
        if (closed.get() || (pendingDefaultNetwork != null && pendingDefaultNetwork != network)) {
            return
        }

        val currentSnapshot = networkSnapshot
        if (currentSnapshot.isInitialNetworkStateEstablished &&
            currentSnapshot.activeNetworkIdentityKnown &&
            currentSnapshot.activeNetworkIdentity != network
        ) {
            // A replacement default network has already been published.
            return
        }

        val observation = NetworkObservation(
            currentNetwork = CurrentNetworkProvider.NO_NETWORK,
            activeNetworkIdentity = null,
            activeNetworkIdentityKnown = true
        )
        if (closed.get()) {
            return
        }

        val publication = publishNetworkObservation(observation)
        notifyListeners(observation.currentNetwork, publication)
    }

    // Detection is kept off the Android callback thread.
    private fun detectNetwork(): NetworkObservation = try {
        networkDetector.detectNetwork()
    } catch (exception: Exception) {
        Logger.w(TAG, "Failed to detect the current network.", exception)
        NetworkObservation(
            currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK,
            activeNetworkIdentity = null,
            activeNetworkIdentityKnown = false
        )
    }

    private fun detectNetwork(network: Network): NetworkObservation = try {
        networkDetector.detectNetwork(network)
    } catch (exception: Exception) {
        Logger.w(TAG, "Failed to detect the available network.", exception)
        NetworkObservation(
            currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK,
            activeNetworkIdentity = network,
            activeNetworkIdentityKnown = true
        )
    }

    private fun observeCallbackNetwork(network: Network, capabilities: NetworkCapabilities): NetworkObservation = try {
        networkDetector.observeNetwork(network, capabilities).let { observation ->
            // Keep the identity and attributes paired even if a detector implementation returns
            // an observation built from a different source.
            observation.copy(activeNetworkIdentity = network, activeNetworkIdentityKnown = true)
        }
    } catch (exception: Exception) {
        Logger.w(TAG, "Failed to read callback network capabilities.", exception)
        NetworkObservation(
            currentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK,
            activeNetworkIdentity = network,
            activeNetworkIdentityKnown = true
        )
    }

    // Snapshot publication determines event emission versus attribute-only updates.
    private data class Publication(val emitEventAndNotifyAttributes: Boolean, val notifyAttributes: Boolean)

    private fun publishNetworkObservation(observation: NetworkObservation): Publication =
        synchronized(networkSnapshotLock) {
            val previousSnapshot = networkSnapshot
            val activeNetworkChanged = if (
                observation.activeNetworkIdentityKnown &&
                previousSnapshot.activeNetworkIdentityKnown &&
                (observation.activeNetworkIdentity != null || previousSnapshot.activeNetworkIdentity != null)
            ) {
                previousSnapshot.activeNetworkIdentity != observation.activeNetworkIdentity
            } else {
                previousSnapshot.currentNetwork != observation.currentNetwork
            }
            val currentNetworkChanged = previousSnapshot.currentNetwork != observation.currentNetwork
            val shouldEmitEvent = previousSnapshot.isInitialNetworkStateEstablished && activeNetworkChanged
            networkSnapshot = previousSnapshot.copy(
                isInitialNetworkStateEstablished = true,
                activeNetworkIdentityKnown = observation.activeNetworkIdentityKnown,
                activeNetworkIdentity = observation.activeNetworkIdentity,
                currentNetwork = observation.currentNetwork
            )
            Publication(
                emitEventAndNotifyAttributes = shouldEmitEvent,
                // The initial state must be applied once. After that, notify only when either
                // the active identity or the attributes changed.
                notifyAttributes = !previousSnapshot.isInitialNetworkStateEstablished ||
                    activeNetworkChanged ||
                    currentNetworkChanged
            )
        }

    private fun notifyListeners(currentNetwork: CurrentNetwork, publication: Publication) {
        when {
            publication.emitEventAndNotifyAttributes ->
                listeners.forEach { listener -> notifyListener(listener, currentNetwork) }
            publication.notifyAttributes ->
                initialNetworkStateListenerRef.get()?.let { listener ->
                    notifyListener(listener, currentNetwork)
                }
        }
    }

    private fun notifyListener(listener: NetworkChangeListener, currentNetwork: CurrentNetwork) {
        try {
            listener.onNetworkChange(currentNetwork)
        } catch (exception: RuntimeException) {
            Logger.w(TAG, "Network change listener failed.", exception)
        }
    }

    private fun shutdownExecutor(executor: ExecutorService, name: String) {
        try {
            executor.shutdownNow()
        } catch (exception: RuntimeException) {
            Logger.w(TAG, "Failed to shut down $name executor.", exception)
        }
    }

    private companion object {
        private const val TAG = "CurrentNetworkProvider"

        fun createInitialDetectionExecutor(): ExecutorService =
            Executors.newSingleThreadExecutor(NamedThreadFactory("SplunkInitialNetworkMonitor"))

        fun createNetworkObservationExecutor(): ExecutorService = ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(MAX_PENDING_NETWORK_OBSERVATIONS),
            NamedThreadFactory("SplunkNetworkMonitor"),
            ThreadPoolExecutor.AbortPolicy()
        )

        fun createNetworkMonitoringRequest(): NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()

        /** Queue a short burst of network observations to protect host app from being
         * negatively impacted by repeated transitions. */
        private const val MAX_PENDING_NETWORK_OBSERVATIONS = 5
    }

    private data class NetworkSnapshot(
        val isInitialNetworkStateEstablished: Boolean,
        val activeNetworkIdentityKnown: Boolean,
        val activeNetworkIdentity: Network?,
        val currentNetwork: CurrentNetwork
    )

    private data class PendingObservation(
        val network: Network?,
        val capabilities: NetworkCapabilities? = null,
        val isNetworkLost: Boolean = false
    )
}
