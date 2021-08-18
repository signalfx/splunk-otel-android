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

import androidx.fragment.app.Fragment;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

class FragmentTracer implements TrackableTracer {
    static final AttributeKey<String> FRAGMENT_NAME_KEY = AttributeKey.stringKey("fragmentName");

    private final Tracer tracer;
    private final String fragmentName;
    private final VisibleScreenTracker visibleScreenTracker;

    private Span span;
    private Scope scope;

    FragmentTracer(Fragment fragment, Tracer tracer, VisibleScreenTracker visibleScreenTracker) {
        this.tracer = tracer;
        this.fragmentName = fragment.getClass().getSimpleName();
        this.visibleScreenTracker = visibleScreenTracker;
    }

    @Override
    public TrackableTracer startSpanIfNoneInProgress(String action) {
        if (span != null) {
            return this;
        }
        startSpan(action);
        return this;
    }

    @Override
    public TrackableTracer startTrackableCreation() {
        startSpan("Created");
        return this;
    }

    @Override
    public TrackableTracer initiateRestartSpanIfNecessary(boolean multiActivityApp) {
        if (span != null) {
            return this;
        }
        startSpan("Restarted");
        return this;
    }

    private Span startSpan(String spanName) {
        span = tracer.spanBuilder(spanName)
                .setAttribute(FRAGMENT_NAME_KEY, fragmentName)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_UI).startSpan();
        //do this after the span is started, so we can override the default screen.name set by the RumAttributeAppender.
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, fragmentName);
        scope = span.makeCurrent();
        return span;
    }

    @Override
    public void endActiveSpan() {
        if (scope != null) {
            scope.close();
            scope = null;
        }
        if (this.span != null) {
            this.span.end();
            this.span = null;
        }
    }

    @Override
    public TrackableTracer addPreviousScreenAttribute() {
        String previouslyVisibleScreen = visibleScreenTracker.getPreviouslyVisibleScreen();
        if (!fragmentName.equals(previouslyVisibleScreen)) {
            span.setAttribute(SplunkRum.LAST_SCREEN_NAME_KEY, previouslyVisibleScreen);
        }
        return this;
    }

    @Override
    public TrackableTracer addEvent(String eventName) {
        if (span != null) {
            span.addEvent(eventName);
        }
        return this;
    }
}
