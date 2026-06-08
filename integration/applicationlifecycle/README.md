# Application Lifecycle Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Android `Application` lifecycle callbacks.

## Overview

Application Lifecycle tracks app-level state transitions and emits telemetry when the application is created, foregrounded, or backgrounded.

Detection sources:

- `AppStateObserver` callbacks attached to the `Application`.
- Cached lifecycle events observed before the logger provider is ready.

Detection behavior:

- The module is enabled by default.
- Events observed before install completes are cached and emitted after install when the module is enabled.
- When disabled, cached events are cleared and no lifecycle telemetry is emitted.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    ApplicationLifecycleModuleConfiguration(
        isEnabled = true
    )
)
```

## API Documentation

### `ApplicationLifecycleModuleConfiguration`

Configures app-level lifecycle tracking.

```kotlin
ApplicationLifecycleModuleConfiguration(
    isEnabled: Boolean = true
)
```

- `isEnabled`: Enables or disables application lifecycle telemetry.
- `name`: Module name reported as `applicationLifecycle`.
- `attributes`: Module attributes containing `enabled`.

## Telemetry Data Model

The module emits log records that the SDK exports as zero-length internal spans.

| Signal | Name | When emitted |
|---|---|---|
| Span | `device.app.lifecycle` | App state changes to created, foreground, or background. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `app-lifecycle` |
| `android.app.state` | string | Yes | App state: `created`, `foreground`, or `background`. |

Resource / scope:

- Instrumentation scope name: `SplunkRum`
- Span kind: `INTERNAL`
- Span duration: zero-length
