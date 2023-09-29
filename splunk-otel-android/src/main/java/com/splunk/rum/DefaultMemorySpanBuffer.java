package com.splunk.rum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import io.opentelemetry.sdk.trace.data.SpanData;

public class DefaultMemorySpanBuffer implements MemorySpanBuffer {

    // note: no need to make this queue thread-safe since it will only ever be called from the
    // BatchSpanProcessor worker thread.
    private final Queue<SpanData> backlog = new ArrayDeque<>();
    @Override
    public void addAll(Collection<SpanData> spans) {
        backlog.addAll(spans);
    }

    @Override
    public void addFailedSpansToBacklog(SpanData spanData) {
        backlog.add(spanData);
    }

    @Override
    public List<SpanData> drain() {
        List<SpanData> retries = new ArrayList<>(backlog);
        backlog.clear();
        return retries;
    }

    @Override
    public boolean isEmpty() {
        return backlog.isEmpty();
    }

    @Override
    public void clear() {
        backlog.clear();
    }

    @Override
    public int size() {
        return backlog.size();
    }
}

