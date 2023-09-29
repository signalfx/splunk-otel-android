package com.splunk.rum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.sdk.trace.data.SpanData;

public class StartTypeAwareBacklogProvider implements BacklogProvider{

    private static final int MAX_BACKLOG_SIZE = 100;
    private final VisibleScreenTracker visibleScreenTracker;

    private final Queue<SpanData> backlog = new ArrayDeque<>(MAX_BACKLOG_SIZE);

    /**
     * @backgroundSpanBacklog will never get sent if last visible screen is null until process kill
     */
    private final Queue<SpanData> backgroundSpanBacklog = new ArrayDeque<>(MAX_BACKLOG_SIZE);

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
    public void addFailedSpansToBacklog(List<SpanData> toExport) {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() == null){
            addFailedSpan(toExport, backgroundSpanBacklog);
        } else {
            addFailedSpan(toExport, backlog);
        }
    }

    private void addFailedSpan(List<SpanData> toExport, Queue<SpanData> backlog) {
        for (SpanData spanData : toExport) {
            if (backlog.size() < MAX_BACKLOG_SIZE) {
                backlog.add(spanData);
            }
        }
    }

    @Override
    public List<SpanData> fillFromBacklog() {
        List<SpanData> retries = new ArrayList<>(backlog);
        backlog.clear();

        if (visibleScreenTracker.getPreviouslyVisibleScreen() != null){
            retries.addAll(backgroundSpanBacklog);
            backgroundSpanBacklog.clear();
        }
        return retries;
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
}
