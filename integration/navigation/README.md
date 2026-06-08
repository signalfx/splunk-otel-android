# Navigation Detection Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- AndroidX Fragment APIs for automatic Fragment tracking.
- AndroidX Navigation 2.4.0+ for Compose navigation tracking.

## Overview

Navigation Detection tracks visible screen changes and emits a RUM navigation event when the current screen changes.

Detection sources:

- Manual calls through `Navigation.track(...)`.
- Activity resume and pause callbacks.
- Fragment resume and pause callbacks.
- Jetpack Compose `NavController` destination changes.

Detection behavior:

- The module is enabled by default, but automatic Activity and Fragment tracking is disabled by default.
- Visible screen priority is Compose route, then Fragment, then Activity.
- Screen names are resolved from `@NavigationElement`, then `@RumScreenName`, then the class simple name.
- `DialogFragment`, AndroidX `NavHost` implementations, and elements annotated with `isIgnored = true` are ignored.
- Compose destinations ignore `NavGraph` containers, dialog destinations, and internal navigation arguments.
- Compose events are deduplicated by screen name and attributes, so the same route template with different arguments still emits.
- Events observed before install completes are cached and emitted after install.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    NavigationModuleConfiguration(
        isEnabled = true,
        isAutomatedTrackingEnabled = true
    )
)
```

```kotlin
SplunkRum.instance.navigation.track("checkout")
```

```kotlin
val navController = rememberNavController()
SplunkRum.instance.navigation.registerNavController(navController)
```

```kotlin
@NavigationElement(name = "Checkout")
class CheckoutActivity : AppCompatActivity()
```

## API Documentation

### `NavigationModuleConfiguration`

Configures navigation tracking.

```kotlin
NavigationModuleConfiguration(
    isEnabled: Boolean = true,
    isAutomatedTrackingEnabled: Boolean = false,
    navigationEventProcessor: NavigationEventProcessor? = null
)
```

- `isEnabled`: Enables or disables navigation tracking. When disabled, manual tracking and Compose tracking are detached.
- `isAutomatedTrackingEnabled`: Enables automatic Activity and Fragment screen detection.
- `navigationEventProcessor`: Optional processor for transforming or suppressing Compose route events.
- `name`: Module name reported as `navigation`.
- `attributes`: Module attributes containing `enabled` and `isAutomatedTrackingEnabled`.

### `Navigation`

Entry point for manual navigation tracking and Compose `NavController` registration.

```kotlin
Navigation.instance
```

- Kotlin: `Navigation.instance` or `SplunkRum.instance.navigation`.
- Java: `Navigation.getInstance()`.

### `SplunkRum.navigation`

Kotlin extension property for accessing navigation tracking from a `SplunkRum` instance.

```kotlin
val SplunkRum.navigation: Navigation
```

### `Navigation.track(...)`

Records a manual navigation event.

```kotlin
fun track(
    screenName: String,
    attributes: Attributes = Attributes.empty()
)
```

- `screenName`: Screen name to report.
- `attributes`: Optional OpenTelemetry attributes attached to the emitted event.

### `Navigation.registerNavController(...)`

Registers one Jetpack Compose `NavController` for automatic route tracking.

```kotlin
fun registerNavController(navController: NavController)
```

- `navController`: Controller whose destination changes should be tracked.

Only one controller is tracked at a time. Registering a new controller replaces the previous one. Passing the same controller again is a no-op.

### `Navigation.unregisterNavController(...)`

Unregisters a previously registered Jetpack Compose `NavController`.

```kotlin
fun unregisterNavController(navController: NavController)
```

- `navController`: Controller to unregister.

The destination listener is removed only when the supplied controller is the currently registered controller.

### `NavigationEventProcessor`

Processes Compose route navigation events before they are emitted.

```kotlin
fun interface NavigationEventProcessor {
    fun process(event: NavigationEvent): NavigationEvent?
}
```

Return the event to emit it, modify it before emission, or return `null` to suppress it.

### `NavigationEvent`

Mutable event object passed to `NavigationEventProcessor`.

```kotlin
class NavigationEvent(
    var name: String,
    val attributes: MutableMap<String, String>,
    val sourceType: SourceType
)
```

- `name`: Screen name to emit. Processors can modify this value.
- `attributes`: String attributes to emit. Processors can add, remove, or change entries.
- `sourceType`: Event origin.

### `NavigationEvent.SourceType`

Identifies the source of a navigation event.

```kotlin
enum class SourceType {
    ACTIVITY,
    FRAGMENT,
    COMPOSE_ROUTE
}
```

### `@NavigationElement`

Overrides or ignores Activity and Fragment screen detection.

```kotlin
@NavigationElement(name = "Checkout", isIgnored = false)
```

`@NavigationElement` takes precedence over `@RumScreenName`.

### `@RumScreenName`

Legacy annotation for overriding or ignoring Activity and Fragment screen detection.

```kotlin
@RumScreenName(name = "Checkout", isIgnored = false)
```

## Telemetry Data Model

The module emits log records that the SDK exports as zero-length internal spans.

| Signal | Name | When emitted |
|---|---|---|
| Span | `app.ui.navigation` | Visible screen changes. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `component` | string | Yes | `ui` |
| `navigation.name` | string | Yes | Destination screen name. |
| `screen.name` | string | Yes | Current screen name. |
| `last.screen.name` | string | No | Previous screen name when known. |
| `nav.graph` | string | No | Compose parent graph route. |
| `<route argument>` | string | No | Compose route argument. |
| `<custom attribute>` | any OTel type | No | Manual tracking attribute. |

Resource / scope:

- Instrumentation scope name: `SplunkRum`
- Span kind: `INTERNAL`
- Span duration: zero-length
