/*
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.okhttp3;

import android.annotation.SuppressLint;
import com.splunk.rum.agent.common.utils.InstrumenterBuildUtils;
import com.splunk.rum.instrumentation.okhttp3.common.internal.OkHttpAttributesGetter;
import com.splunk.rum.instrumentation.okhttp3.common.internal.OkHttpClientInstrumenterBuilderFactory;
import com.splunk.rum.instrumentation.okhttp3.common.internal.PeerServiceAttributesExtractor;
import com.splunk.rum.instrumentation.okhttp3.internal.Experimental;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.internal.ServiceLoaderUtil;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import okhttp3.Interceptor;
import okhttp3.Response;

/** Builder for {@link OkHttpTelemetry}. */
public final class OkHttpTelemetryBuilder {

  static {
    Experimental.internalSetEmitExperimentalTelemetry(
        (builder, emit) -> builder.emitExperimentalHttpClientTelemetry = emit);
  }

  private final OpenTelemetry openTelemetry;
  private final List<AttributesExtractor<Interceptor.Chain, Response>> additionalExtractors =
      new ArrayList<>();
  private Collection<String> capturedRequestHeaders;
  private Collection<String> capturedResponseHeaders;
  private Collection<String> knownMethods;
  // Null unless the caller opts in via setSpanNameExtractor().
  private Function<SpanNameExtractor<Interceptor.Chain>, SpanNameExtractor<Interceptor.Chain>>
      spanNameExtractorTransformer;
  private Map<String, String> peerServiceMapping = Collections.emptyMap();
  private boolean emitExperimentalHttpClientTelemetry;

  OkHttpTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an {@link AttributesExtractor} to extract attributes from requests and responses. Executed
   * after all default extractors.
   */
  public OkHttpTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<Interceptor.Chain, Response> attributesExtractor) {
    additionalExtractors.add(attributesExtractor);
    return this;
  }

  /**
   * Configures HTTP request headers to capture as span attributes.
   *
   * @param requestHeaders HTTP header names to capture.
   */
  public OkHttpTelemetryBuilder setCapturedRequestHeaders(Collection<String> requestHeaders) {
    capturedRequestHeaders = new ArrayList<>(requestHeaders);
    return this;
  }

  /**
   * Configures HTTP response headers to capture as span attributes.
   *
   * @param responseHeaders HTTP header names to capture.
   */
  public OkHttpTelemetryBuilder setCapturedResponseHeaders(Collection<String> responseHeaders) {
    capturedResponseHeaders = new ArrayList<>(responseHeaders);
    return this;
  }

  /**
   * Configures recognized HTTP request methods.
   *
   * <p>By default, recognizes methods from <a
   * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-methods">RFC9110</a> and PATCH from <a
   * href="https://www.rfc-editor.org/rfc/rfc5789.html">RFC5789</a>.
   *
   * <p><b>Note:</b> This <b>overrides</b> defaults completely; it does not supplement them.
   *
   * @param knownMethods HTTP request methods to recognize.
   * @see HttpClientAttributesExtractorBuilder#setKnownMethods(Collection)
   */
  public OkHttpTelemetryBuilder setKnownMethods(Collection<String> knownMethods) {
    this.knownMethods = new ArrayList<>(knownMethods);
    return this;
  }

  /**
   * Configures the extractor of the {@code peer.service} span attribute.
   */
  public OkHttpTelemetryBuilder setPeerServiceMapping(Map<String, String> peerServiceMapping) {
    this.peerServiceMapping = new HashMap<>(peerServiceMapping);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @SuppressLint("NewApi") // This existing java.util.function API requires consumer desugaring.
  public OkHttpTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<Interceptor.Chain>, SpanNameExtractor<Interceptor.Chain>>
          spanNameExtractorTransformer) {
    this.spanNameExtractorTransformer = spanNameExtractorTransformer;
    return this;
  }

  /** Returns a new instance with the configured settings. */
  public OkHttpTelemetry build() {
    DefaultHttpClientInstrumenterBuilder<Interceptor.Chain, Response> builder =
        OkHttpClientInstrumenterBuilderFactory.create(openTelemetry);
    builder.addAttributesExtractor(
        new PeerServiceAttributesExtractor(
            OkHttpAttributesGetter.INSTANCE,
            peerServiceMapping));
    for (AttributesExtractor<Interceptor.Chain, Response> extractor : additionalExtractors) {
      builder.addAttributesExtractor(extractor);
    }
    if (capturedRequestHeaders != null) {
      builder.setCapturedRequestHeaders(capturedRequestHeaders);
    }
    if (capturedResponseHeaders != null) {
      builder.setCapturedResponseHeaders(capturedResponseHeaders);
    }
    if (knownMethods != null) {
      builder.setKnownMethods(knownMethods);
    }
    if (spanNameExtractorTransformer != null) {
      applySpanNameExtractorCustomizer(builder, spanNameExtractorTransformer);
    }
    builder.setEmitExperimentalHttpClientTelemetry(emitExperimentalHttpClientTelemetry);
    return new OkHttpTelemetry(buildInstrumenter(builder), openTelemetry.getPropagators());
  }

  // Avoid the instrumenter SPI lookup's one-time disk read, which trips Android StrictMode
  // (see open-telemetry/opentelemetry-java-instrumentation issue #19954). ServiceLoaderUtil has no
  // getter, so the reset below restores ServiceLoader::load rather than any custom process-wide
  // loader that may have been installed by the host application.
  private static Instrumenter<Interceptor.Chain, Response> buildInstrumenter(
      DefaultHttpClientInstrumenterBuilder<Interceptor.Chain, Response> builder) {
    return InstrumenterBuildUtils.synchronizedInstrumenterBuild(
        () -> {
          ServiceLoaderUtil.setLoadFunction(clazz -> Collections.emptyList());
          return kotlin.Unit.INSTANCE;
        },
        () -> {
          ServiceLoaderUtil.setLoadFunction(ServiceLoader::load);
          return kotlin.Unit.INSTANCE;
        },
        builder::build,
        builder::build);
  }

  // Isolated so Function#apply (requires API 24 or desugaring) only runs when a caller opts in
  // via setSpanNameExtractor(), not on every build() call.
  @SuppressLint("NewApi") // Requires API 24 or core library desugaring.
  private static void applySpanNameExtractorCustomizer(
      DefaultHttpClientInstrumenterBuilder<Interceptor.Chain, Response> builder,
      Function<SpanNameExtractor<Interceptor.Chain>, SpanNameExtractor<Interceptor.Chain>>
          spanNameExtractorTransformer) {
    builder.setSpanNameExtractorCustomizer(
        spanNameExtractor -> spanNameExtractorTransformer.apply(spanNameExtractor));
  }
}
