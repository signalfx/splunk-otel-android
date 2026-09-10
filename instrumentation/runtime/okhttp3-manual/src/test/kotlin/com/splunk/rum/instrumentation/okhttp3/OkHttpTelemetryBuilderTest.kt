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

package com.splunk.rum.instrumentation.okhttp3

import com.splunk.rum.instrumentation.okhttp3.internal.PeerServiceAttributesExtractor
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.internal.HttpConstants
import io.opentelemetry.instrumentation.api.internal.ServiceLoaderUtil
import java.util.ServiceLoader
import okhttp3.Interceptor
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OkHttpTelemetryBuilderTest {

    @After
    fun tearDown() {
        ServiceLoaderUtil.setLoadFunction { serviceType -> ServiceLoader.load(serviceType) }
    }

    @Test
    fun build_withNoopOpenTelemetry_succeeds() {
        val telemetry = OkHttpTelemetry.builder(OpenTelemetry.noop()).build()
        assertNotNull(telemetry)
    }

    @Test
    fun build_restoresTheDefaultServiceLoaderAfterInstrumenterConstruction() {
        ServiceLoaderUtil.setLoadFunction { listOf("host-provider") }

        OkHttpTelemetry.builder(OpenTelemetry.noop()).build()

        assertFalse(ServiceLoaderUtil.load(String::class.java).iterator().hasNext())
    }

    @Test
    fun setKnownMethods_acceptsStandardHttpMethods() {
        val knownMethods = HttpConstants.KNOWN_METHODS

        val telemetry = OkHttpTelemetry.builder(OpenTelemetry.noop())
            .setKnownMethods(knownMethods)
            .build()

        assertNotNull(telemetry)
        assertEquals(
            setOf("CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE"),
            knownMethods
        )
    }

    @Test
    fun setPeerServiceMapping_buildsSuccessfully() {
        val telemetry = OkHttpTelemetry.builder(OpenTelemetry.noop())
            .setPeerServiceMapping(mapOf("api.example.test:8443" to "checkout-service"))
            .build()

        assertNotNull(telemetry)
    }

    @Test
    fun setSpanNameExtractor_customizerIsAppliedDuringBuild() {
        var invoked = false

        val telemetry = OkHttpTelemetry.builder(OpenTelemetry.noop())
            .setSpanNameExtractor { spanNameExtractor ->
                invoked = true
                spanNameExtractor
            }
            .build()

        assertNotNull(telemetry)
        assertEquals(true, invoked)
    }

    @Test
    fun build_doesNotAccumulatePeerServiceExtractors() {
        val builder = OkHttpTelemetry.builder(OpenTelemetry.noop())
            .setPeerServiceMapping(mapOf("api.example.test:8443" to "checkout-service"))
        builder.build()

        val rebuiltTelemetry = builder
            .setPeerServiceMapping(mapOf("api.example.test:8443" to "payments-service"))
            .build()

        val instrumenterField = OkHttpTelemetry::class.java.getDeclaredField("instrumenter")
        instrumenterField.isAccessible = true
        val instrumenter = instrumenterField.get(rebuiltTelemetry)
        val extractorsField = instrumenter.javaClass.getDeclaredField("attributesExtractors")
        extractorsField.isAccessible = true
        val extractors = extractorsField.get(instrumenter) as Array<*>

        assertEquals(1, extractors.count { it is PeerServiceAttributesExtractor })
    }

    @Test
    fun build_registersPeerServiceExtractorBeforeAdditionalExtractors() {
        val additionalExtractor = object : AttributesExtractor<Interceptor.Chain, Response> {
            override fun onStart(attributes: AttributesBuilder, parentContext: Context, request: Interceptor.Chain) =
                Unit

            override fun onEnd(
                attributes: AttributesBuilder,
                context: Context,
                request: Interceptor.Chain,
                response: Response?,
                error: Throwable?
            ) = Unit
        }

        val telemetry = OkHttpTelemetry.builder(OpenTelemetry.noop())
            .setPeerServiceMapping(mapOf("api.example.test:8443" to "checkout-service"))
            .addAttributesExtractor(additionalExtractor)
            .build()

        val instrumenterField = OkHttpTelemetry::class.java.getDeclaredField("instrumenter")
        instrumenterField.isAccessible = true
        val instrumenter = instrumenterField.get(telemetry)
        val extractorsField = instrumenter.javaClass.getDeclaredField("attributesExtractors")
        extractorsField.isAccessible = true
        val extractors = extractorsField.get(instrumenter) as Array<*>
        val peerServiceIndex = extractors.indexOfFirst { it is PeerServiceAttributesExtractor }
        val additionalExtractorIndex = extractors.indexOfFirst { it === additionalExtractor }

        assertTrue(peerServiceIndex >= 0)
        assertTrue(additionalExtractorIndex >= 0)
        assertTrue(peerServiceIndex < additionalExtractorIndex)
    }
}
