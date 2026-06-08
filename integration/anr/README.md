# ANR Detection Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Main thread `Looper` access.
- App lifecycle services from the OpenTelemetry Android runtime.

## Overview

ANR Detection reports Application Not Responding conditions when the main thread is unresponsive for five consecutive one-second polls.

Detection sources:

- OpenTelemetry Android `AnrInstrumentation`.
- Main thread polling through a `Handler` posted to the main `Looper`.
- Application foreground/background lifecycle callbacks.

Detection behavior:

- The module is enabled by default.
- When disabled, the ANR detector is not installed.
- ANR spans are marked as errors and include the main thread stack trace from the upstream instrumentation.
- Splunk adds `component`, `error`, and the latest known app state when available.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    AnrModuleConfiguration(
        isEnabled = true
    )
)
```

## API Documentation

### `AnrModuleConfiguration`

Configures the ANR module.

```kotlin
AnrModuleConfiguration(
    isEnabled: Boolean = true
)
```

- `isEnabled`: Enables or disables ANR detection.
- `name`: Module name reported as `anr`.
- `attributes`: Module attributes containing `enabled`.

## Telemetry Data Model

The exported telemetry is an error span created by OpenTelemetry Android ANR instrumentation.

| Signal | Name | When emitted |
|---|---|---|
| Span | `ANR` | Main thread is unresponsive for at least five seconds. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `anr` |
| `error` | string | Yes | `true` |
| `android.app.state` | string | No | Last known app state: `created`, `foreground`, or `background`. |
| `exception.stacktrace` | string | Yes | Main thread stack trace added by OpenTelemetry Android. |

Resource / scope:

- Instrumentation scope name: `io.opentelemetry.anr`
- Span kind: `INTERNAL`
- Span status: `ERROR`
