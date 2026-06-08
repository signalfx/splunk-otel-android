# OkHttp3 Manual Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- OkHttp 3.x compatible clients.
- Application code must use the returned `Call.Factory`.

## Overview

OkHttp3 Manual Instrumentation tracks OkHttp requests by wrapping an existing `OkHttpClient` as an instrumented `Call.Factory`.

Detection sources:

- Manual calls to `OkHttpManualInstrumentation.buildOkHttpCallFactory(...)`.
- OpenTelemetry `OkHttpTelemetry` interceptors added to the wrapped client.
- `Content-Length` request and response headers.
- `Server-Timing` response headers containing server trace context.

Detection behavior:

- There is no `isEnabled` flag on the manual module configuration.
- The module creates `OkHttpTelemetry` during install.
- If manual instrumentation is used before initialization, the original `OkHttpClient` is returned and a warning is logged.
- Captured request and response headers are opt-in through configuration.
- Unknown payload sizes, including negative HTTP length values, are omitted.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    OkHttp3ManualModuleConfiguration(
        capturedRequestHeaders = listOf("x-request-id"),
        capturedResponseHeaders = listOf("server-timing")
    )
)
```

```kotlin
val client = OkHttpClient.Builder().build()
val callFactory = SplunkRum.instance.okHttpManualInstrumentation
    .buildOkHttpCallFactory(client)
```

## API Documentation

### `OkHttp3ManualModuleConfiguration`

Configures manual OkHttp request tracking.

```kotlin
OkHttp3ManualModuleConfiguration(
    capturedRequestHeaders: List<String> = emptyList(),
    capturedResponseHeaders: List<String> = emptyList()
)
```

- `capturedRequestHeaders`: Request headers to capture as normalized `http.request.header.<name>` attributes.
- `capturedResponseHeaders`: Response headers to capture as normalized `http.response.header.<name>` attributes.
- `name`: Module name reported as `okHttp3-manual`.
- `attributes`: Module attributes containing `requestHeaders` and `responseHeaders`.

### `OkHttpManualInstrumentation`

Entry point for wrapping OkHttp clients.

```kotlin
OkHttpManualInstrumentation.instance
```

- Kotlin: `OkHttpManualInstrumentation.instance` or `SplunkRum.instance.okHttpManualInstrumentation`.
- Java: `OkHttpManualInstrumentation.getInstance()`.

### `SplunkRum.okHttpManualInstrumentation`

Kotlin extension property for accessing manual OkHttp instrumentation from a `SplunkRum` instance.

```kotlin
val SplunkRum.okHttpManualInstrumentation: OkHttpManualInstrumentation
```

### `OkHttpManualInstrumentation.buildOkHttpCallFactory(...)`

Wraps an `OkHttpClient` with OpenTelemetry interceptors.

```kotlin
fun buildOkHttpCallFactory(client: OkHttpClient): Call.Factory
```

- `client`: Base OkHttp client.
- Returns an instrumented `Call.Factory`, or the original client when manual instrumentation is not initialized.

## Telemetry Data Model

The exported telemetry is HTTP client spans created by OpenTelemetry OkHttp instrumentation.

| Signal | Name | When emitted |
|---|---|---|
| Span | `<HTTP method>` | A request is executed through the instrumented `Call.Factory`. |

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
