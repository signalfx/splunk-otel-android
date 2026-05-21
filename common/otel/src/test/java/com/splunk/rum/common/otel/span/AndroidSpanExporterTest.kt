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

package com.splunk.rum.common.otel.span

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AndroidSpanExporterTest {

    @Test
    fun `filterInternalSpanAttributes removes span attributes with internal prefix`() {
        val publicKey = AttributeKey.stringKey("public.attribute")
        val internalKey = AttributeKey.stringKey("splunk.agent.internal.value")
        val internalLongKey = AttributeKey.longKey("splunk.agent.internal.counter")
        val span = spanDataWithAttributes(
            Attributes.builder()
                .put(publicKey, "kept")
                .put(internalKey, "removed")
                .put(internalLongKey, 1L)
                .build()
        )

        val filteredSpan = AndroidSpanExporter.filterInternalSpanAttributes(listOf(span)).single()

        assertEquals("kept", filteredSpan.attributes.get(publicKey))
        assertNull(filteredSpan.attributes.get(internalKey))
        assertNull(filteredSpan.attributes.get(internalLongKey))
        assertEquals(1, filteredSpan.totalAttributeCount)
    }

    @Test
    fun `filterInternalSpanAttributes keeps original span when it has no internal attributes`() {
        val span = spanDataWithAttributes(
            Attributes.builder()
                .put("public.attribute", "kept")
                .put("another.attribute", true)
                .build()
        )

        val filteredSpan = AndroidSpanExporter.filterInternalSpanAttributes(listOf(span)).single()

        assertSame(span, filteredSpan)
    }

    private fun spanDataWithAttributes(attributes: Attributes): SpanData {
        val span = mock(SpanData::class.java)
        `when`(span.attributes).thenReturn(attributes)
        `when`(span.totalAttributeCount).thenReturn(attributes.size())
        return span
    }
}
