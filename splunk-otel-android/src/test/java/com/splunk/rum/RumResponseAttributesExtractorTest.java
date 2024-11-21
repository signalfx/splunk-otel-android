/*
 * Copyright Splunk Inc.
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

package com.splunk.rum;

import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static com.splunk.rum.SplunkRum.LINK_SPAN_ID_KEY;
import static com.splunk.rum.SplunkRum.LINK_TRACE_ID_KEY;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class RumResponseAttributesExtractorTest {

    @Test
    void spanDecoration() {
        Request fakeRequest = mock(Request.class);
        Response response =
                getBaseRuestBuilder(fakeRequest)
                        .addHeader(
                                "Server-Timing",
                                "traceparent;desc=\"00-00000000000000000000000000000001-0000000000000001-01\"")
                        .build();
        Attributes attributes = performAttributesExtraction(fakeRequest, response);

        assertThat(attributes)
                .containsOnly(
                        entry(COMPONENT_KEY, "http"),
                        entry(LINK_TRACE_ID_KEY, "00000000000000000000000000000001"),
                        entry(LINK_SPAN_ID_KEY, "0000000000000001"));
    }

    @Test
    void ignoresMalformed() {
        Request fakeRequest = mock(Request.class);
        Response response =
                getBaseRuestBuilder(fakeRequest)
                        .addHeader("Server-Timing", "othervalue 1")
                        .addHeader(
                                "Server-Timing",
                                "traceparent;desc=\"00-00000000000000000000000000000001-0000000000000001-01\"")
                        .addHeader("Server-Timing", "othervalue 2")
                        .build();
        Attributes attributes = performAttributesExtraction(fakeRequest, response);

        assertThat(attributes)
                .containsOnly(
                        entry(COMPONENT_KEY, "http"),
                        entry(LINK_TRACE_ID_KEY, "00000000000000000000000000000001"),
                        entry(LINK_SPAN_ID_KEY, "0000000000000001"));
    }

    @Test
    void lastMatchingWins() {
        Request fakeRequest = mock(Request.class);
        Response response =
                getBaseRuestBuilder(fakeRequest)
                        .addHeader(
                                "Server-Timing",
                                "traceparent;desc=\"00-00000000000000000000000000000001-0000000000000001-01\"")
                        .addHeader(
                                "Server-Timing",
                                "traceparent;desc=\"00-00000000000000000000000000000002-0000000000000002-01\"")
                        .addHeader(
                                "Server-Timing",
                                "traceparent;desc=\"00-00000000000000000000000000000003-0000000000000003-01\"")
                        .build();
        Attributes attributes = performAttributesExtraction(fakeRequest, response);

        assertThat(attributes)
                .containsOnly(
                        entry(COMPONENT_KEY, "http"),
                        entry(LINK_TRACE_ID_KEY, "00000000000000000000000000000003"),
                        entry(LINK_SPAN_ID_KEY, "0000000000000003"));
    }

    @Test
    void spanDecoration_noLinkingHeader() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse(null)).thenReturn(new String[0]);

        Request fakeRequest = mock(Request.class);
        Response response = getBaseRuestBuilder(fakeRequest).build();
        Attributes attributes = performAttributesExtraction(fakeRequest, response);

        assertThat(attributes).containsOnly(entry(COMPONENT_KEY, "http"));
    }

    private static Attributes performAttributesExtraction(Request fakeRequest, Response response) {
        RumResponseAttributesExtractor attributesExtractor =
                new RumResponseAttributesExtractor(new ServerTimingHeaderParser());
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onStart(attributesBuilder, Context.root(), fakeRequest);
        attributesExtractor.onEnd(attributesBuilder, Context.root(), fakeRequest, response, null);
        Attributes attributes = attributesBuilder.build();
        return attributes;
    }

    private Response.Builder getBaseRuestBuilder(Request fakeRequest) {
        return new Response.Builder()
                .request(fakeRequest)
                .protocol(Protocol.HTTP_1_1)
                .message("hello")
                .code(200);
    }
}
