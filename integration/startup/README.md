# App Startup Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- The startup runtime `ContentProvider` must initialize before the first Activity draw.
- Android Activity lifecycle callbacks and root view draw/pre-draw callbacks.

## Overview

App Startup Instrumentation measures cold, warm, and hot application starts and emits startup spans.

Detection sources:

- Startup runtime `ContentProvider`.
- `ApplicationStartupTimekeeper` Activity lifecycle callbacks.
- First root view draw or pre-draw callback, depending on Android API level.
- Module initialization timing recorded by the agent.

Detection behavior:

- The module is always reported as enabled through `StartupModuleConfiguration`.
- Only one startup event is reported per process initialization.
- Cold start duration is measured from process start to first draw.
- Warm start duration is measured from first Activity create to first draw.
- Hot start duration is measured from first Activity start to first draw.
- Startup events observed before the module listener is attached are cached and delivered later.
- The app start span sets `screen.name` to `unknown` so startup telemetry is not associated with a visible screen.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    StartupModuleConfiguration()
)
```

The startup module is included by default when the agent installs its default module set.

## API Documentation

### `StartupModuleConfiguration`

Configures startup instrumentation.

```kotlin
StartupModuleConfiguration()
```

- `name`: Module name reported as `startup`.
- `attributes`: Module attributes containing `enabled = true`.

## Telemetry Data Model

The exported telemetry is startup spans created by the Splunk RUM tracer.

| Signal | Name | When emitted |
|---|---|---|
| Span | `AppStart` | The first cold, warm, or hot startup event is measured. |
| Span | `SplunkRum.initialize` | Agent module initialization timing is reported as a child of `AppStart`. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `appstart` |
| `screen.name` | string | Yes | `unknown` |
| `start.type` | string | Yes on `AppStart` | `cold`, `warm`, or `hot`. |
| `config_settings` | string | Yes on `SplunkRum.initialize` | Serialized module configuration settings. |

Span events:

- `<module>_initialized`: Initialization duration event added to `SplunkRum.initialize` for each initialized module.

Resource / scope:

- Instrumentation scope name: `SplunkRum`
- Span kind: `INTERNAL`
- Span duration: measured startup or initialization duration
