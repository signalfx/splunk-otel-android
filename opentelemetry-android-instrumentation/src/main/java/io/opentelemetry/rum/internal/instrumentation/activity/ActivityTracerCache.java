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

package io.opentelemetry.rum.internal.instrumentation.activity;

import android.app.Activity;
import androidx.annotation.VisibleForTesting;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.rum.internal.instrumentation.ScreenNameExtractor;
import io.opentelemetry.rum.internal.instrumentation.startup.AppStartupTimer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Encapsulates the fact that we have an ActivityTracer instance per Activity class, and provides
 * convenience methods for adding events and starting spans.
 */
public class ActivityTracerCache {

    private final Map<String, ActivityTracer> tracersByActivityClassName = new HashMap<>();

    private final Function<Activity, ActivityTracer> tracerFactory;

    public ActivityTracerCache(
            Tracer tracer,
            VisibleScreenTracker visibleScreenTracker,
            AppStartupTimer startupTimer,
            ScreenNameExtractor screenNameExtractor) {
        this(
                tracer,
                visibleScreenTracker,
                new AtomicReference<>(),
                startupTimer,
                screenNameExtractor);
    }

    @VisibleForTesting
    ActivityTracerCache(
            Tracer tracer,
            VisibleScreenTracker visibleScreenTracker,
            AtomicReference<String> initialAppActivity,
            AppStartupTimer startupTimer,
            ScreenNameExtractor screenNameExtractor) {
        this(
                activity ->
                        ActivityTracer.builder(activity)
                                .setScreenName(screenNameExtractor.extract(activity))
                                .setInitialAppActivity(initialAppActivity)
                                .setTracer(tracer)
                                .setAppStartupTimer(startupTimer)
                                .setVisibleScreenTracker(visibleScreenTracker)
                                .build());
    }

    @VisibleForTesting
    ActivityTracerCache(Function<Activity, ActivityTracer> tracerFactory) {
        this.tracerFactory = tracerFactory;
    }

    public ActivityTracer addEvent(Activity activity, String eventName) {
        return getTracer(activity).addEvent(eventName);
    }

    public ActivityTracer startSpanIfNoneInProgress(Activity activity, String spanName) {
        return getTracer(activity).startSpanIfNoneInProgress(spanName);
    }

    public ActivityTracer initiateRestartSpanIfNecessary(Activity activity) {
        boolean isMultiActivityApp = tracersByActivityClassName.size() > 1;
        return getTracer(activity).initiateRestartSpanIfNecessary(isMultiActivityApp);
    }

    public ActivityTracer startActivityCreation(Activity activity) {
        return getTracer(activity).startActivityCreation();
    }

    private ActivityTracer getTracer(Activity activity) {
        ActivityTracer activityTracer =
                tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            activityTracer = tracerFactory.apply(activity);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }
}
