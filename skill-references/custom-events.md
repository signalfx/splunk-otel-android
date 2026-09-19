# Custom Events & Actions — Splunk Android RUM SDK

> **Load when:** User wants to track business events, timed workflows, or handled exceptions beyond what the SDK captures automatically.
>
> **Do not load when:** User only needs default instrumentation (crash, ANR, lifecycle, network) with no custom business logic tracking.
>
> **Source files to verify:**
> - `integration/customtracking/src/main/kotlin/com/splunk/rum/integration/customtracking/CustomTracking.kt` — `trackCustomEvent()`, `trackWorkflow()`, `trackException()`

The `CustomTracking` API lets you capture business events, timed workflows, and handled
exceptions as OpenTelemetry spans sent to Splunk RUM.

---

## Accessing the API

```kotlin
import com.splunk.rum.integration.customtracking.extension.customTracking

val tracking = SplunkRum.instance.customTracking
```

Java:
```java
import com.splunk.rum.integration.customtracking.CustomTracking;

CustomTracking tracking = CustomTracking.getInstance();
```

> `CustomTracking` is a lazy singleton. No `ModuleConfiguration` needed — it activates
> automatically when the SDK is installed.

---

## Three Methods, Three Use Cases

### 1. `trackCustomEvent()` — Point-in-Time Business Events

Creates a zero-duration span marking that something happened.

```kotlin
// Simple event
tracking.trackCustomEvent("user_signed_up")

// With attributes
tracking.trackCustomEvent(
    "purchase_completed",
    Attributes.of(
        AttributeKey.stringKey("product.id"), "sku-123",
        AttributeKey.doubleKey("order.total"), 49.99,
        AttributeKey.stringKey("payment.method"), "credit_card"
    )
)
```

**When to use:** Discrete business milestones — sign-ups, purchases, feature flags toggled,
onboarding steps completed, A/B test enrollments. The event has no duration; it records
*that* something happened, not *how long* it took.

### 2. `trackWorkflow()` — Timed User Workflows

Starts a span that you manually end, measuring elapsed time.

```kotlin
// Start timing
val span = tracking.trackWorkflow("checkout_flow")

// ... user completes checkout steps ...

// End timing (records duration)
span?.end()
```

With error recording:
```kotlin
val span = tracking.trackWorkflow("image_upload")
try {
    uploadImage(file)
    span?.end()  // success
} catch (e: Exception) {
    span?.recordException(e)
    span?.end()  // ended with error info
}
```

**When to use:** Multi-step user journeys where duration matters — checkout flows, onboarding
wizards, form submissions, file uploads, search queries. The span measures wall-clock time
between `trackWorkflow()` and `span.end()`.

### 3. `trackException()` — Handled Exceptions

Creates a zero-duration error span for exceptions your app catches and handles.

```kotlin
try {
    parseUserInput(input)
} catch (e: NumberFormatException) {
    tracking.trackException(e)
    showErrorToUser("Invalid number")
}

// With attributes
try {
    syncData()
} catch (e: IOException) {
    tracking.trackException(
        e,
        Attributes.of(
            AttributeKey.stringKey("sync.endpoint"), "users",
            AttributeKey.longKey("sync.retry_count"), 3L
        )
    )
}
```

**When to use:** Exceptions that are caught and handled gracefully but you still want
visibility into in Splunk. Unhandled exceptions are captured automatically by the crash
module — this is for *handled* errors.

---

## When to Use What — Decision Guide

| Scenario | Method | Why |
|----------|--------|-----|
| User taps "Buy Now" | `trackCustomEvent("purchase_initiated")` | Point-in-time event, no duration |
| User completes checkout (3 steps) | `trackWorkflow("checkout_flow")` | Duration matters — measures the full flow |
| Payment API returns 402 | `trackException(paymentError)` | Handled error worth tracking |
| Feature flag evaluated | `trackCustomEvent("feature_flag_evaluated")` | Point-in-time, include flag name/value as attributes |
| Image upload start → finish | `trackWorkflow("image_upload")` | Timed operation |
| JSON parse failure (non-fatal) | `trackException(parseError)` | Handled gracefully, but want visibility |
| Search query executed | `trackCustomEvent("search")` + attributes | If you only care that it happened |
| Search query timing | `trackWorkflow("search")` | If you care how long it took |
| User reached onboarding step 3 | `trackCustomEvent("onboarding_step")` with step attribute | Milestone event |
| User completed full onboarding | `trackWorkflow("onboarding_flow")` | Measure total onboarding time |

---

## Best Practices

### Naming Conventions
Use `snake_case` for event/workflow names and keep them stable:
```kotlin
// Good — stable, queryable
trackCustomEvent("cart_item_added")
trackWorkflow("checkout_flow")

// Bad — dynamic, creates high-cardinality span names
trackCustomEvent("added_${product.name}_to_cart")
trackWorkflow("checkout_for_user_${userId}")
```

Put variable data in attributes, not in the event name.

### Attributes
Use typed `AttributeKey` values:
```kotlin
Attributes.of(
    AttributeKey.stringKey("product.category"), "electronics",
    AttributeKey.longKey("cart.item_count"), 3L,
    AttributeKey.doubleKey("cart.total"), 149.97,
    AttributeKey.booleanKey("user.is_premium"), true
)
```

### Workflow Spans — Always End Them
A `trackWorkflow()` span that is never ended leaks and eventually times out. Use try/finally:
```kotlin
val span = tracking.trackWorkflow("data_sync")
try {
    performSync()
} finally {
    span?.end()
}
```

### Don't Double-Track
- Crashes and unhandled exceptions are already captured by the crash module
- Activity/Fragment lifecycle is captured by the lifecycle module
- HTTP requests are captured by the network instrumentation
- Screen transitions are captured by the navigation module

Use custom tracking for business logic that the SDK can't automatically observe.
