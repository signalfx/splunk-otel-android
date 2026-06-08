# Network Monitor Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Android network state services from the OpenTelemetry Android runtime.

## Overview

Network Monitor tracks network connectivity changes and keeps current network attributes available for spans emitted by the SDK.

Detection sources:

- OpenTelemetry Android `NetworkChangeInstrumentation`.
- OpenTelemetry Android `CurrentNetworkProvider` network change callbacks.
- Application foreground/background lifecycle callbacks.

Detection behavior:

- The module is enabled by default.
- Network change events are emitted only while the app is foregrounded.
- Current network attributes are stored as global span attributes and updated on network changes.
- `network.connection.type` is initialized to `unavailable` so spans have a default value before the first network callback.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    NetworkMonitorModuleConfiguration(
        isEnabled = true
    )
)
```

## API Documentation

### `NetworkMonitorModuleConfiguration`

Configures network monitoring.

```kotlin
NetworkMonitorModuleConfiguration(
    isEnabled: Boolean = true
)
```

- `isEnabled`: Enables or disables network change telemetry and global network attributes.
- `name`: Module name reported as `networkMonitor`.
- `attributes`: Module attributes containing `enabled`.

## Telemetry Data Model

The upstream instrumentation emits network change log records that the SDK exports as zero-length internal spans. The module also adds current network attributes to other spans through the global attribute processor.

| Signal | Name | When emitted |
|---|---|---|
| Span | `network.change` | Network state changes while the app is foregrounded. |
| Span attributes | N/A | Added to SDK spans after the current network state is known. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `network.status` | string | Yes for `network.change` | `available` or `lost`. |
| `network.connection.type` | string | Yes | Current network state, initialized as `unavailable`. |
| `network.connection.subtype` | string | No | Current network subtype. |
| `network.carrier.name` | string | No | Mobile carrier name. |
| `network.carrier.mcc` | string | No | Mobile country code. |
| `network.carrier.mnc` | string | No | Mobile network code. |
| `network.carrier.icc` | string | No | Carrier ISO country code. |

Resource / scope:

- Instrumentation scope name: `SplunkRum` for exported network change spans
- Span kind: `INTERNAL`
- Span duration: zero-length
