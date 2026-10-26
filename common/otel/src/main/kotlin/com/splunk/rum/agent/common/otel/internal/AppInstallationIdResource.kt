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

package com.splunk.rum.agent.common.otel.internal

import com.splunk.rum.agent.common.storage.IAgentStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.Value
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.logs.data.Body
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.data.internal.ExtendedLogRecordData
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.SpanData

/**
 * Replaces [IAgentStorage.PENDING_APP_INSTALLATION_ID] in a resource with the stored app installation ID.
 * May wait for storage, so it is used only on export threads.
 */
internal object AppInstallationIdResource {
    private val APP_INSTALLATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("app.installation.id")

    /** OTLP marshalers group telemetry by resource identity, so a resource is completed only once. */
    private var lastCompleted: CompletedResource? = null

    fun complete(resource: Resource, storage: IAgentStorage): Resource =
        if (resource.getAttribute(APP_INSTALLATION_ID_KEY) == IAgentStorage.PENDING_APP_INSTALLATION_ID) {
            completed(resource, storage)
        } else {
            resource
        }

    @Synchronized
    private fun completed(resource: Resource, storage: IAgentStorage): Resource {
        lastCompleted?.let { if (it.source === resource) return it.completed }

        val completed = resource.toBuilder()
            .put(APP_INSTALLATION_ID_KEY, storage.readOrCreateAppInstallationId())
            .build()
        lastCompleted = CompletedResource(resource, completed)
        return completed
    }

    private class CompletedResource(val source: Resource, val completed: Resource)
}

internal fun SpanData.withAppInstallationId(storage: IAgentStorage): SpanData {
    val resource = AppInstallationIdResource.complete(resource, storage)

    return if (resource === this.resource) {
        this
    } else {
        object : DelegatingSpanData(this) {
            override fun getResource(): Resource = resource
        }
    }
}

internal fun LogRecordData.withAppInstallationId(storage: IAgentStorage): LogRecordData {
    val resource = AppInstallationIdResource.complete(resource, storage)

    return if (resource === this.resource) this else ResourceReplacingLogRecordData(this, resource)
}

private class ResourceReplacingLogRecordData(private val delegate: LogRecordData, private val resource: Resource) :
    ExtendedLogRecordData {
    override fun getResource(): Resource = resource

    override fun getInstrumentationScopeInfo(): InstrumentationScopeInfo = delegate.instrumentationScopeInfo

    override fun getTimestampEpochNanos(): Long = delegate.timestampEpochNanos

    override fun getObservedTimestampEpochNanos(): Long = delegate.observedTimestampEpochNanos

    override fun getSpanContext(): SpanContext = delegate.spanContext

    override fun getSeverity(): Severity = delegate.severity

    override fun getSeverityText(): String? = delegate.severityText

    @Deprecated("Deprecated in LogRecordData", ReplaceWith("getBodyValue()"))
    @Suppress("DEPRECATION")
    override fun getBody(): Body = delegate.body

    override fun getBodyValue(): Value<*>? = delegate.bodyValue

    override fun getAttributes(): Attributes = delegate.attributes

    override fun getTotalAttributeCount(): Int = delegate.totalAttributeCount

    override fun getEventName(): String? = (delegate as? ExtendedLogRecordData)?.eventName
}
