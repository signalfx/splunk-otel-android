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

import static com.splunk.rum.SplunkRum.LINK_SPAN_ID_KEY;
import static com.splunk.rum.SplunkRum.LINK_TRACE_ID_KEY;
import static com.splunk.rum.VolleyResponseUtils.getHeader;

import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

/**
 * This class is responsible for parsing the Server-Timing header and setting the linked trace id
 * and span id attributes.
 */
class VolleyServerTimingAttributesExtractor
        implements AttributesExtractor<RequestWrapper, HttpResponse> {

    private final ServerTimingHeaderParser serverTimingHeaderParser;

    public VolleyServerTimingAttributesExtractor(
            ServerTimingHeaderParser serverTimingHeaderParser) {
        this.serverTimingHeaderParser = serverTimingHeaderParser;
    }

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, RequestWrapper requestWrapper) {}

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            RequestWrapper requestWrapper,
            HttpResponse httpResponse,
            Throwable error) {
        if (httpResponse == null) {
            return;
        }
        String serverTimingHeader = getHeader(httpResponse, "Server-Timing");

        String[] ids = serverTimingHeaderParser.parse(serverTimingHeader);
        if (ids.length == 2) {
            attributes.put(LINK_TRACE_ID_KEY, ids[0]);
            attributes.put(LINK_SPAN_ID_KEY, ids[1]);
        }
    }
}
