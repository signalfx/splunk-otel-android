package com.splunk.rum;

import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.trace.data.SpanData;

public interface BacklogProvider {
    void addAll(Collection<SpanData> spans);

    void addFailedSpansToBacklog(List<SpanData> toExport);

    List<SpanData> drain();

    boolean isEmpty();

    void clear();
}
