package com.splunk.rum;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import androidx.annotation.Nullable;

import com.android.volley.Header;
import com.android.volley.toolbox.HttpResponse;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

class VolleyResponseAttributesExtractor implements AttributesExtractor<RequestWrapper, HttpResponse> {
    static final AttributeKey<String> LINK_TRACE_ID_KEY = stringKey("link.traceId");
    static final AttributeKey<String> LINK_SPAN_ID_KEY = stringKey("link.spanId");

    private final ServerTimingHeaderParser serverTimingHeaderParser;

    public VolleyResponseAttributesExtractor(ServerTimingHeaderParser serverTimingHeaderParser) {
        this.serverTimingHeaderParser = serverTimingHeaderParser;
    }

    @Override
    public void onStart(AttributesBuilder attributes, RequestWrapper requestWrapper) {
        attributes.put(SplunkRum.COMPONENT_KEY, "http");
    }

    @Override
    public void onEnd(AttributesBuilder attributes, RequestWrapper requestWrapper, @Nullable HttpResponse response, @Nullable Throwable error) {
        if (response != null) {
            onResponse(attributes, response);
        }
        if (error != null) {
            onError(attributes, error);
        }
    }


    private void onResponse(AttributesBuilder attributes, HttpResponse response) {
        recordContentLength(attributes, response);
        String serverTimingHeader = getHeader(response, "Server-Timing");

        String[] ids = serverTimingHeaderParser.parse(serverTimingHeader);
        if (ids.length == 2) {
            attributes.put(LINK_TRACE_ID_KEY, ids[0]);
            attributes.put(LINK_SPAN_ID_KEY, ids[1]);
        }
    }

    private void recordContentLength(AttributesBuilder attributesBuilder, HttpResponse response) {
        //make a best low-impact effort at getting the content length on the response.
        String contentLengthHeader = getHeader(response, "Content-Length");
        if (contentLengthHeader != null) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader);
                if (contentLength > 0) {
                    attributesBuilder.put(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH, contentLength);
                }
            } catch (NumberFormatException e) {
                //who knows what we got back? It wasn't a number!
            }
        }
    }

    private void onError(AttributesBuilder attributes, Throwable error) {
        SplunkRum.addExceptionAttributes((key, value) -> attributes.put((AttributeKey<? super Object>) key, value), error);
    }

    private String getHeader(HttpResponse response, String headerName){
        for(Header header : response.getHeaders()){
            if(header.getName().equals(headerName)){
                return header.getValue();
            }
        }
        return null;
    }
}
