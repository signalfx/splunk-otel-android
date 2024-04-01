package com.splunk.rum;

import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

public class VolleyServerTimingAttributesExtractorTest {

    static final String TRACE_ID = "9499195c502eb217c448a68bfe0f967c";
    static final String SPAN_ID = "fe16eca542cd5d86";

    @Test
    public void serverTiming() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse("headerValue"))
                .thenReturn(new String[]{TRACE_ID, SPAN_ID});

        List<Header> responseHeaders =
                Collections.singletonList(new Header("Server-Timing", "headerValue"));
        RequestWrapper fakeRequest =
                new RequestWrapper(mock(Request.class), Collections.emptyMap());
        HttpResponse response = new HttpResponse(200, responseHeaders, "hello".getBytes());

        VolleyServerTimingAttributesExtractor attributesExtractor = new VolleyServerTimingAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onStart(attributesBuilder, null, fakeRequest);
        attributesExtractor.onEnd(attributesBuilder, null, fakeRequest, response, null);
        Attributes attributes = attributesBuilder.build();

        assertEquals(TRACE_ID, attributes.get(SplunkRum.LINK_TRACE_ID_KEY));
        assertEquals(SPAN_ID, attributes.get(SplunkRum.LINK_SPAN_ID_KEY));
    }

    @Test
    public void spanDecoration_noLinkingHeader() {
        ServerTimingHeaderParser headerParser = mock(ServerTimingHeaderParser.class);
        when(headerParser.parse(null)).thenReturn(new String[0]);

        RequestWrapper fakeRequest =
                new RequestWrapper(mock(Request.class), Collections.emptyMap());
        HttpResponse response = new HttpResponse(200, Collections.emptyList(), "hello".getBytes());

        VolleyServerTimingAttributesExtractor attributesExtractor = new VolleyServerTimingAttributesExtractor(headerParser);
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesExtractor.onEnd(attributesBuilder, null, fakeRequest, response, null);
        attributesExtractor.onStart(attributesBuilder, null, fakeRequest);
        Attributes attributes = attributesBuilder.build();

        assertNull(attributes.get(SplunkRum.LINK_TRACE_ID_KEY));
        assertNull(attributes.get(SplunkRum.LINK_SPAN_ID_KEY));
    }
}