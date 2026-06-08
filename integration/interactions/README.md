# User Interaction Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Session recording interaction and frame capture runtime.
- Compose UI is optional; Compose element identification is installed only when Compose UI is present.

## Overview

User Interaction Instrumentation reports selected user actions and rage tap frustration events.

Detection sources:

- Session recording `Interactions` listeners.
- Session recording `FrameCapturer` wireframes used to resolve target paths.
- Optional Compose modifiers inserted through internal Compose element identification.

Detection behavior:

- The module is enabled by default.
- When disabled, attached listeners stay registered but do not emit telemetry.
- Pointer events, rage taps in the normal interaction stream, non-final continuous touch events, orientation changes, and swipe gestures are filtered from `action` telemetry.
- Rage taps are emitted separately as `frustration` telemetry.
- Target XPath values are built from the target element path and may include user-provided IDs when available.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    InteractionsModuleConfiguration(
        isEnabled = true
    )
)
```

## API Documentation

### `InteractionsModuleConfiguration`

Configures user interaction telemetry.

```kotlin
InteractionsModuleConfiguration(
    isEnabled: Boolean = true
)
```

- `isEnabled`: Enables or disables emitted interaction and frustration telemetry.
- `name`: Module name reported as `interactions`.
- `attributes`: Module attributes containing `enabled`.

## Telemetry Data Model

The module emits log records that the SDK exports as zero-length internal spans.

| Signal | Name | When emitted |
|---|---|---|
| Span | `action` | A supported user interaction is observed. |
| Span | `frustration` | A rage tap interaction is observed. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `ui` for `action`, `user-interaction` for `frustration`. |
| `action.name` | string | Yes for `action` | One of `focus`, `soft_keyboard`, `phone_button`, `double_tap`, `long_press`, `pinch`, `rotation`, or `tap`. |
| `target.type` | string | Yes for `action` | Target view ID when available, otherwise an empty string. |
| `target_xpath` | string | No | XPath-like target path for targetable interactions. |
| `target_element` | string | No | Target element type name for targetable interactions. |
| `frustration_type` | string | Yes for `frustration` | `rage` |
| `interaction_type` | string | Yes for `frustration` | `tap` |

Resource / scope:

- Instrumentation scope name: `SplunkRum`
- Span kind: `INTERNAL`
- Span duration: zero-length
