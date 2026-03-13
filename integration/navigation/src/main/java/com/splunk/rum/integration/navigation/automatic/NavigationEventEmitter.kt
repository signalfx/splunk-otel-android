/*
 * Copyright 2025 Splunk Inc.
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
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
import com.splunk.rum.integration.navigation.RumConstant
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.TimeUnit

/**
 * Emits OpenTelemetry navigation events (device.app.ui.navigation).
 * Updates [ScreenNameTracker] and caches events when the logger provider is not ready or installation is not complete.
 */
internal class NavigationEventEmitter {

    private companion object {
        const val TAG = "NavigationEventEmitter"
    }

    private val lock = Any()
    private val cache: MutableList<CachedEvent> = mutableListOf()

    @Volatile
    private var isInstallComplete = false

    /**
     * Emit a navigation event for a screen change.
     * Updates [ScreenNameTracker.screenName] and emits a log record.
     * Caches the event if installation is not complete.
     */
    fun emitNavigationEvent(
        screenName: String,
        previousScreenName: String?,
        attributes: Attributes = Attributes.empty()
    ) {
        val timestamp = System.currentTimeMillis()
        synchronized(lock) {
            if (!isInstallComplete) {
                Logger.d(TAG) {
                    "Install not complete, caching navigation event: $previousScreenName -> $screenName"
                }
                cache += CachedEvent(screenName, previousScreenName, attributes, timestamp)
                return
            }
        }

        emitEventInternal(screenName, previousScreenName, attributes, timestamp)
    }

    /**
     * Actually emit the event to OpenTelemetry.
     */
    private fun emitEventInternal(
        screenName: String,
        previousScreenName: String?,
        attributes: Attributes,
        timestamp: Long
    ) {
        ScreenNameTracker.screenName = screenName

        val logger = SplunkOpenTelemetrySdk.instance?.sdkLoggerProvider
        if (logger == null) {
            Logger.w(TAG, "Logger provider not ready, skipping navigation event")
            return
        }

        Logger.d(TAG) { "Emitting navigation event: $previousScreenName -> $screenName" }

        // Build a single immutable Attributes snapshot so we don't rely on the builder's
        // internal mutable state, which may be reused for the next record and can cause
        // wrong attributes on export when many events are emitted in quick succession.
        val attrsBuilder = io.opentelemetry.api.common.Attributes.builder()
            .put(GlobalRumConstants.LOG_EVENT_NAME_KEY, RumConstant.NAVIGATION_LOG_EVENT_NAME)
            .put(GlobalRumConstants.COMPONENT_KEY, RumConstant.COMPONENT_NAVIGATION)
            .put(GlobalRumConstants.SCREEN_NAME_KEY, screenName)
        previousScreenName?.let {
            attrsBuilder.put(GlobalRumConstants.LAST_SCREEN_NAME_KEY, it)
        }
        attributes.forEach { key, value ->
            when (value) {
                is String -> attrsBuilder.put(AttributeKey.stringKey(key.key), value)
                is Long -> attrsBuilder.put(AttributeKey.longKey(key.key), value)
                is Double -> attrsBuilder.put(AttributeKey.doubleKey(key.key), value)
                is Boolean -> attrsBuilder.put(AttributeKey.booleanKey(key.key), value)
                is List<*> -> attrsBuilder.put(
                    AttributeKey.stringKey(key.key),
                    value.joinToString(",") { it.toString() }
                )
                else -> attrsBuilder.put(AttributeKey.stringKey(key.key), value.toString())
            }
        }
        val allAttributes = attrsBuilder.build()

        logger.get(GlobalRumConstants.RUM_TRACER_NAME)
            .logRecordBuilder()
            .setTimestamp(timestamp, TimeUnit.MILLISECONDS)
            .setAllAttributes(allAttributes)
            .emit()
    }

    /**
     * Process all cached events. Called when installation is complete.
     */
    fun processCachedEvents() {
        val cachedEvents: List<CachedEvent>
        synchronized(lock) {
            cachedEvents = cache.toList()
            cache.clear()
            isInstallComplete = true
        }
        if (cachedEvents.isNotEmpty()) {
            Logger.d(TAG) { "Processing cached navigation events (size: ${cachedEvents.size})" }
            cachedEvents.forEach { event ->
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
