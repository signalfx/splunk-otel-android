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

import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
import com.splunk.rum.integration.navigation.RumConstant
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.TimeUnit

/**
 * Emits OpenTelemetry navigation events (app.ui.navigation).
 * Updates [ScreenNameTracker] and caches events when the logger provider is not ready or installation is not complete.
 */
internal class NavigationEventEmitter {

    private companion object {
        const val TAG = "NavigationEventEmitter"
    }

    private val cache: MutableList<CachedEvent> = mutableListOf()
    private var isInstallComplete = false

    /**
     * Emit a navigation event for a screen change.
     * Resolves [ScreenNameTracker.screenName] as the previous screen, updates it to [screenName],
     * and emits a log record. Caches the event if installation is not complete.
     */
    fun emitNavigationEvent(screenName: String, attributes: Attributes = Attributes.empty()) {
        val timestamp = System.currentTimeMillis()
        val previousScreenName = ScreenNameTracker.screenName.takeIf {
            it != GlobalRumConstants.DEFAULT_SCREEN_NAME
        }
        ScreenNameTracker.screenName = screenName
        if (!isInstallComplete) {
            Logger.d(TAG) {
                "Install not complete, caching navigation event: $previousScreenName -> $screenName"
            }
            cache += CachedEvent(screenName, previousScreenName, attributes, timestamp)
            return
        }

        emitEventInternal(screenName, previousScreenName, attributes, timestamp)
    }

    private fun emitEventInternal(
        screenName: String,
        previousScreenName: String?,
        attributes: Attributes,
        timestamp: Long
    ) {
        val logger = SplunkOpenTelemetrySdk.instance?.sdkLoggerProvider
        if (logger == null) {
            Logger.w(TAG, "Logger provider not ready, skipping navigation event")
            return
        }

        Logger.d(TAG) { "Emitting navigation event: $previousScreenName -> $screenName" }

        val builder = logger.get(GlobalRumConstants.RUM_TRACER_NAME)
            .logRecordBuilder()
            .setTimestamp(timestamp, TimeUnit.MILLISECONDS)
            .setAllAttributes(attributes)
            .setAttribute(GlobalRumConstants.LOG_EVENT_NAME_KEY, RumConstant.NAVIGATION_LOG_EVENT_NAME)
            .setAttribute(GlobalRumConstants.COMPONENT_KEY, RumConstant.COMPONENT_NAVIGATION)
            .setAttribute(RumConstant.NAVIGATION_NAME_KEY, screenName)
            .setAttribute(GlobalRumConstants.SCREEN_NAME_KEY, screenName)

        previousScreenName?.let { builder.setAttribute(GlobalRumConstants.LAST_SCREEN_NAME_KEY, it) }
        builder.emit()
    }

    /**
     * Clears any cached events without emitting them.
     */
    fun clearCache() {
        cache.clear()
        isInstallComplete = true
    }

    /**
     * Process all cached events. Called when installation is complete.
     */
    fun processCachedEvents() {
        val cachedEvents = cache.toList()
        cache.clear()
        isInstallComplete = true

        if (cachedEvents.isNotEmpty()) {
            Logger.d(TAG) { "Processing cached navigation events (size: ${cachedEvents.size})" }
            cachedEvents.forEachFast { event ->
                emitEventInternal(event.screenName, event.previousScreenName, event.attributes, event.timestamp)
            }
        }
    }

    private data class CachedEvent(
        val screenName: String,
        val previousScreenName: String?,
        val attributes: Attributes,
        val timestamp: Long
    )
}
