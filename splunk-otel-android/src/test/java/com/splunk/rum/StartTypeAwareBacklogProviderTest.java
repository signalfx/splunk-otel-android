package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
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
        assertEquals(0, backlogProvider.fillFromBacklog().size());
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

        assertEquals(20, backlogProvider.fillFromBacklog().size());
    }

    @Test
    void addFailedSpansToBacklog_givenInBackground_shouldAddFailedSpanToBackgroundBacklog(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        backlogProvider.addFailedSpansToBacklog(spans);

        assertEquals(0, backlogProvider.fillFromBacklog().size());
    }

    @Test
    void addFailedSpansToBacklog_givenInForeground_shouldAddFailedSpanToBacklog(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("MainActivity", null);

        backlogProvider.addFailedSpansToBacklog(spans);

        assertEquals(10, backlogProvider.fillFromBacklog().size());
    }

    @Test
    void addFailedSpansToBacklog_givenSpanExceedMax_shouldKeeplastMaxSpan(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn("MainActivity");

        backlogProvider.addFailedSpansToBacklog(spans);

        assertEquals(100, backlogProvider.fillFromBacklog().size());
    }
}