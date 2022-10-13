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

import static com.splunk.rum.SplunkRum.ERROR_MESSAGE_KEY;
import static com.splunk.rum.SplunkRum.ERROR_TYPE_KEY;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class SplunkSpanDataModifier implements SpanExporter {

    static final AttributeKey<String> SPLUNK_OPERATION_KEY = stringKey("_splunk_operation");

    private static final Set<AttributeKey<String>> resourceAttributesToCopy =
            unmodifiableSet(
                    new HashSet<>(
                            asList(
                                    ResourceAttributes.DEPLOYMENT_ENVIRONMENT,
                                    ResourceAttributes.DEVICE_MODEL_NAME,
                                    ResourceAttributes.DEVICE_MODEL_IDENTIFIER,
                                    ResourceAttributes.OS_NAME,
                                    ResourceAttributes.OS_TYPE,
                                    ResourceAttributes.OS_VERSION,
                                    SplunkRum.APP_NAME_KEY,
                                    SplunkRum.RUM_VERSION_KEY)));

    private final SpanExporter delegate;

    SplunkSpanDataModifier(SpanExporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        return delegate.export(spans.stream().map(this::modify).collect(Collectors.toList()));
    }

    private SpanData modify(SpanData original) {
        List<EventData> modifiedEvents = new ArrayList<>(original.getEvents().size());
        AttributesBuilder modifiedAttributes = original.getAttributes().toBuilder();

        // zipkin eats the event attributes that are recorded by default, so we need to convert
        // the exception event to span attributes
        for (EventData event : original.getEvents()) {
            if (event.getName().equals(SemanticAttributes.EXCEPTION_EVENT_NAME)) {
                modifiedAttributes.putAll(extractExceptionAttributes(event));
            } else {
                // if it's not an exception, leave the event as it is
                modifiedEvents.add(event);
            }
        }

        // set this custom attribute in order to let the CustomZipkinEncoder use it for the span
        // name on the wire.
        modifiedAttributes.put(SPLUNK_OPERATION_KEY, original.getName());

        // zipkin does not have resource attributes, we'll need to copy them to span level
        for (AttributeKey<String> key : resourceAttributesToCopy) {
            String value = original.getResource().getAttribute(key);
            if (value != null) {
                modifiedAttributes.put(key, value);
            }
        }

        return new SplunkSpan(original, modifiedEvents, modifiedAttributes.build());
    }

    private static Attributes extractExceptionAttributes(EventData event) {
        String type = event.getAttributes().get(EXCEPTION_TYPE);
        String message = event.getAttributes().get(EXCEPTION_MESSAGE);
        String stacktrace = event.getAttributes().get(EXCEPTION_STACKTRACE);

        AttributesBuilder builder = Attributes.builder();
        if (type != null) {
            int dot = type.lastIndexOf('.');
            String simpleType = dot == -1 ? type : type.substring(dot + 1);
            builder.put(EXCEPTION_TYPE, simpleType);
            // this attribute's here to support the RUM UI/backend until it can be updated to use
            // otel conventions.
            builder.put(ERROR_TYPE_KEY, simpleType);
        }
        if (message != null) {
            builder.put(EXCEPTION_MESSAGE, message);
            // this attribute's here to support the RUM UI/backend until it can be updated to use
            // otel conventions.
            builder.put(ERROR_MESSAGE_KEY, message);
        }
        if (stacktrace != null) {
            builder.put(EXCEPTION_STACKTRACE, stacktrace);
        }
        return builder.build();
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    private static final class SplunkSpan extends DelegatingSpanData {

        private final List<EventData> modifiedEvents;
        private final Attributes modifiedAttributes;

        private SplunkSpan(
                SpanData delegate, List<EventData> modifiedEvents, Attributes modifiedAttributes) {
            super(delegate);
            this.modifiedEvents = modifiedEvents;
            this.modifiedAttributes = modifiedAttributes;
        }

        @Override
        public List<EventData> getEvents() {
            return modifiedEvents;
        }

        @Override
        public int getTotalRecordedEvents() {
            return modifiedEvents.size();
        }

        @Override
        public Attributes getAttributes() {
            return modifiedAttributes;
        }

        @Override
        public int getTotalAttributeCount() {
            return modifiedAttributes.size();
        }
    }
}
