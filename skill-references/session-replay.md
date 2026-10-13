# Session Replay — Splunk Android RUM SDK

> **Load when:** User requests visual session recording, asks about Session Replay, or comprehensive depth is selected.
>
> **Do not load when:** User has not requested Session Replay and depth is baseline or targeted.
>
> **Hard Gate:** Never enable Session Replay without confirming privacy/sensitivity configuration with the user. Session Replay records visual content of user sessions.
>
> **Source files to verify:**
> - `integration/sessionreplay/src/main/kotlin/com/splunk/rum/integration/sessionreplay/SessionReplayModuleConfiguration.kt` — config options
> - `integration/sessionreplay/src/main/kotlin/com/splunk/rum/integration/sessionreplay/api/SessionReplay.kt` — `start()`, `stop()`, `preferences`, `sensitivity`, `recordingMask`
> - `integration/sessionreplay/src/main/kotlin/com/splunk/rum/integration/sessionreplay/api/RenderingMode.kt` — `NATIVE`, `WIREFRAME_ONLY`
> - `integration/sessionreplay/src/main/kotlin/com/splunk/rum/integration/sessionreplay/api/Sensitivity.kt` — class/instance sensitivity
> - `integration/agent/api/src/main/kotlin/com/splunk/rum/integration/agent/api/extension/ModifierExt.kt` — Compose `splunkRum()` modifier

Session Replay visually records user sessions for debugging and UX analysis. It is opt-in
and requires both configuration and an explicit `start()` call.

> **Min API:** 24 (same as the SDK; silently disabled on lower APIs)
> **Export format:** OTLP logs (not spans)
> **Rendering modes:** `NATIVE` (screenshots) or `WIREFRAME_ONLY` (structural wireframes)

---

## Setup

### 1. Configure the Module

Pass `SessionReplayModuleConfiguration` to `SplunkRum.install()`:

```kotlin
SessionReplayModuleConfiguration(
    isEnabled = true,        // allow recording to be started (default: true)
    samplingRate = 0.2f      // 20% of sessions can be recorded (default: 0.2)
)
```

### 2. Start Recording (Required)

Session replay does **not** start automatically. You must call `start()` after install:

```kotlin
import com.splunk.rum.integration.sessionreplay.extension.sessionReplay
import com.splunk.rum.integration.sessionreplay.api.RenderingMode

val agent = SplunkRum.install(this, agentConfig,
    SessionReplayModuleConfiguration(isEnabled = true, samplingRate = 0.5f)
)

// Configure rendering mode before starting
agent.sessionReplay.preferences.renderingMode = RenderingMode.NATIVE
agent.sessionReplay.start()
```

To stop recording:
```kotlin
SplunkRum.instance.sessionReplay.stop()
```

### 3. Full Example

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val agent = SplunkRum.install(
            this,
            AgentConfiguration(
                endpoint = EndpointConfiguration(
                    realm = BuildConfig.SPLUNK_REALM,
                    rumAccessToken = BuildConfig.SPLUNK_RUM_ACCESS_TOKEN
                ),
                appName = "MyApp",
                deploymentEnvironment = "prod"
            ),
            SessionReplayModuleConfiguration(
                isEnabled = true,
                samplingRate = if (BuildConfig.DEBUG) 1.0f else 0.2f
            )
        )

        agent.sessionReplay.preferences.renderingMode = RenderingMode.NATIVE
        agent.sessionReplay.start()
    }
}
```

---

## Rendering Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| `NATIVE` | Screenshots of what the user sees; sensitive views can be masked | Full visual fidelity |
| `WIREFRAME_ONLY` | Structural wireframe representation of the view hierarchy | Maximum privacy, lower bandwidth |

```kotlin
agent.sessionReplay.preferences.renderingMode = RenderingMode.NATIVE
// or
agent.sessionReplay.preferences.renderingMode = RenderingMode.WIREFRAME_ONLY
```

---

## Privacy & Sensitivity

### Default Sensitivity

`EditText` views are marked as sensitive by default. Sensitive views are covered/masked in
the recording.

### View Class Sensitivity

Mark all instances of a View class as sensitive:
```kotlin
val replay = SplunkRum.instance.sessionReplay

// Mark a custom view class as sensitive
replay.sensitivity.setViewClassSensitivity(CreditCardInputView::class.java, true)

// Mark a class as non-sensitive (override default)
replay.sensitivity.setViewClassSensitivity(PublicLabelView::class.java, false)

// Remove class-level override
replay.sensitivity.setViewClassSensitivity(SomeView::class.java, null)
```

### View Instance Sensitivity

Mark a specific view instance:
```kotlin
replay.sensitivity.setViewInstanceSensitivity(ssnField, true)
replay.sensitivity.setViewInstanceSensitivity(publicLabel, false)
```

Instance sensitivity overrides class sensitivity.

### Recording Masks

For precise control, define rectangular masks that cover or erase regions:
```kotlin
import com.splunk.rum.integration.sessionreplay.api.RecordingMask

replay.recordingMask = RecordingMask(
    elements = listOf(
        RecordingMask.Element(rect, RecordingMask.Type.COVERING),
        RecordingMask.Element(rect2, RecordingMask.Type.ERASING)
    )
)

// Clear masks
replay.recordingMask = null
```

### Compose Sensitivity

For Jetpack Compose, use the `Modifier.splunkRum()` extension:
```kotlin
import com.splunk.rum.integration.agent.api.extension.splunkRum

@Composable
fun PaymentForm() {
    Column {
        // Mark as sensitive — will be masked in recording
        TextField(
            value = cardNumber,
            onValueChange = { ... },
            modifier = Modifier.splunkRum(id = "card-number", isSensitive = true)
        )

        // Public content — explicitly not sensitive
        Text(
            text = "Total: $49.99",
            modifier = Modifier.splunkRum(id = "order-total", isSensitive = false)
        )
    }
}
```

The `splunkRum` modifier parameters:
| Parameter | Type | Purpose |
|-----------|------|---------|
| `id` | `String?` | Element identifier in the wireframe |
| `isSensitive` | `Boolean?` | `true` = mask, `false` = don't mask, `null` = inherit |
| `positionInList` | `Int?` | Position hint for list items |

---

## State Inspection

Check the current session replay state:
```kotlin
val replay = SplunkRum.instance.sessionReplay

// Recording status
val status = replay.state.status  // Status.Recording or Status.NotRecording(cause)

// Effective settings
val rate = replay.state.samplingRate
val mode = replay.state.renderingMode
```

`Status.NotRecording` causes:
- `NOT_STARTED` — `start()` has not been called
- `STOPPED` — recording was stopped via `stop()`
- `BELOW_MIN_SDK_VERSION` — device API level too low
- `DISABLED_BY_SAMPLING` — this session was not selected by sampling
- `STORAGE_LIMIT_REACHED` — device storage too low to record
- `INTERNAL_ERROR` — internal database or recording error

---

## Sampling

`samplingRate` is evaluated per-session. A rate of `0.2` means ~20% of sessions will be
eligible for recording (determined randomly at session start).

| Environment | Recommended Rate |
|-------------|-----------------|
| Development | `1.0` (record all) |
| Staging | `0.5` |
| Production | `0.1–0.2` |

---

## Best Practices

1. **Always configure sensitivity before starting** — especially for views containing PII
2. **Use `WIREFRAME_ONLY` for maximum privacy** when visual fidelity is not critical
3. **Lower sampling rate in production** to reduce bandwidth and storage
4. **Review privacy with your team** before enabling in production
5. **Test on representative devices** — recording behavior may vary by API level and device
6. **Never enable debug logging in production** — it is verbose and includes recording diagnostics

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Replay not recording | Verify `isEnabled = true` AND `start()` was called after install |
| Recording status is `NotRecording` | Check `state.status` for the cause (sampled out, disabled, low API) |
| Sensitive data visible | Add class/instance sensitivity or use `Modifier.splunkRum(isSensitive = true)` |
| High bandwidth usage | Lower `samplingRate` or switch to `WIREFRAME_ONLY` |
| `start()` has no effect | Ensure `SessionReplayModuleConfiguration` was passed to `install()` |
