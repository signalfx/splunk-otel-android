# <Instrumentation Name> Instrumentation

## Requirements

- <Minimum Android API level and any opt-in lower API support.>
- <Required AndroidX, Compose, Gradle plugin, or platform APIs.>
- <Any module-specific setup requirements.>

## Overview

<Describe what this instrumentation tracks and what user-visible SDK behavior it enables.>

Detection sources:

- <Manual API calls, if supported.>
- <Android framework callbacks or listeners, if used.>
- <Bytecode instrumentation hooks, registered controllers, platform APIs, or other inputs.>

Detection behavior:

- <Source priority when more than one source can report the same behavior.>
- <Filtering, ignored elements, suppression rules, or deduplication rules.>
- <Default enabled or disabled state.>
- <Fallback or degraded behavior when optional APIs, modules, or runtime conditions are unavailable.>

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    <InstrumentationModuleConfiguration>(
        isEnabled = true
    )
)
```

```kotlin
<Minimal manual or advanced usage example, if applicable.>
```

## API Documentation

### `<PublicConfigurationOrEntryPoint>`

<Short purpose statement.>

```kotlin
<Constructor, function, property, annotation, or interface signature.>
```

- `<parameterOrProperty>`: <Behavior, default, and effect.>

### `<PublicFunctionOrType>`

<Repeat for public APIs exposed by the instrumentation. Omit private/internal implementation details unless needed to explain behavior.>

## Telemetry Data Model

<State what telemetry is exported. If the instrumentation does not emit telemetry directly, say what it delegates to.>

| Signal | Name | When emitted |
|---|---|---|
| <Span/Event/Metric/Log> | `<name>` | <Condition.> |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `<attribute.name>` | <string/boolean/long/double/OTel type> | <Yes/No> | <Description.> |
| `<custom attribute>` | any OTel type | No | <When custom attributes are accepted.> |

Resource / scope:

- Instrumentation scope name: `<scope name>`
- Span kind: `<INTERNAL/CLIENT/SERVER/etc.>`
- Span duration: `<zero-length/measured duration/etc.>`
