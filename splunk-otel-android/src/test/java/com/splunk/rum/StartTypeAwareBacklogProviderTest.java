package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.sdk.trace.data.SpanData;

public class StartTypeAwareBacklogProviderTest {

    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

    private final StartTypeAwareBacklogProvider backlogProvider = new StartTypeAwareBacklogProvider(
            visibleScreenTracker
    );

    @Test
    void fillFromBacklog_givenInBackground_shouldReturnForegroundSpansOnly(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        backlogProvider.addAll(spans);

        //0 foreground spans since it's background from the start
        assertEquals(0, backlogProvider.drain().size());
    }

    @Test
    void fillFromBacklog_givenPreviouslyInBackgroundThenMoveToForeground_shouldAddBackgroundSpansToBacklog(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null, "MainActivity");

        backlogProvider.addAll(spans);
        backlogProvider.addAll(spans);

        assertEquals(20, backlogProvider.drain().size());
    }

    @Test
    void addFailedSpansToBacklog_givenInBackground_shouldAddFailedSpanToBackgroundBacklog(){
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        backlogProvider.addFailedSpansToBacklog(mock(SpanData.class));

        assertEquals(0, backlogProvider.drain().size());
    }

    @Test
    void addFailedSpansToBacklog_givenInForeground_shouldAddFailedSpanToBacklog(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("MainActivity", null);

        spans.forEach(backlogProvider::addFailedSpansToBacklog);

        assertEquals(10, backlogProvider.drain().size());
    }

    @Test
    void addFailedSpansToBacklog_givenSpanExceedMax_shouldKeeplastMaxSpan(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("MainActivity");

        spans.forEach(backlogProvider::addFailedSpansToBacklog);

        assertEquals(100, backlogProvider.drain().size());
    }
}