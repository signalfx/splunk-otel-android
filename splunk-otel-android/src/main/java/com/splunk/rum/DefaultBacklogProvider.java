package com.splunk.rum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import io.opentelemetry.sdk.trace.data.SpanData;

public class DefaultBacklogProvider implements BacklogProvider {

    private static final int MAX_BACKLOG_SIZE = 100;
    // note: no need to make this queue thread-safe since it will only ever be called from the
    // BatchSpanProcessor worker thread.
    private final Queue<SpanData> backlog = new ArrayDeque<>(MAX_BACKLOG_SIZE);
    @Override
    public void addAll(Collection<SpanData> spans) {
        backlog.addAll(spans);
    }

    @Override
    public void addFailedSpansToBacklog(List<SpanData> toExport) {
        for (SpanData spanData : toExport) {
            if (backlog.size() < MAX_BACKLOG_SIZE) {
                backlog.add(spanData);
            }
        }
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
}

