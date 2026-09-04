/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.instrumentation.okhttp3.internal;

import com.splunk.rum.agent.common.utils.PeerServiceMappingResolver;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import java.net.URI;
import java.util.Map;
import okhttp3.Interceptor;
import okhttp3.Response;

/** Extracts the configured {@code peer.service} mapping for manually instrumented OkHttp requests. */
public final class PeerServiceAttributesExtractor
    implements AttributesExtractor<Interceptor.Chain, Response> {

  private static final AttributeKey<String> PEER_SERVICE = AttributeKey.stringKey("peer.service");

  private final ServerAttributesGetter<Interceptor.Chain> attributesGetter;
  private final PeerServiceMappingResolver resolver;

  public PeerServiceAttributesExtractor(
      ServerAttributesGetter<Interceptor.Chain> attributesGetter,
      Map<String, String> peerServiceMapping) {
    this.attributesGetter = attributesGetter;
    resolver = new PeerServiceMappingResolver(peerServiceMapping);
  }

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, Interceptor.Chain request) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Interceptor.Chain request,
      Response response,
      Throwable error) {
    if (resolver.isEmpty()) {
      return;
    }

    String serviceName =
        resolver.resolve(
            attributesGetter.getServerAddress(request),
            attributesGetter.getServerPort(request),
            getPath(request));
    if (serviceName != null) {
      attributes.put(PEER_SERVICE, serviceName);
    }
  }

  private static String getPath(Interceptor.Chain request) {
    try {
      return new URI(request.request().url().toString()).getPath();
    } catch (java.net.URISyntaxException ignored) {
      return null;
    }
  }
}
