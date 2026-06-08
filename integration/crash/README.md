# Crash Reporting Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- JVM uncaught exception handling.
- App lifecycle services for app state attributes.

## Overview

Crash Reporting captures uncaught exceptions and reports crash telemetry before delegating to the previous uncaught exception handler.

Detection sources:

- OpenTelemetry Android `CrashReporterInstrumentation`.
- The process default `Thread.UncaughtExceptionHandler`.
- Application foreground/background lifecycle callbacks.

Detection behavior:

- The module is enabled by default.
- When disabled, crash reporting is not installed.
- The first uncaught exception is marked with `component = crash`; additional concurrent exceptions are marked with `component = error`.
- The crash reporter force flushes the SDK logger provider for up to 10 seconds before delegating to the existing exception handler.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    CrashModuleConfiguration(
        isEnabled = true
    )
)
```

## API Documentation

### `CrashModuleConfiguration`

Configures crash reporting.

```kotlin
CrashModuleConfiguration(
    isEnabled: Boolean = true
)
```

- `isEnabled`: Enables or disables crash reporting.
- `name`: Module name reported as `crash`.
- `attributes`: Module attributes containing `enabled`.

## Telemetry Data Model

The upstream crash reporter emits a `device.crash` log event. The SDK exports it as an internal span.

| Signal | Name | When emitted |
|---|---|---|
| Span | `device.crash` | An uncaught exception reaches the default exception handler. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `crash` for the first crash event, `error` for additional concurrent crash details. |
| `error` | string | Yes | `true` |
| `android.app.state` | string | No | Last known app state: `created`, `foreground`, or `background`. |
| `exception.type` | string | Yes | Throwable class name. |
| `exception.message` | string | No | Throwable message. |
| `exception.stacktrace` | string | Yes | Throwable stack trace. |
| `thread.id` | long | Yes | Crashing thread ID. |
| `thread.name` | string | Yes | Crashing thread name. |

Resource / scope:

- Source log instrumentation scope name: `io.opentelemetry.crash`
- Exported span instrumentation scope name: `SplunkRum`
- Span kind: `INTERNAL`
- Span duration: zero-length
