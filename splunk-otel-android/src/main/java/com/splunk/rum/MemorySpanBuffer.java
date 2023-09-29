package com.splunk.rum;

import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.trace.data.SpanData;

interface MemorySpanBuffer {
    void addAll(Collection<SpanData> spans);

    void addFailedSpansToBacklog(SpanData spanData);

    List<SpanData> drain();

    boolean isEmpty();

    void clear();

    int size();
}
