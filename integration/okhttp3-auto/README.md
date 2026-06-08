# OkHttp3 Auto Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- The `com.splunk.rum-okhttp3-auto-plugin` Gradle plugin for bytecode hooks.
- OkHttp 3.x compatible clients.

## Overview

OkHttp3 Auto Instrumentation tracks OkHttp client requests without manually wrapping each client.

Detection sources:

- Build-time instrumentation from the `com.splunk.rum-okhttp3-auto-plugin` Gradle plugin.
- Runtime OpenTelemetry OkHttp instrumentation.
- `Content-Length` request and response headers.
- `Server-Timing` response headers containing server trace context.

Detection behavior:

- The module is enabled by default, but automatic request tracking requires the Gradle plugin.
- When disabled, runtime OkHttp auto-instrumentation is not configured.
- Captured request and response headers are opt-in through configuration.
- Unknown payload sizes, including negative HTTP length values, are omitted.
- Server trace context is parsed from valid `Server-Timing` response headers.

## Quick Start

Apply the Gradle plugin:

```kotlin
plugins {
    id("com.splunk.rum-okhttp3-auto-plugin")
}
```

Configure the module:

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    OkHttp3AutoModuleConfiguration(
        isEnabled = true,
        capturedRequestHeaders = listOf("x-request-id"),
        capturedResponseHeaders = listOf("server-timing")
    )
)
```

## API Documentation

### `OkHttp3AutoModuleConfiguration`

Configures automatic OkHttp request tracking.

```kotlin
OkHttp3AutoModuleConfiguration(
    isEnabled: Boolean = true,
    capturedRequestHeaders: List<String> = emptyList(),
    capturedResponseHeaders: List<String> = emptyList()
)
```

- `isEnabled`: Enables or disables runtime OkHttp auto-instrumentation.
- `capturedRequestHeaders`: Request headers to capture as normalized `http.request.header.<name>` attributes.
- `capturedResponseHeaders`: Response headers to capture as normalized `http.response.header.<name>` attributes.
- `name`: Module name reported as `okHttp3-auto`.
- `attributes`: Module attributes containing `enabled`, `requestHeaders`, and `responseHeaders`.

## Telemetry Data Model

The exported telemetry is HTTP client spans created by OpenTelemetry OkHttp instrumentation.

| Signal | Name | When emitted |
|---|---|---|
| Span | `<HTTP method>` | A bytecode-instrumented OkHttp request is executed. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `http` |
| `http.request.body.size` | long | No | Request `Content-Length` when known and non-negative. |
| `http.response.body.size` | long | No | Response `Content-Length` when known and non-negative. |
| `link.traceId` | string | No | Server trace ID parsed from `Server-Timing`. |
| `link.spanId` | string | No | Server span ID parsed from `Server-Timing`. |
| `http.request.header.<name>` | string | No | Captured request header values. |
| `http.response.header.<name>` | string | No | Captured response header values. |
| `<HTTP semantic convention attribute>` | OTel type | Yes | Standard HTTP client attributes added by OpenTelemetry. |

Resource / scope:

- Instrumentation scope name: `io.opentelemetry.okhttp-3.0`
- Span kind: `CLIENT`
- Span duration: request duration
