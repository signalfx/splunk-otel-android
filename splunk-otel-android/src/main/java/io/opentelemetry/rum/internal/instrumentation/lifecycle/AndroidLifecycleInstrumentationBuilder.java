package io.opentelemetry.rum.internal.instrumentation.lifecycle;

import java.util.function.Function;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.rum.internal.instrumentation.startup.AppStartupTimer;

public class AndroidLifecycleInstrumentationBuilder {
    AppStartupTimer startupTimer;
    VisibleScreenTracker visibleScreenTracker;
    Function<Tracer, Tracer> tracerCustomizer = Function.identity();

    public AndroidLifecycleInstrumentationBuilder setStartupTimer(AppStartupTimer timer) {
        this.startupTimer = timer;
        return this;
    }

    public AndroidLifecycleInstrumentationBuilder setVisibleScreenTracker(VisibleScreenTracker tracker) {
        this.visibleScreenTracker = tracker;
        return this;
    }

    public AndroidLifecycleInstrumentationBuilder setTracerCustomizer(Function<Tracer, Tracer> customizer) {
        this.tracerCustomizer = customizer;
        return this;
    }

    public AndroidLifecycleInstrumentation build() {
        return new AndroidLifecycleInstrumentation(this);
    }
}
