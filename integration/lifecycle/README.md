# UI Lifecycle Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Android `Application.ActivityLifecycleCallbacks`.
- AndroidX Fragment APIs for Fragment lifecycle tracking.

## Overview

UI Lifecycle Instrumentation tracks Activity and Fragment lifecycle transitions and emits `app.ui.lifecycle` telemetry.

Detection sources:

- Activity lifecycle callbacks registered on the `Application`.
- AndroidX Fragment lifecycle callbacks registered for `FragmentActivity` instances.
- API 29+ pre/post Activity callbacks when available.

Detection behavior:

- The module is enabled by default.
- Only configured `allowedEvents` are emitted.
- The default event set is `MAIN_LIFECYCLE_EVENTS`.
- Events observed before install completes are cached and emitted after install.
- If the logger provider is unavailable after install, the event is skipped.
- Fragment callbacks are registered on Activity create for API 21-28 and Activity pre-create for API 29+.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    LifecycleModuleConfiguration(
        isEnabled = true,
        allowedEvents = LifecycleModuleConfiguration.MAIN_LIFECYCLE_EVENTS
    )
)
```

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    LifecycleModuleConfiguration(
        allowedEvents = LifecycleModuleConfiguration.ALL_LIFECYCLE_EVENTS
    )
)
```

## API Documentation

### `LifecycleModuleConfiguration`

Configures Activity and Fragment lifecycle telemetry.

```kotlin
LifecycleModuleConfiguration(
    isEnabled: Boolean = true,
    allowedEvents: Set<LifecycleAction> = MAIN_LIFECYCLE_EVENTS
)
```

- `isEnabled`: Enables or disables lifecycle telemetry.
- `allowedEvents`: Lifecycle actions that should be emitted.
- `name`: Module name reported as `lifecycle`.
- `attributes`: Module attributes containing `enabled` and `allowedEvents`.

### `LifecycleModuleConfiguration.MAIN_LIFECYCLE_EVENTS`

Default event set.

```kotlin
val MAIN_LIFECYCLE_EVENTS: Set<LifecycleAction>
```

Contains `CREATED`, `STARTED`, `RESUMED`, `PAUSED`, `STOPPED`, `DESTROYED`, `ATTACHED`, `VIEW_CREATED`, `VIEW_DESTROYED`, and `DETACHED`.

### `LifecycleModuleConfiguration.PRE_POST_LIFECYCLE_EVENTS`

Pre/post Activity event set.

```kotlin
val PRE_POST_LIFECYCLE_EVENTS: Set<LifecycleAction>
```

Contains API 29+ Activity pre/post lifecycle actions and `PRE_ATTACHED`.

### `LifecycleModuleConfiguration.ALL_LIFECYCLE_EVENTS`

All lifecycle actions.

```kotlin
val ALL_LIFECYCLE_EVENTS: Set<LifecycleAction>
```

Equivalent to all `LifecycleAction` enum values.

### `LifecycleAction`

Lifecycle action values emitted as `lifecycle.action`.

```kotlin
enum class LifecycleAction(val attributeValue: String)
```

Values include `CREATED`, `STARTED`, `RESUMED`, `PAUSED`, `STOPPED`, `DESTROYED`, pre/post Activity variants, and Fragment-specific `ATTACHED`, `DETACHED`, `VIEW_CREATED`, and `VIEW_DESTROYED`.

## Telemetry Data Model

The module emits log records that the SDK exports as zero-length internal spans.

| Signal | Name | When emitted |
|---|---|---|
| Span | `app.ui.lifecycle` | A configured Activity or Fragment lifecycle action is observed. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `ui` |
| `element.type` | string | Yes | `Activity` or `Fragment`. |
| `element.name` | string | Yes | Simple class name. |
| `element.id` | string | Yes | Fully qualified class name. |
| `lifecycle.action` | string | Yes | Lifecycle action value such as `created`, `resumed`, or `view_destroyed`. |

Resource / scope:

- Instrumentation scope name: `SplunkRum`
- Span kind: `INTERNAL`
- Span duration: zero-length
