package io.opentelemetry.rum.internal.instrumentation.lifecycle;

import android.app.Application;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.function.Function;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
import io.opentelemetry.rum.internal.instrumentation.activity.ActivityCallbacks;
import io.opentelemetry.rum.internal.instrumentation.activity.ActivityTracerCache;
import io.opentelemetry.rum.internal.instrumentation.activity.Pre29ActivityCallbacks;
import io.opentelemetry.rum.internal.instrumentation.activity.Pre29VisibleScreenLifecycleBinding;
import io.opentelemetry.rum.internal.instrumentation.activity.RumFragmentActivityRegisterer;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenLifecycleBinding;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.rum.internal.instrumentation.fragment.RumFragmentLifecycleCallbacks;
import io.opentelemetry.rum.internal.instrumentation.startup.AppStartupTimer;

/**
 * This is an umbrella instrumentation that covers several things:
 * * startup timer callback is registered so that UI startup time can be measured
 * - activity lifecycle callbacks are registered so that lifecycle events can be generated
 * - activity lifecycle callback listener is registered to that will register a FragmentLifecycleCallbacks when appropriate
 * - activity lifecycle callback listener is registered to dispatch events to the VisibleScreenTracker
 */
public class AndroidLifecycleInstrumentation {

    private static final String INSTRUMENTATION_SCOPE = "io.opentelemetry.lifecycle";
    private final AppStartupTimer startupTimer;
    private final VisibleScreenTracker visibleScreenTracker;

    private final Function<Tracer,Tracer> tracerCustomizer;

    private AndroidLifecycleInstrumentation(Builder builder) {
        this.startupTimer = builder.startupTimer;
        this.visibleScreenTracker = builder.visibleScreenTracker;
        this.tracerCustomizer = builder.tracerCustomizer;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void installOn(InstrumentedApplication app){
        installStartupTimerInstrumentation(app);
        installActivityLifecycleEventsInstrumentation(app);
        installFragmentLifecycleInstrumentation(app);
        installScreenTrackingInstrumentation(app);
    }

    private void installStartupTimerInstrumentation(InstrumentedApplication app) {
        app.getApplication()
                .registerActivityLifecycleCallbacks(
                        startupTimer.createLifecycleCallback());
    }

    private void installActivityLifecycleEventsInstrumentation(InstrumentedApplication app){
            Application.ActivityLifecycleCallbacks activityCallbacks =
                    buildActivityEventsCallback(app);
            app.getApplication().registerActivityLifecycleCallbacks(activityCallbacks);
    }

    @NonNull
    private Application.ActivityLifecycleCallbacks buildActivityEventsCallback(InstrumentedApplication instrumentedApp) {
        Tracer delegateTracer = instrumentedApp.getOpenTelemetrySdk().getTracer(INSTRUMENTATION_SCOPE);
        Tracer tracer = tracerCustomizer.apply(delegateTracer);

        ActivityTracerCache tracers =
                new ActivityTracerCache(tracer, visibleScreenTracker, startupTimer);
        if (Build.VERSION.SDK_INT < 29) {
            return new Pre29ActivityCallbacks(tracers);
        }
        return new ActivityCallbacks(tracers);
    }

    private void installFragmentLifecycleInstrumentation(InstrumentedApplication app) {
        Application.ActivityLifecycleCallbacks fragmentRegisterer = buildFragmentRegisterer(app);
        app.getApplication()
                .registerActivityLifecycleCallbacks(fragmentRegisterer);
    }

    @NonNull
    private Application.ActivityLifecycleCallbacks buildFragmentRegisterer(InstrumentedApplication app) {

        Tracer delegateTracer = app.getOpenTelemetrySdk().getTracer(INSTRUMENTATION_SCOPE);
        Tracer tracer = tracerCustomizer.apply(delegateTracer);
        RumFragmentLifecycleCallbacks fragmentLifecycle =
                new RumFragmentLifecycleCallbacks(tracer, visibleScreenTracker);
        if (Build.VERSION.SDK_INT < 29) {
            return RumFragmentActivityRegisterer.createPre29(fragmentLifecycle);
        }
        return RumFragmentActivityRegisterer.create(fragmentLifecycle);
    }

    private void installScreenTrackingInstrumentation(InstrumentedApplication app) {
        Application.ActivityLifecycleCallbacks screenTrackingBinding =
                buildScreenTrackingBinding(visibleScreenTracker);
        app.getApplication()
                .registerActivityLifecycleCallbacks(screenTrackingBinding);
    }

    @NonNull
    private Application.ActivityLifecycleCallbacks buildScreenTrackingBinding(
            VisibleScreenTracker visibleScreenTracker) {
        if (Build.VERSION.SDK_INT < 29) {
            return new Pre29VisibleScreenLifecycleBinding(visibleScreenTracker);
        }
        return new VisibleScreenLifecycleBinding(visibleScreenTracker);
    }

    public static class Builder {
        private AppStartupTimer startupTimer;
        private VisibleScreenTracker visibleScreenTracker;
        private Function<Tracer, Tracer> tracerCustomizer = Function.identity();

        public Builder setStartupTimer(AppStartupTimer timer) {
            this.startupTimer = timer;
            return this;
        }

        public Builder setVisibleScreenTracker(VisibleScreenTracker tracker) {
            this.visibleScreenTracker = tracker;
            return this;
        }

        public Builder setTracerCustomizer(Function<Tracer, Tracer> customizer) {
            this.tracerCustomizer = customizer;
            return this;
        }

        public AndroidLifecycleInstrumentation build() {
            return new AndroidLifecycleInstrumentation(this);
        }
    }

}
