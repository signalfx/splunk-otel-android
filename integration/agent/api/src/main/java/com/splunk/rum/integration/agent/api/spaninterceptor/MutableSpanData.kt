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

package com.splunk.rum.integration.agent.api.spaninterceptor

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData

/**
 * Mutable implementation of [SpanData].
 */
class MutableSpanData(private val spanData: SpanData) : SpanData {

    private var name: String? = null
    private var kind: SpanKind? = null
    private var spanContext: SpanContext? = null
    private var parentSpanContext: SpanContext? = null
    private var status: StatusData? = null
    private var startEpochNanos: Long? = null
    private var attributes: Attributes? = null
    private var events: MutableList<EventData>? = null
    private var links: MutableList<LinkData>? = null
    private var endEpochNanos: Long? = null
    private var hasEnded: Boolean? = null
    private var totalRecordedEvents: Int? = null
    private var totalRecordedLinks: Int? = null
    private var totalAttributeCount: Int? = null
    private var instrumentationLibraryInfo: InstrumentationLibraryInfo? = null
    private var resource: Resource? = null

    override fun getName(): String = name ?: spanData.name
    fun setName(value: String) {
        name = value
    }

    override fun getKind(): SpanKind = kind ?: spanData.kind
    fun setKind(value: SpanKind) {
        kind = value
    }

    override fun getSpanContext(): SpanContext = spanContext ?: spanData.spanContext
    fun setSpanContext(value: SpanContext) {
        spanContext = value
    }

    override fun getParentSpanContext(): SpanContext = parentSpanContext ?: spanData.parentSpanContext
    fun setParentSpanContext(value: SpanContext) {
        parentSpanContext = value
    }

    override fun getStatus(): StatusData = status ?: spanData.status
    fun setStatus(value: StatusData) {
        status = value
    }

    override fun getStartEpochNanos(): Long = startEpochNanos ?: spanData.startEpochNanos
    fun setStartEpochNanos(value: Long) {
        startEpochNanos = value
    }

    override fun getAttributes(): Attributes = attributes ?: spanData.attributes
    fun setAttributes(value: Attributes) {
        attributes = value
    }

    override fun getEvents(): MutableList<EventData> = events ?: spanData.events.toMutableList()
    fun setEvents(value: List<EventData>) {
        events = value.toMutableList()
    }

    override fun getLinks(): MutableList<LinkData> = links ?: spanData.links.toMutableList()
    fun setLinks(value: List<LinkData>) {
        links = value.toMutableList()
    }

    override fun getEndEpochNanos(): Long = endEpochNanos ?: spanData.endEpochNanos
    fun setEndEpochNanos(value: Long) {
        endEpochNanos = value
    }

    override fun hasEnded(): Boolean = hasEnded ?: spanData.hasEnded()
    fun setHasEnded(value: Boolean) {
        hasEnded = value
    }

    override fun getTotalRecordedEvents(): Int = totalRecordedEvents ?: spanData.totalRecordedEvents
    fun setTotalRecordedEvents(value: Int) {
        totalRecordedEvents = value
    }

    override fun getTotalRecordedLinks(): Int = totalRecordedLinks ?: spanData.totalRecordedLinks
    fun setTotalRecordedLinks(value: Int) {
        totalRecordedLinks = value
    }

    override fun getTotalAttributeCount(): Int = totalAttributeCount ?: spanData.totalAttributeCount
    fun setTotalAttributeCount(value: Int) {
        totalAttributeCount = value
    }

    override fun getInstrumentationLibraryInfo(): InstrumentationLibraryInfo = instrumentationLibraryInfo ?: spanData.instrumentationLibraryInfo
    fun setInstrumentationLibraryInfo(value: InstrumentationLibraryInfo) {
        instrumentationLibraryInfo = value
    }

    override fun getResource(): Resource = resource ?: spanData.resource
    fun setResource(value: Resource) {
        resource = value
    }
}
