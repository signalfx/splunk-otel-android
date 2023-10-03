/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StartTypeAwareMemorySpanBufferTest {

    private final VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);

    private final StartTypeAwareMemorySpanBuffer memorySpanBuffer =
            new StartTypeAwareMemorySpanBuffer(visibleScreenTracker);

    @Test
    void fillFromBacklog_givenInBackground_shouldReturnForegroundSpansOnly() {
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        memorySpanBuffer.addAll(spans);

        // 0 foreground spans since it's background from the start
        assertEquals(0, memorySpanBuffer.drain().size());
    }

    @Test
    void
            fillFromBacklog_givenPreviouslyInBackgroundThenMoveToForeground_shouldAddBackgroundSpansToBacklog() {
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
    void addFailedSpansToBacklog_givenInBackground_shouldAddFailedSpanToBackgroundBacklog() {
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        memorySpanBuffer.addFailedSpansToBacklog(mock(SpanData.class));

        assertEquals(0, memorySpanBuffer.drain().size());
    }

    @Test
    void addFailedSpansToBacklog_givenInForeground_shouldAddFailedSpanToBacklog() {
        List<SpanData> spans = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            spans.add(mock(SpanData.class));
        }
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null, "MainActivity");

        spans.forEach(memorySpanBuffer::addFailedSpansToBacklog);

        assertEquals(10, memorySpanBuffer.drain().size());
    }
}
