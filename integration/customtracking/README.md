# Custom Tracking Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- `SplunkRum.install(...)` must complete before custom telemetry can be emitted.

## Overview

Custom Tracking lets applications create custom RUM events, workflow spans, and exception spans.

Detection sources:

- Manual calls through `CustomTracking`.
- Kotlin access through `SplunkRum.instance.customTracking`.
- Java access through `CustomTracking.getInstance()`.

Detection behavior:

- There is no module configuration for custom tracking.
- Calls made before the OpenTelemetry SDK is available are logged internally and ignored.
- Custom events and exceptions are zero-length spans.
- Workflows return a started span that the application is responsible for ending.

## Quick Start

```kotlin
SplunkRum.instance.customTracking.trackCustomEvent("checkout.started")
```

```kotlin
val workflow = SplunkRum.instance.customTracking.trackWorkflow("checkout")
try {
    // application work
} finally {
    workflow?.end()
}
```

```kotlin
SplunkRum.instance.customTracking.trackException(exception)
```

## API Documentation

### `CustomTracking`

Entry point for manual custom telemetry.

```kotlin
CustomTracking.instance
```

- Kotlin: `CustomTracking.instance` or `SplunkRum.instance.customTracking`.
- Java: `CustomTracking.getInstance()`.

### `SplunkRum.customTracking`

Kotlin extension property for accessing custom tracking from a `SplunkRum` instance.

```kotlin
val SplunkRum.customTracking: CustomTracking
```

### `CustomTracking.trackCustomEvent(...)`

Records a custom event as a zero-length span.

```kotlin
fun trackCustomEvent(
    name: String,
    attributes: Attributes = Attributes.empty()
)
```

- `name`: Span name for the custom event.
- `attributes`: Optional OpenTelemetry attributes attached to the span.

### `CustomTracking.trackWorkflow(...)`

Starts a span for a named workflow.

```kotlin
fun trackWorkflow(workflowName: String): Span?
```

- `workflowName`: Span name and `workflow.name` attribute value.
- Returns a started span, or `null` if the SDK tracer is unavailable.

### `CustomTracking.trackException(...)`

Records an exception as a zero-length error span and attaches exception details using `Span.recordException(...)`.

```kotlin
fun trackException(
    throwable: Throwable,
    attributes: Attributes? = null
)
```

- `throwable`: Exception to report.
- `attributes`: Optional OpenTelemetry attributes attached before exception recording.

## Telemetry Data Model

The exported telemetry is custom spans created from manual API calls.

| Signal | Name | When emitted |
|---|---|---|
| Span | `<custom event name>` | `trackCustomEvent(...)` is called. |
| Span | `<workflow name>` | `trackWorkflow(...)` is called. |
| Span | `<throwable simple class name>` | `trackException(...)` is called. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `custom-event`, `custom-workflow`, or `error`. |
| `workflow.name` | string | Yes for workflows | Workflow name passed to `trackWorkflow(...)`. |
| `error` | string | Yes for exceptions | `true` |
| `<custom attribute>` | any OTel type | No | Attributes supplied by the application. |
| `<exception attribute>` | string | Yes for exceptions | Exception attributes added by `Span.recordException(...)`. |

Resource / scope:

- Instrumentation scope name: `SplunkRum`
- Span kind: `INTERNAL`
- Span duration: zero-length for custom events and exceptions; application-controlled for workflows
