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

package com.splunk.rum.integration.navigation.automatic

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.navigation.NavigationEvent
import com.splunk.rum.integration.navigation.NavigationEventProcessor
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.lang.ref.WeakReference

/**
 * Manages NavController registration and processes Compose navigation destination changes.
 *
 * Filters out NavGraph containers and dialog destinations, extracts route templates as screen names,
 * and separates resolved arguments into event attributes. Events are routed through
 * [ScreenChangeDetector] so the Compose route takes priority over Activity/Fragment names.
 */
internal class ComposeNavigationTracker(
    private val screenChangeDetector: ScreenChangeDetector,
    private val processor: NavigationEventProcessor?
) {

    private var registeredController: WeakReference<NavController>? = null
    private var activeListener: NavController.OnDestinationChangedListener? = null

    fun register(navController: NavController) {
        if (registeredController?.get() === navController) {
            navController.currentDestination?.let { destination ->
                handleDestinationChanged(destination, navController.currentBackStackEntry?.arguments)
            }
            return
        }

        unregisterCurrent()

        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            handleDestinationChanged(destination, arguments)
        }
        navController.addOnDestinationChangedListener(listener)
        registeredController = WeakReference(navController)
        activeListener = listener
        Logger.d(TAG, "Registered NavController for Compose navigation tracking")
    }

    fun unregister(navController: NavController) {
        if (registeredController?.get() === navController) {
            activeListener?.let { navController.removeOnDestinationChangedListener(it) }
            registeredController = null
            activeListener = null
            screenChangeDetector.clearComposeRoute()
            Logger.d(TAG, "Unregistered NavController")
        }
    }

    fun unregisterCurrent() {
        registeredController?.get()?.let { controller ->
            activeListener?.let { controller.removeOnDestinationChangedListener(it) }
        }
        registeredController = null
        activeListener = null
        screenChangeDetector.clearComposeRoute()
    }

    private fun handleDestinationChanged(destination: NavDestination, arguments: Bundle?) {
        if (destination is NavGraph) return
        if (destination.navigatorName == NAVIGATOR_NAME_DIALOG) return

        val screenName = destination.route ?: return

        if (processor != null) {
            val attrMap = extractArguments(arguments)
            destination.parent?.route?.let { attrMap[ATTR_NAV_GRAPH] = it }

            val event = NavigationEvent(
                screenName = screenName,
                attributes = attrMap,
                sourceType = NavigationEvent.SourceType.COMPOSE_ROUTE
            )

            if (!processor.process(event)) {
                Logger.d(TAG, "NavigationEventProcessor suppressed event for: $screenName")
                return
            }

            screenChangeDetector.onComposeRouteChanged(event.screenName, mapToAttributes(event.attributes))
        } else {
            val attrs = buildAttributes(arguments, destination)
            screenChangeDetector.onComposeRouteChanged(screenName, attrs)
        }
    }

    private fun extractArguments(arguments: Bundle?): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        if (arguments == null) return map
        for (key in arguments.keySet()) {
            if (key.startsWith(NAV_INTERNAL_KEY_PREFIX)) continue
            @Suppress("DEPRECATION")
            val value = arguments.get(key)
            if (value != null) {
                map[key] = value.toString()
            }
        }
        return map
    }

    private fun buildAttributes(arguments: Bundle?, destination: NavDestination): Attributes {
        val builder = Attributes.builder()
        if (arguments != null) {
            for (key in arguments.keySet()) {
                if (key.startsWith(NAV_INTERNAL_KEY_PREFIX)) continue
                @Suppress("DEPRECATION")
                val value = arguments.get(key)
                if (value != null) {
                    builder.put(AttributeKey.stringKey(key), value.toString())
                }
            }
        }
        destination.parent?.route?.let {
            builder.put(AttributeKey.stringKey(ATTR_NAV_GRAPH), it)
        }
        return builder.build()
    }

    private fun mapToAttributes(map: Map<String, String>): Attributes {
        if (map.isEmpty()) return Attributes.empty()
        val builder = Attributes.builder()
        for ((key, value) in map) {
            builder.put(AttributeKey.stringKey(key), value)
        }
        return builder.build()
    }

    private companion object {
        const val TAG = "ComposeNavigationTracker"
        const val NAV_INTERNAL_KEY_PREFIX = "android-support-nav:"
        const val NAVIGATOR_NAME_DIALOG = "dialog"
        const val ATTR_NAV_GRAPH = "nav.graph"
    }
}
