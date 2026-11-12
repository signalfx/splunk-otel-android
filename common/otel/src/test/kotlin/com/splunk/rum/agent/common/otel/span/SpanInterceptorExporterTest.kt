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

package com.splunk.rum.agent.common.otel.span

import com.splunk.rum.agent.common.storage.IAgentStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SpanInterceptorExporterTest {

    private val storage = mock(IAgentStorage::class.java).also {
        `when`(it.readOrCreateAppInstallationId()).thenReturn("installation-id")
    }

    @Test
    fun `interceptor sees the stored app installation id under one resource`() {
        val resource = resource(IAgentStorage.PENDING_APP_INSTALLATION_ID)
        val intercepted = mutableListOf<SpanData>()
        val delegate = CapturingSpanExporter()
        val exporter = SpanInterceptorExporter(delegate, { span -> span.also { intercepted += it } }, storage)

        exporter.export(mutableListOf(span("first", resource), span("second", resource)))

        intercepted.forEach { assertEquals("installation-id", it.resource.getAttribute(APP_INSTALLATION_ID_KEY)) }
        assertSame(intercepted[0].resource, intercepted[1].resource)
        assertEquals(intercepted, delegate.spans)
    }

    @Test
    fun `exported payload contains the app installation id once and no pending value`() {
        val resource = resource(IAgentStorage.PENDING_APP_INSTALLATION_ID)
        val delegate = CapturingSpanExporter()
        SpanInterceptorExporter(delegate, null, storage)
            .export(mutableListOf(span("first", resource), span("second", resource)))

        val payload = ByteArrayOutputStream().use {
            TraceRequestMarshaler.create(delegate.spans).writeBinaryTo(it)
            String(it.toByteArray(), Charsets.ISO_8859_1)
        }

        assertEquals(1, payload.split("installation-id").size - 1)
        assertEquals(0, payload.split(IAgentStorage.PENDING_APP_INSTALLATION_ID).size - 1)
    }

    @Test
    fun `spans without the pending app installation id are not changed`() {
        val span = span("span", resource("stored-installation-id"))
        val delegate = CapturingSpanExporter()

        SpanInterceptorExporter(delegate, null, storage).export(mutableListOf(span))

        assertSame(span, delegate.spans.single())
        verify(storage, never()).readOrCreateAppInstallationId()
    }

    private fun resource(appInstallationId: String): Resource = Resource.create(
        Attributes.of(AttributeKey.stringKey("service.name"), "app", APP_INSTALLATION_ID_KEY, appInstallationId)
    )

    private fun span(name: String, resource: Resource): SpanData {
        val exporter = CapturingSpanExporter()
        SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build()
            .get("test")
            .spanBuilder(name)
            .startSpan()
            .end()

        return exporter.spans.single()
    }

    private class CapturingSpanExporter : SpanExporter {
        val spans = mutableListOf<SpanData>()

        override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
            this.spans += spans
            return CompletableResultCode.ofSuccess()
        }

        override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

        override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
    }

    private companion object {
        val APP_INSTALLATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("app.installation.id")
    }
}
