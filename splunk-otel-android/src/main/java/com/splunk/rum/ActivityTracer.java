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

import android.app.Activity;

import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

class ActivityTracer implements TrackableTracer {
    static final AttributeKey<String> ACTIVITY_NAME_KEY = AttributeKey.stringKey("activityName");
    static final String APP_START_SPAN_NAME = "AppStart";

    private final AtomicReference<String> initialAppActivity;
    private final Tracer tracer;
    private final String activityName;
    private final VisibleScreenTracker visibleScreenTracker;
    private final AppStartupTimer appStartupTimer;

    private Span span;
    private Scope scope;

    ActivityTracer(Activity activity, AtomicReference<String> initialAppActivity, Tracer tracer, VisibleScreenTracker visibleScreenTracker, AppStartupTimer appStartupTimer) {
        this.initialAppActivity = initialAppActivity;
        this.tracer = tracer;
        this.activityName = activity.getClass().getSimpleName();
        this.visibleScreenTracker = visibleScreenTracker;
        this.appStartupTimer = appStartupTimer;
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
        //If the application has never loaded an activity, or this is the initial activity getting re-created,
        // we name this span specially to show that it's the application starting up. Otherwise, use
        // the activity class name as the base of the span name.
        final boolean isColdStart = initialAppActivity.get() == null;
        if (isColdStart && appStartupTimer != null) {
            startSpan("Created", appStartupTimer.getStartupSpan());
        } else if (activityName.equals(initialAppActivity.get())) {
            Span span = startSpan(APP_START_SPAN_NAME);
            span.setAttribute(SplunkRum.START_TYPE_KEY, "warm");
            //override the component to be appstart
            span.setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART);
        } else {
            startSpan("Created");
        }
        return this;
    }

    @Override
    public TrackableTracer initiateRestartSpanIfNecessary(boolean multiActivityApp) {
        if (span != null) {
            return this;
        }
        //restarting the first activity is a "hot" AppStart
        //Note: in a multi-activity application, navigating back to the first activity can trigger
        //this, so it would not be ideal to call it an AppStart.
        if (!multiActivityApp && activityName.equals(initialAppActivity.get())) {
            Span span = startSpan(APP_START_SPAN_NAME);
            span.setAttribute(SplunkRum.START_TYPE_KEY, "hot");
            //override the component to be appstart
            span.setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART);
        } else {
            startSpan("Restarted");
        }
        return this;
    }

    private Span startSpan(String spanName) {
        return startSpan(spanName, null);
    }

    private Span startSpan(String spanName, Span parentSpan) {
        final SpanBuilder spanBuilder = tracer.spanBuilder(spanName)
                .setAttribute(ACTIVITY_NAME_KEY, activityName)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_UI);
        if (parentSpan != null) {
            spanBuilder.setParent(parentSpan.storeInContext(Context.current()));
        }
        span = spanBuilder
                .startSpan();
        //do this after the span is started, so we can override the default screen.name set by the RumAttributeAppender.
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, activityName);
        scope = span.makeCurrent();
        return span;
    }

    @Override
    public void endSpanForTrackableResumed() {
        if (initialAppActivity.get() == null) {
            initialAppActivity.set(activityName);
        }
        endActiveSpan();
    }

    @Override
    public void endActiveSpan() {
        if (appStartupTimer != null) {
            appStartupTimer.end();
        }
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
        if (!activityName.equals(previouslyVisibleScreen)) {
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
