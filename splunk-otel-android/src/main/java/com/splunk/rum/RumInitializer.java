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

import static com.splunk.rum.SplunkRum.APPLICATION_ID_KEY;
import static com.splunk.rum.SplunkRum.APP_NAME_KEY;
import static com.splunk.rum.SplunkRum.APP_VERSION_CODE_KEY;
import static com.splunk.rum.SplunkRum.COMPONENT_APPSTART;
import static com.splunk.rum.SplunkRum.COMPONENT_ERROR;
import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static com.splunk.rum.SplunkRum.COMPONENT_UI;
import static com.splunk.rum.SplunkRum.LOG_TAG;
import static com.splunk.rum.SplunkRum.RUM_TRACER_NAME;
import static com.splunk.rum.SplunkRum.SPLUNK_OLLY_UUID_KEY;
import static io.opentelemetry.android.common.RumConstants.APP_START_SPAN_NAME;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT;
import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.splunk.rum.internal.GlobalAttributesSupplier;
import com.splunk.rum.internal.UInt32QuadXorTraceIdRatioSampler;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.OpenTelemetryRumBuilder;
import io.opentelemetry.android.RuntimeDetailsExtractor;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration;
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader;
import io.opentelemetry.android.instrumentation.activity.ActivityLifecycleInstrumentation;
import io.opentelemetry.android.instrumentation.activity.startup.AppStartupTimer;
import io.opentelemetry.android.instrumentation.anr.AnrInstrumentation;
import io.opentelemetry.android.instrumentation.crash.CrashReporterInstrumentation;
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

class RumInitializer {

    // we're setting a fairly large length limit to capture long stack traces; ~256 lines,
    // assuming 128 chars per line
    static final int MAX_ATTRIBUTE_LENGTH = 256 * 128;

    private final SplunkRumBuilder builder;
    private final Application application;
    private final AppStartupTimer startupTimer;
    private final InitializationEvents initializationEvents;

    RumInitializer(
            SplunkRumBuilder builder, Application application, AppStartupTimer startupTimer) {
        this.builder = builder;
        this.application = application;
        this.startupTimer = startupTimer;
        this.initializationEvents = new InitializationEvents(startupTimer);
    }

    SplunkRum initialize(Looper mainLooper) {

        initializationEvents.begin();

        OtelRumConfig config = new OtelRumConfig();
        GlobalAttributesSupplier globalAttributeSupplier =
                new GlobalAttributesSupplier(builder.globalAttributes);
        config.setGlobalAttributes(globalAttributeSupplier);

        // TODO: Note/document this instrumentation is now opt-in via application classpath via
        // build settings
        //        if (!builder.isNetworkMonitorEnabled()) {
        //            config.disableNetworkChangeMonitoring();
        //        }

        config.disableScreenAttributes();
        DiskBufferingConfiguration diskBufferingConfig =
                DiskBufferingConfiguration.builder()
                        .setEnabled(builder.isDiskBufferingEnabled())
                        .setMaxCacheSize(100_000_000)
                        .build();
        config.setDiskBufferingConfiguration(diskBufferingConfig);

        OpenTelemetryRumBuilder otelRumBuilder = OpenTelemetryRum.builder(application, config);

        otelRumBuilder.mergeResource(createSplunkResource());
        initializationEvents.emit("resourceInitialized");

        ServiceManager.initialize(application);
        ServiceManager serviceManager = ServiceManager.get();
        CurrentNetworkProvider currentNetworkProvider = serviceManager.getCurrentNetworkProvider();
        VisibleScreenService visibleScreenService = serviceManager.getVisibleScreenService();

        // TODO: now spelled rum.sdk.init.net.provider and currently mixed up in network
        // attributes enabled config in upstream
        ////        CurrentNetworkProvider currentNetworkProvider =
        ////                CurrentNetworkProvider.create(application);
        //        currentNetworkProvider.start();
        //        otelRumBuilder.setCurrentNetworkProvider(currentNetworkProvider);
        initializationEvents.emit("connectionUtilInitialized");

        // TODO: How truly important is the order of these span processors? The location of event
        // generation should probably not be altered...

        // Get exporters going...
        otelRumBuilder.addSpanExporterCustomizer(this::buildSpanExporter);
        //        otelRumBuilder.addLogRecordExporterCustomizer(xxx todo );

        //        // Add batch span processor
        //        otelRumBuilder.addTracerProviderCustomizer(
        //                (tracerProviderBuilder, app) -> {
        //                    SpanExporter zipkinExporter =
        //                            buildFilteringExporter(currentNetworkProvider,
        // visibleScreenService);
        //                    initializationEvents.emit("exporterInitialized");
        //
        //                    BatchSpanProcessor batchSpanProcessor =
        //                            BatchSpanProcessor.builder(zipkinExporter).build();
        //                    initializationEvents.emit("batchSpanProcessorInitialized");
        //                    return tracerProviderBuilder.addSpanProcessor(batchSpanProcessor);
        //                });

        // Inhibit the upstream exporter because we add our own BatchSpanProcessor
        //        otelRumBuilder.addSpanExporterCustomizer(x -> new NoOpSpanExporter());

        // Set span limits
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) ->
                        tracerProviderBuilder.setSpanLimits(
                                SpanLimits.builder()
                                        .setMaxAttributeValueLength(MAX_ATTRIBUTE_LENGTH)
                                        .build()));

        // Set up the sampler, if enabled
        // TODO: Make this better...
        // This holder is required because we cannot reasonably get the session id until after
        // OpenTelemetryRum has been created. So this is spackled into place below.
        AtomicReference<Supplier<String>> sessionSupplierHolder = new AtomicReference<>(() -> null);
        if (builder.sessionBasedSamplerEnabled) {
            otelRumBuilder.addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        Sampler sampler =
                                UInt32QuadXorTraceIdRatioSampler.create(
                                        builder.sessionBasedSamplerRatio,
                                        () -> {
                                            Supplier<String> supplier = sessionSupplierHolder.get();
                                            return supplier == null ? null : supplier.get();
                                        });
                        return tracerProviderBuilder.setSampler(sampler);
                    });
        }

        // Wire up the logging exporter, if enabled.
        if (builder.isDebugEnabled()) {
            otelRumBuilder.addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        tracerProviderBuilder.addSpanProcessor(
                                SimpleSpanProcessor.create(
                                        builder.decorateWithSpanFilter(
                                                LoggingSpanExporter.create())));
                        initializationEvents.emit("debugSpanExporterInitialized");
                        return tracerProviderBuilder;
                    });
        }

        // Add final event showing tracer provider init finished
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) -> {
                    initializationEvents.emit("tracerProviderInitialized");
                    return tracerProviderBuilder;
                });

        // install the log->span bridge
        LogToSpanBridge logBridge = new LogToSpanBridge();
        otelRumBuilder.addLoggerProviderCustomizer(
                (loggerProviderBuilder, app) ->
                        loggerProviderBuilder.addLogRecordProcessor(logBridge));
        // make sure the TracerProvider gets set as the very first thing, before any other
        // instrumentations
        otelRumBuilder.addInstrumentation(
                (app, otelRum) ->
                        logBridge.setTracerProvider(
                                otelRum.getOpenTelemetry().getTracerProvider()));

        if (builder.isAnrDetectionEnabled()) {
            configureAnrInstrumentation();
        }
        if (builder.isSlowRenderingDetectionEnabled()) {
            configureSlowRenderingInstrumentation();
        }
        if (builder.isCrashReportingEnabled()) {
            configureCrashReporter();
        }

        SettableScreenAttributesAppender screenAttributesAppender =
                new SettableScreenAttributesAppender(visibleScreenService);
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) ->
                        tracerProviderBuilder.addSpanProcessor(screenAttributesAppender));

        // Lifecycle events instrumentation are always installed.
        configureLifecycleInstrumentations();

        OpenTelemetryRum openTelemetryRum = otelRumBuilder.build();

        sessionSupplierHolder.set(openTelemetryRum::getRumSessionId);

        initializationEvents.recordInitializationSpans(
                builder.getConfigFlags(),
                openTelemetryRum.getOpenTelemetry().getTracer(RUM_TRACER_NAME));

        return new SplunkRum(openTelemetryRum, globalAttributeSupplier, screenAttributesAppender);
    }

    @NonNull
    private MemorySpanBuffer constructBacklogProvider(VisibleScreenService visibleScreenService) {
        if (builder.isBackgroundInstrumentationDeferredUntilForeground()) {
            return new StartTypeAwareMemorySpanBuffer(visibleScreenService);
        } else {
            return new DefaultMemorySpanBuffer();
        }
    }

    @NonNull
    private SpanStorage constructSpanFileProvider(VisibleScreenService visibleScreenService) {
        if (builder.isBackgroundInstrumentationDeferredUntilForeground()) {
            return StartTypeAwareSpanStorage.create(
                    visibleScreenService,
                    new FileUtils(),
                    application.getApplicationContext().getFilesDir());
        } else {
            return new DefaultSpanStorage(
                    new FileUtils(), application.getApplicationContext().getFilesDir());
        }
    }

    private void configureLifecycleInstrumentations() {
        ActivityLifecycleInstrumentation instrumentation =
                AndroidInstrumentationLoader.getInstrumentation(
                        ActivityLifecycleInstrumentation.class);
        if (instrumentation == null) {
            Log.w(
                    LOG_TAG,
                    "Activity rendering instrumentation was not loaded! Skipping configuration.");
            return;
        }
        instrumentation.setTracerCustomizer(
                tracer ->
                        spanName -> {
                            String component =
                                    spanName.equals(APP_START_SPAN_NAME)
                                            ? COMPONENT_APPSTART
                                            : COMPONENT_UI;
                            return tracer.spanBuilder(spanName)
                                    .setAttribute(COMPONENT_KEY, component);
                        });
        instrumentation.setScreenNameExtractor(SplunkScreenNameExtractor.INSTANCE);
        initializationEvents.emit("activityLifecycleCallbacksInitialized");
    }

    /**
     * Creates a minimal Splunk-specific resource. This will be blended with the upstream
     * AndroidResource.
     */
    private Resource createSplunkResource() {
        // applicationName can't be null at this stage
        String applicationName = requireNonNull(builder.applicationName);
        ResourceBuilder resourceBuilder = Resource.builder().put(APP_NAME_KEY, applicationName);
        if (builder.deploymentEnvironment != null) {
            resourceBuilder.put(DEPLOYMENT_ENVIRONMENT, builder.deploymentEnvironment);
        }
        return resourceBuilder.build();
    }

    private void configureAnrInstrumentation() {
        AnrInstrumentation instrumentation =
                AndroidInstrumentationLoader.getInstrumentation(AnrInstrumentation.class);
        if (instrumentation == null) {
            Log.w(LOG_TAG, "ANR instrumentation was not loaded! Skipping configuration.");
            return;
        }
        instrumentation.addAttributesExtractor(constant(COMPONENT_KEY, COMPONENT_ERROR));
        addErrorIdentifyingAttributes(instrumentation::addAttributesExtractor);

        initializationEvents.emit("anrMonitorInitialized");
    }

    private void configureSlowRenderingInstrumentation() {
        SlowRenderingInstrumentation instrumentation =
                AndroidInstrumentationLoader.getInstrumentation(SlowRenderingInstrumentation.class);
        if (instrumentation == null) {
            Log.w(
                    LOG_TAG,
                    "Slow rendering instrumentation was not loaded! Skipping configuration.");
            return;
        }
        instrumentation.setSlowRenderingDetectionPollInterval(
                builder.slowRenderingDetectionPollInterval);
        initializationEvents.emit("slowRenderingDetectorInitialized");
    }

    private void configureCrashReporter() {
        CrashReporterInstrumentation instrumentation =
                AndroidInstrumentationLoader.getInstrumentation(CrashReporterInstrumentation.class);
        if (instrumentation == null) {
            Log.w(
                    LOG_TAG,
                    "Crash reporter instrumentation was not loaded! Skipping configuration.");
            return;
        }
        instrumentation.addAttributesExtractor(
                RuntimeDetailsExtractor.create(application.getApplicationContext()));
        instrumentation.addAttributesExtractor(new CrashComponentExtractor());
        addErrorIdentifyingAttributes(instrumentation::addAttributesExtractor);

        initializationEvents.emit("crashReportingInitialized");
    }

    private <REQ, RES> void addErrorIdentifyingAttributes(
            Consumer<AttributesExtractor<REQ, RES>> consumer) {
        ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(application);
        ErrorIdentifierInfo errorIdentifierInfo = extractor.extractInfo();
        String applicationId = errorIdentifierInfo.getApplicationId();
        String versionCode = errorIdentifierInfo.getVersionCode();
        if (applicationId != null) {
            consumer.accept(constant(APPLICATION_ID_KEY, applicationId));
        }
        if (versionCode != null) {
            consumer.accept(constant(APP_VERSION_CODE_KEY, versionCode));
        }
        if (errorIdentifierInfo.getCustomUUID() != null) {
            consumer.accept(constant(SPLUNK_OLLY_UUID_KEY, errorIdentifierInfo.getCustomUUID()));
        }
    }

    // visible for testing
    @NonNull
    SpanExporter buildSpanExporter(SpanExporter delegate) {
        OtlpHttpSpanExporter otlp =
                OtlpHttpSpanExporter.builder()
                        .setEndpoint(builder.beaconEndpoint)
                        .addHeader("X-SF-Token", builder.rumAccessToken)
                        .build();
        SpanExporter splunkTranslatedExporter =
                new SplunkSpanDataModifier(otlp, builder.isReactNativeSupportEnabled(), true);
        SpanExporter filteredExporter = builder.decorateWithSpanFilter(splunkTranslatedExporter);
        initializationEvents.emit("otlp span exporter initialized");
        return filteredExporter;
    }

    @NonNull
    private String getEndpointWithAuthTokenQueryParam() {
        return builder.beaconEndpoint + "?auth=" + builder.rumAccessToken;
    }

    //    private static ThrottlingExporter buildThrottlingExporter(
    //            MemoryBufferingExporter memoryBufferingExporter) {
    //        return ThrottlingExporter.newBuilder(memoryBufferingExporter)
    //                .categorizeByAttribute(COMPONENT_KEY)
    //                .maxSpansInWindow(100)
    //                .windowSize(Duration.ofSeconds(30))
    //                .build();
    //    }
    //
    //    // visible for testing
    //    SpanExporter getCoreSpanExporter() {
    //        Supplier<SpanExporter> exporterSupplier = supplyZipkinExporter();
    //        if (builder.shouldUseOtlpExporter()) {
    //            exporterSupplier = supplyOtlpExporter();
    //        }
    //        // return a lazy init exporter so the main thread doesn't block on the setup.
    //        return new LazyInitSpanExporter(exporterSupplier);
    //    }

    //
    //    //TODO: This needs to go away as part of 2.0, OTLP only
    //    @NonNull
    //    private Supplier<SpanExporter> supplyZipkinExporter() {
    //        String endpoint = getEndpointWithAuthTokenQueryParam();
    //        return () ->
    //                ZipkinSpanExporter.builder()
    //                        .setEncoder(new CustomZipkinEncoder())
    //                        .setEndpoint(endpoint)
    //                        // remove the local IP address
    //                        .setLocalIpAddressSupplier(() -> null)
    //                        .setSender(buildCustomizedZipkinSender())
    //                        .build();
    //    }
    //
    //    private static class LazyInitSpanExporter implements SpanExporter {
    //        @Nullable private volatile SpanExporter delegate;
    //        private final Supplier<SpanExporter> s;
    //
    //        public LazyInitSpanExporter(Supplier<SpanExporter> s) {
    //            this.s = s;
    //        }
    //
    //        private SpanExporter getDelegate() {
    //            SpanExporter d = delegate;
    //            if (d == null) {
    //                synchronized (this) {
    //                    d = delegate;
    //                    if (d == null) {
    //                        delegate = d = s.get();
    //                    }
    //                }
    //            }
    //            return d;
    //        }
    //
    //        @Override
    //        public CompletableResultCode export(Collection<SpanData> spans) {
    //            return getDelegate().export(spans);
    //        }
    //
    //        @Override
    //        public CompletableResultCode flush() {
    //            return getDelegate().flush();
    //        }
    //
    //        @Override
    //        public CompletableResultCode shutdown() {
    //            return getDelegate().shutdown();
    //        }
    //    }
}
