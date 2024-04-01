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

import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

/**
 * This class is just responsible for setting the COMPONENT_KEY attribute to "http" on Volley client
 * spans.
 */
class VolleyComponentKeySetter implements AttributesExtractor<RequestWrapper, HttpResponse> {
    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, RequestWrapper requestWrapper) {
        attributes.put(COMPONENT_KEY, "http");
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            RequestWrapper requestWrapper,
            HttpResponse httpResponse,
            Throwable error) {}
}
