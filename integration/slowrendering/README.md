# Slow and Frozen Rendering Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Slow and frozen rendering detection itself requires Android API level 24+ because it uses `Window.OnFrameMetricsAvailableListener`.
- Core library desugaring for `java.time.Duration` on Android API levels below 26.

## Overview

Slow and Frozen Rendering Instrumentation reports UI frames that exceed slow or frozen rendering thresholds.

Detection sources:

- OpenTelemetry Android `SlowRenderingInstrumentation`.
- Activity resume and pause callbacks.
- Android frame metrics from each resumed Activity window.

Detection behavior:

- The module is enabled by default.
- Slow frames are draw durations greater than 16 ms.
- Frozen frames are draw durations greater than 700 ms.
- First draw frames are ignored.
- Metrics are polled every `interval`, defaulting to one second.
- Below Android API level 24, the upstream detector logs that the platform is unsupported and does not install frame metrics collection.
- If `interval` is not positive, the upstream detector logs an error and keeps its current interval.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    SlowRenderingModuleConfiguration(
        isEnabled = true,
        interval = Duration.ofSeconds(1)
    )
)
```

## API Documentation

### `SlowRenderingModuleConfiguration`

Configures slow and frozen rendering detection.

```kotlin
SlowRenderingModuleConfiguration(
    isEnabled: Boolean = true,
    interval: Duration = Duration.ofSeconds(1)
)
```

- `isEnabled`: Enables or disables slow and frozen rendering detection.
- `interval`: Polling interval for frame metrics.
- `name`: Module name reported as `slowrendering`.
- `attributes`: Module attributes containing `enabled` and `interval`.

## Telemetry Data Model

The exported telemetry is zero-length spans created by OpenTelemetry Android slow rendering instrumentation.

| Signal | Name | When emitted |
|---|---|---|
| Span | `slowRenders` | One or more frames exceed 16 ms and do not exceed 700 ms during a polling window. |
| Span | `frozenRenders` | One or more frames exceed 700 ms during a polling window. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `count` | long | Yes | Number of slow or frozen frames in the polling window. |
| `activity.name` | string | Yes | Short flattened Activity component name. |

Resource / scope:

- Instrumentation scope name: `io.opentelemetry.slow-rendering`
- Span kind: `INTERNAL`
- Span duration: zero-length
