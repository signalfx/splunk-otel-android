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

import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

public class StartTypeAwareMemorySpanBuffer implements MemorySpanBuffer {

    private final VisibleScreenService visibleScreenTracker;

    private final Queue<SpanData> backlog = new ArrayDeque<>();

    /**
     * backgroundSpanBacklog will never get sent if last visible screen is null until process kill.
     */
    private final Queue<SpanData> backgroundSpanBacklog = new ArrayDeque<>();

    public StartTypeAwareMemorySpanBuffer(VisibleScreenService visibleScreenTracker) {
        this.visibleScreenTracker = visibleScreenTracker;
    }

    @Override
    public void addAll(Collection<SpanData> spans) {
        if (!isAppForeground()) {
            backgroundSpanBacklog.addAll(spans);
        } else {
            backlog.addAll(backgroundSpanBacklog);
            backgroundSpanBacklog.clear();
            backlog.addAll(spans);
        }
    }

    @Override
    public void addFailedSpansToBacklog(SpanData spanData) {
        if (!isAppForeground()) {
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
        if (isAppForeground()) {
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
        if (!isAppForeground()) {
            return backgroundSpanBacklog.size();
        } else {
            return backlog.size();
        }
    }

    private boolean isAppForeground() {
        return (visibleScreenTracker.getCurrentlyVisibleScreen() != null
                        && !visibleScreenTracker.getCurrentlyVisibleScreen().equals("unknown"))
                || visibleScreenTracker.getPreviouslyVisibleScreen() != null;
    }
}
