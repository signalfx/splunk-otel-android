# OkHttp3 Common Instrumentation Support

## Requirements

- OkHttp 3.x compatible request and response types.
- OpenTelemetry instrumentation API.
- Used by `integration/okhttp3-auto` and `integration/okhttp3-manual`.

## Overview

OkHttp3 Common provides the shared attribute extractor used by automatic and manual OkHttp instrumentation.

Detection sources:

- OkHttp `Interceptor.Chain` request data.
- OkHttp `Response` header data.
- `Content-Length` request and response headers.
- `Server-Timing` response headers containing server trace context.

Detection behavior:

- This module is not installed independently through `SplunkRum.install(...)`.
- The auto and manual OkHttp modules attach `OkHttp3AdditionalAttributesExtractor` to their OpenTelemetry instrumentation.
- Unknown payload sizes, including negative HTTP length values, are omitted.
- Server trace context is parsed from valid `Server-Timing` response headers.

## Quick Start

This module is used by the OkHttp instrumentation modules:

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    OkHttp3AutoModuleConfiguration()
)
```

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    OkHttp3ManualModuleConfiguration()
)
```

## API Documentation

### `OkHttp3AdditionalAttributesExtractor`

Adds Splunk RUM HTTP attributes to OpenTelemetry OkHttp spans.

```kotlin
class OkHttp3AdditionalAttributesExtractor :
    AttributesExtractor<Interceptor.Chain, Response>
```

- `onStart(...)`: Adds `component = http`.
- `onEnd(...)`: Adds request size, response size, and server trace context attributes when available.

## Telemetry Data Model

This support module does not emit telemetry by itself. It enriches spans emitted by OkHttp auto and manual instrumentation.

| Signal | Name | When emitted |
|---|---|---|
| N/A | N/A | No standalone telemetry. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes when extractor is used | `http` |
| `http.request.body.size` | long | No | Request `Content-Length` when known and non-negative. |
| `http.response.body.size` | long | No | Response `Content-Length` when known and non-negative. |
| `link.traceId` | string | No | Server trace ID parsed from `Server-Timing`. |
| `link.spanId` | string | No | Server span ID parsed from `Server-Timing`. |

Resource / scope:

- Instrumentation scope name: determined by the consuming OkHttp instrumentation
- Span kind: determined by the consuming OkHttp instrumentation
- Span duration: determined by the consuming OkHttp instrumentation
