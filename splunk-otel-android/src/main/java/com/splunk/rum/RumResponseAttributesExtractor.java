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

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import okhttp3.Request;
import okhttp3.Response;

class RumResponseAttributesExtractor implements AttributesExtractor<Request, Response> {

    public static final String SERVER_TIMING_HEADER = "server-timing";
    private final ServerTimingHeaderParser serverTimingHeaderParser;

    public RumResponseAttributesExtractor(ServerTimingHeaderParser serverTimingHeaderParser) {
        this.serverTimingHeaderParser = serverTimingHeaderParser;
    }

    @Override
    public void onStart(AttributesBuilder attributes, Context parentContext, Request request) {
        attributes.put(COMPONENT_KEY, "http");
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            Request request,
            Response response,
            Throwable error) {
        if (response != null) {
            onResponse(attributes, response);
        }
    }

    private void onResponse(AttributesBuilder attributes, Response response) {
        response.headers().forEach(header -> {
            if (!header.getFirst().equalsIgnoreCase(SERVER_TIMING_HEADER)) {
                return;
            }

            String[] ids = serverTimingHeaderParser.parse(header.getSecond());
            if (ids.length == 2) {
                attributes.put(LINK_TRACE_ID_KEY, ids[0]);
                attributes.put(LINK_SPAN_ID_KEY, ids[1]);
            }
        });
    }
}
