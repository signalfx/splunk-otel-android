package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.sdk.trace.data.SpanData;

class DefaultMemorySpanBufferTest {

    DefaultMemorySpanBuffer backlogProvider = new DefaultMemorySpanBuffer();

    @Test
    void addFailedSpansToBacklog_givenSpansMoreThanMax_shouldKeepLastMaxSpan() {
        List<SpanData> firstSet = new ArrayList<>();
        for (int i = 0; i < 110; i++) {
            firstSet.add(mock(SpanData.class));
        }
        List<SpanData> secondSet = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            secondSet.add(mock(SpanData.class));
        }

        firstSet.forEach(backlogProvider::addFailedSpansToBacklog);
        secondSet.forEach(backlogProvider::addFailedSpansToBacklog);

        assertEquals(100, backlogProvider.drain().size());
    }

    @Test
    void fillFromBacklog_shouldEmptiesBacklog(){
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            spans.add(mock(SpanData.class));
        }

        backlogProvider.addAll(spans);
        backlogProvider.drain();

        assertTrue(backlogProvider.isEmpty());
    }
}