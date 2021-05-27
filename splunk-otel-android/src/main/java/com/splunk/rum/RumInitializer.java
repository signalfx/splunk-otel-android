package com.splunk.rum;

import android.app.Application;

import com.splunk.android.rum.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.internal.SystemClock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

class RumInitializer {

    private final Config config;
    private final Application application;

    RumInitializer(Config config, Application application) {
        this.config = config;
        this.application = application;
    }

    SplunkRum initialize() {
        String rumVersion = detectRumVersion();
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

        Clock clock = SystemClock.getInstance();
        long startTimeNanos = clock.now();
        List<RumInitializer.InitializationEvent> initializationEvents = new ArrayList<>();

        SpanExporter zipkinExporter = buildExporter();
        initializationEvents.add(new RumInitializer.InitializationEvent("exporterInitialized", clock.now()));

        SessionId sessionId = new SessionId();
        initializationEvents.add(new RumInitializer.InitializationEvent("sessionIdInitialized", clock.now()));

        SdkTracerProvider sdkTracerProvider = buildTracerProvider(clock, zipkinExporter, sessionId, rumVersion, visibleScreenTracker);
        initializationEvents.add(new RumInitializer.InitializationEvent("tracerProviderInitialized", clock.now()));

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).build();
        initializationEvents.add(new RumInitializer.InitializationEvent("openTelemetrySdkInitialized", clock.now()));

        Tracer tracer = openTelemetrySdk.getTracer("SplunkRum");
        application.registerActivityLifecycleCallbacks(new RumLifecycleCallbacks(tracer, visibleScreenTracker));
        initializationEvents.add(new RumInitializer.InitializationEvent("activityLifecycleCallbacksInitialized", clock.now()));

        if (config.isCrashReportingEnabled()) {
            CrashReporter.initializeCrashReporting(tracer, openTelemetrySdk);
            initializationEvents.add(new RumInitializer.InitializationEvent("crashReportingInitialized", clock.now()));
        }

        recordInitializationSpan(startTimeNanos, initializationEvents, tracer);

        return new SplunkRum(config, openTelemetrySdk, sessionId);
    }

    private String detectRumVersion() {
        try {
            //todo: figure out if there's a way to get access to resources from pure non-UI library code.
            return application.getApplicationContext().getResources().getString(R.string.rum_version);
        } catch (Exception e) {
            //ignore for now
        }
        return "unknown";
    }

    private static void recordInitializationSpan(long startTimeNanos, List<InitializationEvent> initializationEvents, Tracer tracer) {
        Span span = tracer.spanBuilder("SplunkRum.initialize")
                .setStartTimestamp(startTimeNanos, TimeUnit.NANOSECONDS)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART)
                .startSpan();
        for (RumInitializer.InitializationEvent initializationEvent : initializationEvents) {
            span.addEvent(initializationEvent.name, initializationEvent.time, TimeUnit.NANOSECONDS);
        }
        span.end();
    }

    private SdkTracerProvider buildTracerProvider(Clock clock, SpanExporter zipkinExporter, SessionId sessionId, String rumVersion, VisibleScreenTracker visibleScreenTracker) {
        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder()
                .setClock(clock)
                .addSpanProcessor(BatchSpanProcessor.builder(zipkinExporter).build())
                .addSpanProcessor(new RumAttributeAppender(config, sessionId, rumVersion, visibleScreenTracker))
                .setResource(Resource.getDefault().toBuilder().put("service.name", config.getApplicationName()).build());
        if (config.isDebugEnabled()) {
            tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()));
        }
        return tracerProviderBuilder
                .build();
    }

    SpanExporter buildExporter() {
        String endpoint = config.getBeaconUrl() + "?auth=" + config.getRumAuthToken();
        return ZipkinSpanExporter.builder().setEndpoint(endpoint).build();
    }

    static class InitializationEvent {
        private final String name;
        private final long time;

        private InitializationEvent(String name, long time) {
            this.name = name;
            this.time = time;
        }
    }
}
