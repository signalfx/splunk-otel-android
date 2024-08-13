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
import static com.splunk.rum.SplunkRum.RUM_TRACER_NAME;
import static com.splunk.rum.SplunkRum.SPLUNK_OLLY_UUID_KEY;
import static io.opentelemetry.android.common.RumConstants.APP_START_SPAN_NAME;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT;
import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.splunk.rum.internal.GlobalAttributesSupplier;
import com.splunk.rum.internal.NoOpSpanExporter;
import com.splunk.rum.internal.UInt32QuadXorTraceIdRatioSampler;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.OpenTelemetryRumBuilder;
import io.opentelemetry.android.RuntimeDetailsExtractor;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.android.instrumentation.anr.AnrDetector;
import io.opentelemetry.android.instrumentation.anr.AnrDetectorBuilder;
import io.opentelemetry.android.instrumentation.crash.CrashReporter;
import io.opentelemetry.android.instrumentation.crash.CrashReporterBuilder;
import io.opentelemetry.android.instrumentation.lifecycle.AndroidLifecycleInstrumentation;
import io.opentelemetry.android.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingDetector;
import io.opentelemetry.android.instrumentation.startup.AppStartupTimer;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

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
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

        initializationEvents.begin();

        OtelRumConfig config = new OtelRumConfig();
        GlobalAttributesSupplier globalAttributeSupplier =
                new GlobalAttributesSupplier(builder.globalAttributes);
        config.setGlobalAttributes(globalAttributeSupplier);

        // TODO: Note/document this instrumentation is now opt-in via application classpath via build settings
//        if (!builder.isNetworkMonitorEnabled()) {
//            config.disableNetworkChangeMonitoring();
//        }

        config.disableScreenAttributes();
        OpenTelemetryRumBuilder otelRumBuilder = OpenTelemetryRum.builder(application, config);

        otelRumBuilder.mergeResource(createSplunkResource());
        initializationEvents.emit("resourceInitialized");

        // TODO: now spelled rum.sdk.init.net.provider and currently mixed up in network
        // attributes enabled config in upstream
//        CurrentNetworkProvider currentNetworkProvider =
//                CurrentNetworkProvider.createAndStart(application);
//        otelRumBuilder.setCurrentNetworkProvider(currentNetworkProvider);
//        initializationEvents.emit("connectionUtilInitialized");

        // TODO: How truly important is the order of these span processors? The location of event
        // generation should probably not be altered...

        // Add batch span processor
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) -> {
                    SpanExporter zipkinExporter =
                            buildFilteringExporter(currentNetworkProvider, visibleScreenTracker);
                    initializationEvents.emit("exporterInitialized");

                    BatchSpanProcessor batchSpanProcessor =
                            BatchSpanProcessor.builder(zipkinExporter).build();
                    initializationEvents.emit("batchSpanProcessorInitialized");
                    return tracerProviderBuilder.addSpanProcessor(batchSpanProcessor);
                });

        // Inhibit the upstream exporter because we add our own BatchSpanProcessor
        otelRumBuilder.addSpanExporterCustomizer(x -> new NoOpSpanExporter());

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
            installAnrDetector(otelRumBuilder, mainLooper);
        }
        if (builder.isSlowRenderingDetectionEnabled()) {
            installSlowRenderingDetector(otelRumBuilder);
        }
        if (builder.isCrashReportingEnabled()) {
            installCrashReporter(otelRumBuilder);
        }

        SettableScreenAttributesAppender screenAttributesAppender =
                new SettableScreenAttributesAppender(visibleScreenTracker);
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) ->
                        tracerProviderBuilder.addSpanProcessor(screenAttributesAppender));

        // Lifecycle events instrumentation are always installed.
        installLifecycleInstrumentations(otelRumBuilder, visibleScreenTracker);

        OpenTelemetryRum openTelemetryRum = otelRumBuilder.build();

        sessionSupplierHolder.set(openTelemetryRum::getRumSessionId);

        initializationEvents.recordInitializationSpans(
                builder.getConfigFlags(),
                openTelemetryRum.getOpenTelemetry().getTracer(RUM_TRACER_NAME));

        return new SplunkRum(openTelemetryRum, globalAttributeSupplier, screenAttributesAppender);
    }

    @NonNull
    private MemorySpanBuffer constructBacklogProvider(VisibleScreenTracker visibleScreenTracker) {
        if (builder.isBackgroundInstrumentationDeferredUntilForeground()) {
            return new StartTypeAwareMemorySpanBuffer(visibleScreenTracker);
        } else {
            return new DefaultMemorySpanBuffer();
        }
    }

    @NonNull
    private SpanStorage constructSpanFileProvider(VisibleScreenTracker visibleScreenTracker) {
        if (builder.isBackgroundInstrumentationDeferredUntilForeground()) {
            return StartTypeAwareSpanStorage.create(
                    visibleScreenTracker,
                    new FileUtils(),
                    application.getApplicationContext().getFilesDir());
        } else {
            return new DefaultSpanStorage(
                    new FileUtils(), application.getApplicationContext().getFilesDir());
        }
    }

    private void installLifecycleInstrumentations(
            OpenTelemetryRumBuilder otelRumBuilder, VisibleScreenTracker visibleScreenTracker) {

        otelRumBuilder.addInstrumentation(
                instrumentedApp -> {
                    Function<Tracer, Tracer> tracerCustomizer =
                            tracer ->
                                    (Tracer)
                                            spanName -> {
                                                String component =
                                                        spanName.equals(APP_START_SPAN_NAME)
                                                                ? COMPONENT_APPSTART
                                                                : COMPONENT_UI;
                                                return tracer.spanBuilder(spanName)
                                                        .setAttribute(COMPONENT_KEY, component);
                                            };
                    AndroidLifecycleInstrumentation instrumentation =
                            AndroidLifecycleInstrumentation.builder()
                                    .setVisibleScreenTracker(visibleScreenTracker)
                                    .setStartupTimer(startupTimer)
                                    .setTracerCustomizer(tracerCustomizer)
                                    .setScreenNameExtractor(SplunkScreenNameExtractor.INSTANCE)
                                    .build();
                    instrumentation.installOn(instrumentedApp);
                    initializationEvents.emit("activityLifecycleCallbacksInitialized");
                });
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

    private void installAnrDetector(OpenTelemetryRumBuilder otelRumBuilder, Looper mainLooper) {
        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(application);
                    ErrorIdentifierInfo errorIdentifierInfo = extractor.extractInfo();
                    String applicationId = errorIdentifierInfo.getApplicationId();
                    String versionCode = errorIdentifierInfo.getVersionCode();
                    String uuid = errorIdentifierInfo.getCustomUUID();

                    AnrDetectorBuilder builder = AnrDetector.builder();
                    builder.addAttributesExtractor(constant(COMPONENT_KEY, COMPONENT_ERROR));

                    if (applicationId != null)
                        builder.addAttributesExtractor(constant(APPLICATION_ID_KEY, applicationId));
                    if (versionCode != null)
                        builder.addAttributesExtractor(constant(APP_VERSION_CODE_KEY, versionCode));
                    if (uuid != null)
                        builder.addAttributesExtractor(constant(SPLUNK_OLLY_UUID_KEY, uuid));

                    builder.setMainLooper(mainLooper).build().installOn(instrumentedApplication);

                    initializationEvents.emit("anrMonitorInitialized");
                });
    }

    private void installCrashReporter(OpenTelemetryRumBuilder otelRumBuilder) {
        otelRumBuilder.addInstrumentation(
                (app,otelRum) -> {
                    ErrorIdentifierExtractor extractor = new ErrorIdentifierExtractor(application);
                    ErrorIdentifierInfo errorIdentifierInfo = extractor.extractInfo();
                    String applicationId = errorIdentifierInfo.getApplicationId();
                    String versionCode = errorIdentifierInfo.getVersionCode();
                    String uuid = errorIdentifierInfo.getCustomUUID();

                    CrashReporterBuilder builder = CrashReporter.builder();
                    builder.addAttributesExtractor(
                                    RuntimeDetailsExtractor.create(
                                            app.getApplicationContext()))
                            .addAttributesExtractor(new CrashComponentExtractor());

                    if (applicationId != null)
                        builder.addAttributesExtractor(constant(APPLICATION_ID_KEY, applicationId));
                    if (versionCode != null)
                        builder.addAttributesExtractor(constant(APP_VERSION_CODE_KEY, versionCode));
                    if (uuid != null)
                        builder.addAttributesExtractor(constant(SPLUNK_OLLY_UUID_KEY, uuid));

                    builder.build().installOn(instrumentedApplication);

                    initializationEvents.emit("crashReportingInitialized");
                });
    }

    private void installSlowRenderingDetector(OpenTelemetryRumBuilder otelRumBuilder) {
        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    SlowRenderingDetector.builder()
                            .setSlowRenderingDetectionPollInterval(
                                    builder.slowRenderingDetectionPollInterval)
                            .build()
                            .installOn(app);
                    initializationEvents.emit("slowRenderingDetectorInitialized");
                });
    }

    // visible for testing
    SpanExporter buildFilteringExporter(
            CurrentNetworkProvider currentNetworkProvider,
            VisibleScreenTracker visibleScreenTracker) {
        SpanExporter exporter = buildExporter(currentNetworkProvider, visibleScreenTracker);
        SpanExporter splunkTranslatedExporter =
                new SplunkSpanDataModifier(
                        exporter,
                        builder.isReactNativeSupportEnabled(),
                        builder.shouldUseOtlpExporter());
        SpanExporter filteredExporter = builder.decorateWithSpanFilter(splunkTranslatedExporter);
        initializationEvents.emit("zipkin exporter initialized");
        return filteredExporter;
    }

    private SpanExporter buildExporter(
            CurrentNetworkProvider currentNetworkProvider,
            VisibleScreenTracker visibleScreenTracker) {
        if (builder.isDebugEnabled()) {
            // tell the Zipkin exporter to shut up already. We're on mobile, network stuff happens.
            // we'll do our best to hang on to the spans with the wrapping BufferingExporter.
            ZipkinSpanExporter.baseLogger.setLevel(Level.SEVERE);
            initializationEvents.emit("logger setup complete");
        }

        if (builder.isDiskBufferingEnabled()) {
            return buildStorageBufferingExporter(
                    currentNetworkProvider, constructSpanFileProvider(visibleScreenTracker));
        }

        return buildMemoryBufferingThrottledExporter(
                currentNetworkProvider, constructBacklogProvider(visibleScreenTracker));
    }

    //TODO: Make this use OTLP buffering via upstream
    private SpanExporter buildStorageBufferingExporter(
            CurrentNetworkProvider currentNetworkProvider, SpanStorage spanStorage) {
        Sender sender = buildCustomizedZipkinSender();

        BandwidthTracker bandwidthTracker = new BandwidthTracker();

        FileSender fileSender =
                FileSender.builder().sender(sender).bandwidthTracker(bandwidthTracker).build();
        DiskToZipkinExporter diskToZipkinExporter =
                DiskToZipkinExporter.builder()
                        .connectionUtil(currentNetworkProvider)
                        .fileSender(fileSender)
                        .bandwidthTracker(bandwidthTracker)
                        .spanFileProvider(spanStorage)
                        .build();
        diskToZipkinExporter.startPolling();

        return getToDiskExporter(spanStorage);
    }

    @NonNull
    private Sender buildCustomizedZipkinSender() {
        OkHttpSender.Builder okBuilder =
                OkHttpSender.newBuilder().endpoint(getEndpointWithAuthTokenQueryParam());
        builder.httpSenderCustomizer.customize(okBuilder);
        return okBuilder.build();
    }

    @NonNull
    private String getEndpointWithAuthTokenQueryParam() {
        return builder.beaconEndpoint + "?auth=" + builder.rumAccessToken;
    }

    private SpanExporter buildMemoryBufferingThrottledExporter(
            CurrentNetworkProvider currentNetworkProvider, MemorySpanBuffer backlogProvider) {
        SpanExporter zipkinSpanExporter = getCoreSpanExporter();
        MemoryBufferingExporter memoryBufferingExporter =
                new MemoryBufferingExporter(
                        currentNetworkProvider, zipkinSpanExporter, backlogProvider);
        return buildThrottlingExporter(memoryBufferingExporter);
    }

    private static ThrottlingExporter buildThrottlingExporter(
            MemoryBufferingExporter memoryBufferingExporter) {
        return ThrottlingExporter.newBuilder(memoryBufferingExporter)
                .categorizeByAttribute(COMPONENT_KEY)
                .maxSpansInWindow(100)
                .windowSize(Duration.ofSeconds(30))
                .build();
    }

    SpanExporter getToDiskExporter(SpanStorage spanStorage) {
        return new LazyInitSpanExporter(
                () ->
                        ZipkinWriteToDiskExporterFactory.create(
                                builder.maxUsageMegabytes, spanStorage));
    }

    // visible for testing
    SpanExporter getCoreSpanExporter() {
        Supplier<SpanExporter> exporterSupplier = supplyZipkinExporter();
        if (builder.shouldUseOtlpExporter()) {
            exporterSupplier = supplyOtlpExporter();
        }
        // return a lazy init exporter so the main thread doesn't block on the setup.
        return new LazyInitSpanExporter(exporterSupplier);
    }

    @NonNull
    private Supplier<SpanExporter> supplyOtlpExporter() {
        String endpoint = getEndpointWithAuthTokenQueryParam();
        return () ->
                OtlpHttpSpanExporter.builder()
                        .setEndpoint(endpoint)
                        .addHeader("X-SF-Token", builder.rumAccessToken)
                        .build();
    }

    //TODO: This needs to go away as part of 2.0, OTLP only
    @NonNull
    private Supplier<SpanExporter> supplyZipkinExporter() {
        String endpoint = getEndpointWithAuthTokenQueryParam();
        return () ->
                ZipkinSpanExporter.builder()
                        .setEncoder(new CustomZipkinEncoder())
                        .setEndpoint(endpoint)
                        // remove the local IP address
                        .setLocalIpAddressSupplier(() -> null)
                        .setSender(buildCustomizedZipkinSender())
                        .build();
    }

    private static class LazyInitSpanExporter implements SpanExporter {
        @Nullable private volatile SpanExporter delegate;
        private final Supplier<SpanExporter> s;

        public LazyInitSpanExporter(Supplier<SpanExporter> s) {
            this.s = s;
        }

        private SpanExporter getDelegate() {
            SpanExporter d = delegate;
            if (d == null) {
                synchronized (this) {
                    d = delegate;
                    if (d == null) {
                        delegate = d = s.get();
                    }
                }
            }
            return d;
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            return getDelegate().export(spans);
        }

        @Override
        public CompletableResultCode flush() {
            return getDelegate().flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            return getDelegate().shutdown();
        }
    }
}
