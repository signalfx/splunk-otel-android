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

import static io.opentelemetry.semconv.incubating.HttpIncubatingAttributes.HTTP_RESPONSE_BODY_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class VolleyContentLengthAttributesExtractorTest {

    @Test
    public void contentLength() {

        List<Header> responseHeaders =
                Collections.singletonList(new Header("Content-Length", "90210"));
        RequestWrapper fakeRequest =
                new RequestWrapper(mock(Request.class), Collections.emptyMap());
        HttpResponse response = new HttpResponse(200, responseHeaders, "zzz".getBytes());

        VolleyContentLengthAttributesExtractor attributesExtractor =
                new VolleyContentLengthAttributesExtractor();
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onStart(attributesBuilder, null, fakeRequest);
        attributesExtractor.onEnd(attributesBuilder, null, fakeRequest, response, null);
        Attributes attributes = attributesBuilder.build();

        assertThat(attributes.get(HTTP_RESPONSE_BODY_SIZE)).isEqualTo(90210L);
    }
}
