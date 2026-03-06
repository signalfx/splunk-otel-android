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

package com.splunk.rum.integration.lifecycle

import android.app.Activity
import androidx.fragment.app.Fragment
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.lifecycle.model.LifecycleAction
import com.splunk.rum.integration.lifecycle.model.LifecycleEventData
import java.util.concurrent.TimeUnit

/**
 * Emits OpenTelemetry lifecycle events for Activities and Fragments.
 * Caches events when the logger provider is not ready (such as in Flutter/React Native environments).
 */
internal class LifecycleEventEmitter(private val allowedEvents: Set<LifecycleAction>) {

    private companion object {
        const val TAG = "LifecycleEventEmitter"
    }

    private val lock = Any()
    private val cache: MutableList<LifecycleEventData> = mutableListOf()

    @Volatile
    private var isInstallComplete = false

    /**
     * Emit a lifecycle event for an Activity.
     * Uses class-level tracking: element.id = fully qualified class name
     */
    fun emitActivityEvent(activity: Activity, action: LifecycleAction) {
        val elementId = activity::class.java.name // Class-level tracking
        val elementName = activity::class.java.simpleName

        emitEvent(
            elementType = "Activity",
            elementName = elementName,
            elementId = elementId,
            action = action,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Emit a lifecycle event for a Fragment.
     * Uses class-level tracking: element.id = fully qualified class name
     */
    fun emitFragmentEvent(fragment: Fragment, action: LifecycleAction) {
        val elementId = fragment::class.java.name // Class-level tracking
        val elementName = fragment::class.java.simpleName
        emitEvent(
            elementType = "Fragment",
            elementName = elementName,
            elementId = elementId,
            action = action,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Emit an OTel lifecycle event.
     * Caches the event if the logger provider is not ready or installation is not complete.
     * Filters events based on allowedEvents configuration.
     */
    private fun emitEvent(
        elementType: String,
        elementName: String,
        elementId: String,
        action: LifecycleAction,
        timestamp: Long
    ) {
        if (action !in allowedEvents) {
            Logger.d(TAG) {
                "Lifecycle event filtered out (not in allowedEvents): $elementType.$elementName - ${action.attributeValue}"
            }
            return
        }

        synchronized(lock) {
            if (!isInstallComplete) {
                Logger.d(TAG) {
                    "Install not complete, caching lifecycle event: $elementType.$elementName - ${action.attributeValue}"
                }
                cache += LifecycleEventData(elementType, elementName, elementId, action, timestamp)
                return
            }
        }

        emitEventInternal(elementType, elementName, elementId, action, timestamp)
    }

    /**
     * Actually emit the event to OpenTelemetry.
     */
    private fun emitEventInternal(
        elementType: String,
        elementName: String,
        elementId: String,
        action: LifecycleAction,
        timestamp: Long
    ) {
        val logger = SplunkOpenTelemetrySdk.instance?.sdkLoggerProvider

        if (logger == null) {
            Logger.w(TAG, "Logger provider not ready, skipping lifecycle event")
            return
        }

        Logger.d(TAG) { "Emitting lifecycle event: $elementType.$elementName - ${action.attributeValue}" }

        logger.get(RumConstants.RUM_TRACER_NAME)
            .logRecordBuilder()
            .setTimestamp(timestamp, TimeUnit.MILLISECONDS)
            .setAttribute(RumConstants.LOG_EVENT_NAME_KEY, RumConstants.UI_LIFECYCLE_NAME)
            .setAttribute(RumConstants.COMPONENT_KEY, RumConstants.UI_LIFECYCLE_COMPONENT)
            .setAttribute(RumConstants.ELEMENT_TYPE_KEY, elementType)
            .setAttribute(RumConstants.ELEMENT_NAME_KEY, elementName)
            .setAttribute(RumConstants.ELEMENT_ID_KEY, elementId)
            .setAttribute(RumConstants.LIFECYCLE_ACTION_KEY, action.attributeValue)
            .emit()
    }

    /**
     * Process all cached events. Called when installation is complete.
     * Filters events based on allowedEvents configuration.
     */
    fun processCachedEvents() {
        val cachedEvents: List<LifecycleEventData>

        synchronized(lock) {
            cachedEvents = cache.toList()
            cache.clear()
            isInstallComplete = true
        }

        if (cachedEvents.isNotEmpty()) {
            Logger.d(TAG) { "Processing cached lifecycle events (size: ${cachedEvents.size})" }
            cachedEvents.forEachFast { event ->
                if (event.action !in allowedEvents) {
                    Logger.d(TAG) {
                        "Cached lifecycle event filtered out (not in allowedEvents): ${event.elementType}.${event.elementName} - ${event.action.attributeValue}"
                    }
                    return@forEachFast
                }

                Logger.d(TAG) {
                    "Processing cached event: ${event.elementType}.${event.elementName} - ${event.action.attributeValue}"
                }
                emitEventInternal(
                    event.elementType,
                    event.elementName,
                    event.elementId,
                    event.action,
                    event.timestamp
                )
            }
        }
    }
}
