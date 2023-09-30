package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.sdk.trace.data.SpanData;

public class StartTypeAwareMemorySpanBufferTest {

    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

    private final StartTypeAwareMemorySpanBuffer memorySpanBuffer = new StartTypeAwareMemorySpanBuffer(
            visibleScreenTracker
    );

    @Test
    void fillFromBacklog_givenInBackground_shouldReturnForegroundSpansOnly(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        memorySpanBuffer.addAll(spans);

        //0 foreground spans since it's background from the start
        assertEquals(0, memorySpanBuffer.drain().size());
    }

    @Test
    void fillFromBacklog_givenPreviouslyInBackgroundThenMoveToForeground_shouldAddBackgroundSpansToBacklog(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null, "MainActivity");

        memorySpanBuffer.addAll(spans);
        memorySpanBuffer.addAll(spans);

        assertEquals(20, memorySpanBuffer.drain().size());
    }

    @Test
    void addFailedSpansToBacklog_givenInBackground_shouldAddFailedSpanToBackgroundBacklog(){
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        memorySpanBuffer.addFailedSpansToBacklog(mock(SpanData.class));

        assertEquals(0, memorySpanBuffer.drain().size());
    }

    @Test
    void addFailedSpansToBacklog_givenInForeground_shouldAddFailedSpanToBacklog(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null, "MainActivity");

        spans.forEach(memorySpanBuffer::addFailedSpansToBacklog);

        assertEquals(10, memorySpanBuffer.drain().size());
    }
}