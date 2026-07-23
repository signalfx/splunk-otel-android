# Screen & Navigation Tracking — Splunk Android RUM SDK

> **Load when:** App has multiple screens (Activities, Fragments, or Compose destinations) and the user wants screen-level analytics.
>
> **Do not load when:** Single-screen utility app or user explicitly declines navigation tracking.
>
> **Source files to verify:**
> - `integration/navigation/src/main/kotlin/com/splunk/rum/integration/navigation/NavigationModuleConfiguration.kt` — config options
> - `integration/navigation/src/main/kotlin/com/splunk/rum/integration/navigation/Navigation.kt` — `track()`, `registerNavController()`

The navigation module emits `app.ui.navigation` OpenTelemetry events for screen arrivals.
It supports automatic tracking (Activity/Fragment/Compose Navigation) and manual tracking.

---

## Module Configuration

```kotlin
NavigationModuleConfiguration(
    isEnabled = true,                     // module on (default)
    isAutomatedTrackingEnabled = true,    // auto-track Activity/Fragment screens (default: false)
    navigationEventProcessor = null       // optional: transform/filter Compose route events
)
```

Pass to `SplunkRum.install()`:
```kotlin
SplunkRum.install(this, agentConfig,
    NavigationModuleConfiguration(isEnabled = true, isAutomatedTrackingEnabled = true)
)
```

---

## Automatic Tracking

### Activity/Fragment (Views-based apps)

Set `isAutomatedTrackingEnabled = true`. The SDK uses `onActivityResumed` and `onFragmentResumed`
callbacks to detect screen arrivals. Screen names default to the Activity/Fragment class simple name.

No additional code needed beyond the module configuration.

### Compose Navigation

For apps using Jetpack Compose with `NavController`, register the controller after `SplunkRum.install()`:

```kotlin
import com.splunk.rum.integration.navigation.extension.navigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            LaunchedEffect(navController) {
                SplunkRum.instance.navigation.registerNavController(navController)
            }

            NavHost(navController, startDestination = "home") {
                composable("home") { HomeScreen() }
                composable("details/{id}") { DetailScreen() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister is optional but good practice
        // SplunkRum.instance.navigation.unregisterNavController(navController)
    }
}
```

Only one `NavController` can be tracked at a time; registering a new one replaces the previous.

### Navigation Event Processor (Compose routes)

Compose routes often contain path parameters (e.g., `details/123`). Use a `NavigationEventProcessor`
to normalize or filter route names before they become screen names:

```kotlin
NavigationModuleConfiguration(
    isEnabled = true,
    isAutomatedTrackingEnabled = true,
    navigationEventProcessor = NavigationEventProcessor { event ->
        // Normalize parameterized routes
        event.name = event.name.replace(Regex("/\\d+"), "/{id}")
        event  // return the event to emit it, or null to suppress it
    }
)
```

---

## Manual Tracking

For custom screen tracking or when automatic detection doesn't match your navigation pattern:

```kotlin
import com.splunk.rum.integration.navigation.extension.navigation

// Track a screen arrival
SplunkRum.instance.navigation.track("CheckoutScreen")

// With attributes
SplunkRum.instance.navigation.track(
    "ProductDetail",
    Attributes.of(AttributeKey.stringKey("product.id"), "abc-123")
)
```

Manual and automatic tracking can coexist. Use manual tracking for:
- Custom navigation frameworks (not Activity/Fragment/Compose NavController)
- Bottom sheet or dialog screens that don't trigger lifecycle callbacks
- Virtual screens within a single Activity

---

## Choosing a Strategy

| App Architecture | Recommended Approach |
|-----------------|---------------------|
| Multi-Activity | Auto tracking (`isAutomatedTrackingEnabled = true`) |
| Single-Activity + Fragments | Auto tracking |
| Compose + NavController | Auto tracking + `registerNavController()` |
| Compose without NavController | Manual `navigation.track()` calls |
| Custom navigation framework | Manual `navigation.track()` calls |
| Mixed (Views + Compose) | Auto tracking for Activities/Fragments + `registerNavController()` for Compose |

---

## Accessing the Navigation API

The `navigation` property is an extension on `SplunkRum`:
```kotlin
import com.splunk.rum.integration.navigation.extension.navigation

val nav = SplunkRum.instance.navigation
nav.track("MyScreen")
nav.registerNavController(navController)
nav.unregisterNavController(navController)
```

Java:
```java
import com.splunk.rum.integration.navigation.Navigation;

Navigation.getInstance().track("MyScreen");
```
