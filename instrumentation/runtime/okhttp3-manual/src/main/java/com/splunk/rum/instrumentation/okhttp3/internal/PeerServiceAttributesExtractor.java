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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Interceptor;
import okhttp3.Response;

/** Extracts the existing programmatic {@code peer.service} mapping for manual OkHttp telemetry. */
public final class PeerServiceAttributesExtractor
    implements AttributesExtractor<Interceptor.Chain, Response> {

  private static final AttributeKey<String> PEER_SERVICE = AttributeKey.stringKey("peer.service");

  private final ServerAttributesGetter<Interceptor.Chain> attributesGetter;
  private final Map<String, List<Mapping>> mappingsByHost;

  public PeerServiceAttributesExtractor(
      ServerAttributesGetter<Interceptor.Chain> attributesGetter,
      Map<String, String> peerServiceMapping) {
    this.attributesGetter = attributesGetter;
    mappingsByHost = new HashMap<>();
    for (Map.Entry<String, String> entry : peerServiceMapping.entrySet()) {
      Mapping mapping = Mapping.parse(entry.getKey(), entry.getValue());
      if (mapping != null) {
        List<Mapping> mappings = mappingsByHost.get(mapping.host());
        if (mappings == null) {
          mappings = new ArrayList<>();
          mappingsByHost.put(mapping.host(), mappings);
        }
        mappings.add(mapping);
      }
    }
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
    String host = attributesGetter.getServerAddress(request);
    if (host == null) {
      return;
    }

    Integer port = attributesGetter.getServerPort(request);
    List<Mapping> mappings = mappingsByHost.get(host);
    if (mappings == null) {
      return;
    }

    Mapping bestMatch = null;
    for (Mapping mapping : mappings) {
      if (mapping.matches(port)
          && (bestMatch == null || MAPPING_SPECIFICITY.compare(mapping, bestMatch) > 0)) {
        bestMatch = mapping;
      }
    }
    if (bestMatch != null) {
      attributes.put(PEER_SERVICE, bestMatch.serviceName());
    }
  }

  private static final Comparator<Mapping> MAPPING_SPECIFICITY =
      new Comparator<Mapping>() {
        @Override
        public int compare(Mapping first, Mapping second) {
          if (first.port() == null) {
            return second.port() == null ? 0 : -1;
          }
          if (second.port() == null) {
            return 1;
          }
          return first.port().compareTo(second.port());
        }
      };

  private static final class Mapping {
    private final String host;
    private final Integer port;
    private final String serviceName;

    private Mapping(String host, Integer port, String serviceName) {
      this.host = host;
      this.port = port;
      this.serviceName = serviceName;
    }

    static Mapping parse(String peer, String serviceName) {
      try {
        URI uri = URI.create("https://" + peer);
        if (uri.getHost() == null) {
          return null;
        }
        return new Mapping(
            uri.getHost(), uri.getPort() >= 0 ? uri.getPort() : null, serviceName);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    boolean matches(Integer requestPort) {
      return port == null || port.equals(requestPort);
    }

    String host() {
      return host;
    }

    Integer port() {
      return port;
    }

    String serviceName() {
      return serviceName;
    }
  }
}
