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

package com.splunk.rum.integration.agent.api.internal

import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.integration.agent.internal.user.InternalUserTrackingMode
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StoredIdentityAttributesTest {

    private val pendingResource =
        Resource.create(Attributes.of(APP_INSTALLATION_ID_KEY, IAgentStorage.PENDING_APP_INSTALLATION_ID))

    @Test
    fun `span gets the user id that the pending user id stands for`() {
        val attributes = StoredIdentityAttributes(AtomicReference(), FakeUserManager("user-id"))
        val span = span(Attributes.of(USER_ID_KEY, "splunk.internal.pendingUserId"))

        val completed = attributes.applyTo(span)

        assertEquals("user-id", completed.attributes.get(USER_ID_KEY))
        assertEquals(span.name, completed.name)
    }

    @Test
    fun `resource attributes for logging contain the app installation id once it is read`() {
        val attributes = StoredIdentityAttributes(AtomicReference("installation-id"), FakeUserManager())

        assertEquals(
            "installation-id",
            attributes.resourceAttributesIfRead(pendingResource).get(APP_INSTALLATION_ID_KEY)
        )
    }

    @Test
    fun `resource attributes for logging stay pending while the app installation id is read`() {
        val attributes = StoredIdentityAttributes(AtomicReference(), FakeUserManager())

        assertSame(pendingResource.attributes, attributes.resourceAttributesIfRead(pendingResource))
    }

    private fun span(attributes: Attributes): SpanData {
        val captured = mutableListOf<SpanData>()
        val exporter = object : SpanExporter {
            override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
                captured += spans
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }

        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build()
            .get("test")
            .spanBuilder("span")
            .setAllAttributes(attributes)
            .startSpan()
            .end()

        return captured.single()
    }

    private class FakeUserManager(private val pendingUserId: String? = null) : IUserManager {
        override var trackingMode: InternalUserTrackingMode = InternalUserTrackingMode.ANONYMOUS_TRACKING
        override val userId: String? = pendingUserId

        override fun pendingUserId(wait: Boolean): String? = pendingUserId
    }

    private companion object {
        val APP_INSTALLATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("app.installation.id")
        val USER_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("user.anonymous_id")
    }
}
