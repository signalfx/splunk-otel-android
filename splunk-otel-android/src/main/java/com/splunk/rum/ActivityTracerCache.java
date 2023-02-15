package com.splunk.rum;

import android.app.Activity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.Tracer;

public class ActivityTracerCache {

    private final Map<String, ActivityTracer> tracersByActivityClassName = new HashMap<>();
    private final AtomicReference<String> initialAppActivity = new AtomicReference<>();
    private final Tracer tracer;
    private final VisibleScreenTracker visibleScreenTracker;
    private final AppStartupTimer startupTimer;

    public ActivityTracerCache(Tracer tracer, VisibleScreenTracker visibleScreenTracker, AppStartupTimer startupTimer) {
        this.tracer = tracer;
        this.visibleScreenTracker = visibleScreenTracker;
        this.startupTimer = startupTimer;
    }

    public ActivityTracer addEvent(Activity activity, String eventName) {
        return getTracer(activity).addEvent(eventName);
    }

    public ActivityTracer startSpanIfNoneInProgress(Activity activity, String spanName){
        ActivityTracer tracer = getTracer(activity);
        return tracer.startSpanIfNoneInProgress(spanName);
    }

    public ActivityTracer initiateRestartSpanIfNecessary(Activity activity){
        boolean isMultiActivityApp = tracersByActivityClassName.size() > 1;
        return getTracer(activity).initiateRestartSpanIfNecessary(isMultiActivityApp);
    }

    public ActivityTracer startActivityCreation(Activity activity){
        return getTracer(activity).startActivityCreation();
    }

    ActivityTracer getTracer(Activity activity) {
        ActivityTracer activityTracer =
                tracersByActivityClassName.get(activity.getClass().getName());
        if (activityTracer == null) {
            activityTracer =
                    new ActivityTracer(
                            activity,
                            initialAppActivity,
                            tracer,
                            visibleScreenTracker,
                            startupTimer);
            tracersByActivityClassName.put(activity.getClass().getName(), activityTracer);
        }
        return activityTracer;
    }
}
