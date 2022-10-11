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

package com.splunk.rum.internal.opentelemetry;

import android.app.Application;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.time.Duration;
import java.util.function.BiFunction;

/**
 * A builder of {@link OpenTelemetryRum}. It enabled configuring the OpenTelemetry SDK and disabling
 * built-in Android instrumentations.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface OpenTelemetryRumBuilder {

    /**
     * Assign a {@link Resource} to be attached to all telemetry emitted by the {@link
     * OpenTelemetryRum} created by this builder.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder setResource(Resource resource);

    /**
     * Adds a {@link BiFunction} to invoke the with the {@link SdkTracerProviderBuilder} to allow
     * customization. The return value of the {@link BiFunction} will replace the passed-in
     * argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     *
     * <p>Note: calling {@link SdkTracerProviderBuilder#setResource(Resource)} inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via {@link #setResource(Resource)}.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder addTracerProviderCustomizer(
            BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder> customizer);

    /**
     * Adds a {@link BiFunction} to invoke the with the {@link SdkMeterProviderBuilder} to allow
     * customization. The return value of the {@link BiFunction} will replace the passed-in
     * argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     *
     * <p>Note: calling {@link SdkMeterProviderBuilder#setResource(Resource)} inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via {@link #setResource(Resource)}.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder addMeterProviderCustomizer(
            BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder> customizer);

    // addLoggerProviderCustomizer

    /**
     * Enables debugging information to be emitted from the created {@link OpenTelemetryRum}.
     *
     * <p>This feature is disabled by default. You can enable it by calling this configuration
     * method with a {@code true} value.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder enableDebug();

    /**
     * Disables the crash reporting feature.
     *
     * <p>This feature is enabled by default. You can disable it by calling this configuration
     * method with a {@code true} value.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder disableCrashReporting();

    /**
     * Disables the network monitoring feature.
     *
     * <p>This feature is enabled by default. You can disable it by calling this configuration
     * method with a {@code true} value.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder disableNetworkMonitor();

    /**
     * Disables the ANR (application not responding) detection feature. If enabled, when the main
     * thread is unresponsive for 5 seconds or more, an event including the main thread's stack
     * trace will be reported to the RUM system.
     *
     * <p>This feature is enabled by default. You can disable it by calling this configuration
     * method with a {@code true} value.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder disableAnrDetection();

    /**
     * Disables the slow rendering detection feature.
     *
     * <p>This feature is enabled by default. You can disable it by calling this configuration
     * method with a {@code true} value.
     *
     * @return {@code this}
     */
    OpenTelemetryRumBuilder disableSlowRenderingDetection();

    /**
     * Configures the rate at which frame render durations are polled.
     *
     * @param interval The period that should be used for polling
     * @return {@code this}
     */
    OpenTelemetryRumBuilder setSlowRenderingDetectionPollInterval(Duration interval);

    /**
     * Creates a new instance of {@link OpenTelemetryRum} with the settings of this {@link
     * OpenTelemetryRumBuilder}.
     *
     * <p>This method will initialize the OpenTelemetry SDK and install built-in system
     * instrumentations in the passed Android {@link Application}.
     *
     * @param application The {@link Application} that is being instrumented.
     * @return A new {@link OpenTelemetryRum} instance.
     */
    OpenTelemetryRum build(Application application);
}
