package com.splunk.rum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.sdk.trace.data.SpanData;

public class StartTypeAwareBacklogProvider implements MemorySpanBuffer {

    private final VisibleScreenTracker visibleScreenTracker;

    private final Queue<SpanData> backlog = new ArrayDeque<>();

    /**
     * @backgroundSpanBacklog will never get sent if last visible screen is null until process kill
     */
    private final Queue<SpanData> backgroundSpanBacklog = new ArrayDeque<>();

    public StartTypeAwareBacklogProvider(VisibleScreenTracker visibleScreenTracker) {
        this.visibleScreenTracker = visibleScreenTracker;
    }
    @Override
    public void addAll(Collection<SpanData> spans) {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() == null){
            backgroundSpanBacklog.addAll(spans);
        } else {
            backlog.addAll(spans);
        }
    }

    @Override
    public void addFailedSpansToBacklog(SpanData spanData) {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() == null){
            backgroundSpanBacklog.add(spanData);
        } else {
            backlog.add(spanData);
        }
    }

    @Override
    public List<SpanData> drain() {
        List<SpanData> retries = new ArrayList<>(backlog);
        backlog.clear();
        drainBackgroundBacklogIfAppIsForeground(retries);
        return retries;
    }

    private void drainBackgroundBacklogIfAppIsForeground(List<SpanData> retries) {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() != null){
            retries.addAll(backgroundSpanBacklog);
            backgroundSpanBacklog.clear();
        }
    }

    @Override
    public boolean isEmpty() {
        return backlog.isEmpty() && backgroundSpanBacklog.isEmpty();
    }

    @Override
    public void clear() {
        backlog.clear();
        backgroundSpanBacklog.clear();
    }

    @Override
    public int size() {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() == null){
            return backgroundSpanBacklog.size();
        } else {
            return backlog.size();
        }
    }
}
