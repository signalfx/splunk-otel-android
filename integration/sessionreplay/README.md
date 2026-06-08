# Session Replay Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Session recording runtime.
- Compose UI is optional; Compose element identification is installed only when Compose UI is present.
- A session replay endpoint must be available before buffered replay data can upload.

## Overview

Session Replay records user sessions and exports replay data chunks that can be uploaded to the Splunk session replay endpoint.

Detection sources:

- Manual calls through `SessionReplay.start()` and `SessionReplay.stop()`.
- Session changes from the agent session manager.
- Session recording `DataListener` callbacks.
- Optional Compose `Modifier.sessionReplay(...)` metadata.

Detection behavior:

- The module is enabled by default.
- `samplingRate` defaults to `0.2f`; sampled-out sessions cannot record.
- Recording must be requested with `SessionReplay.start()`.
- If a new sampled-in session starts while recording was requested, recording starts or a new data chunk is created.
- If a session is sampled out while recording was requested, recording stops and status reports `DISABLED_BY_SAMPLING`.
- `WebView` is not sensitive by default for Splunk agents.
- Session replay data is specially buffered and uploaded by the log exporter instead of being converted into normal span telemetry.

## Quick Start

```kotlin
SplunkRum.install(
    this,
    agentConfiguration,
    SessionReplayModuleConfiguration(
        isEnabled = true,
        samplingRate = 0.2f
    )
)
```

```kotlin
SplunkRum.instance.sessionReplay.start()
```

```kotlin
SplunkRum.instance.sessionReplay.stop()
```

```kotlin
Modifier.sessionReplay(
    id = "checkout-button",
    isSensitive = false
)
```

## API Documentation

### `SessionReplayModuleConfiguration`

Configures session replay.

```kotlin
SessionReplayModuleConfiguration(
    isEnabled: Boolean = true,
    samplingRate: Float = 0.2f
)
```

- `isEnabled`: Enables session replay so recording can be started.
- `samplingRate`: Session sampling rate. `0f` records no sessions, `1f` records all sessions, and `0.2f` records about one fifth of sessions.
- `name`: Module name reported as `sessionReplay`.
- `attributes`: Module attributes containing `enabled` and `samplingRate`.

### `SessionReplay`

Entry point for controlling recording and accessing replay state.

```kotlin
SessionReplay.instance
```

- Kotlin: `SessionReplay.instance` or `SplunkRum.instance.sessionReplay`.
- Java: `SessionReplay.getInstance()`.
- Must be accessed after `SplunkRum.install(...)` creates the session replay instance.

### `SplunkRum.sessionReplay`

Kotlin extension property for accessing session replay from a `SplunkRum` instance.

```kotlin
val SplunkRum.sessionReplay: SessionReplay
```

### `SessionReplay.start()`

Starts recording user activity when the module is enabled, the Android API level is supported, and the current session is sampled in.

```kotlin
fun start()
```

If recording cannot start because the module is disabled, the API level is below the agent runtime floor, or the session is sampled out, the SDK logs internally and updates state where applicable.

### `SessionReplay.stop()`

Stops recording user activity.

```kotlin
fun stop()
```

### `SessionReplay.preferences`

Preferred session replay configuration.

```kotlin
val preferences: Preferences
```

### `Preferences.renderingMode`

Preferred screen data rendering mode.

```kotlin
var renderingMode: RenderingMode?
```

### `SessionReplay.state`

Current session replay state.

```kotlin
val state: State
```

### `State`

Read-only current state.

```kotlin
class State
```

- `status`: Current `Status`.
- `samplingRate`: Active module sampling rate.
- `renderingMode`: Current `RenderingMode`.

### `Status`

Recording status.

```kotlin
sealed interface Status
```

- `Status.Recording`: The SDK is recording.
- `Status.NotRecording`: The SDK is not recording.
- `Status.NotRecording.Cause`: `NOT_STARTED`, `STOPPED`, `BELOW_MIN_SDK_VERSION`, `STORAGE_LIMIT_REACHED`, `INTERNAL_ERROR`, or `DISABLED_BY_SAMPLING`.
- `isRecording`: `true` when the status is `Recording`.

### `RenderingMode`

Screen data rendering mode.

```kotlin
enum class RenderingMode {
    NATIVE,
    WIREFRAME_ONLY
}
```

- `NATIVE`: Records screen images with sensitive areas hidden.
- `WIREFRAME_ONLY`: Records wireframe screen data.

### `SessionReplay.sensitivity`

Sensitivity configuration for native rendering mode.

```kotlin
val sensitivity: Sensitivity
```

### `Sensitivity`

Configures whether views are covered in replay data.

```kotlin
fun <T : View> setViewInstanceSensitivity(view: T, isSensitive: Boolean?)
fun <T : View> setViewClassSensitivity(clazz: Class<T>, isSensitive: Boolean?)
fun <T : View> getViewInstanceSensitivity(view: T): Boolean?
fun <T : View> getViewClassSensitivity(clazz: Class<T>): Boolean?
```

- Instance sensitivity overrides class sensitivity.
- `EditText` is sensitive by default.
- `null` clears the override for that instance or class.

### Sensitivity Extensions

Kotlin-only helpers for sensitivity configuration.

```kotlin
var View.isSensitive: Boolean?
var <T : View> KClass<T>.isSensitive: Boolean?
var <T : View> Class<T>.isSensitive: Boolean?
```

### `Modifier.sessionReplay(...)`

Adds session replay metadata to a Compose modifier.

```kotlin
fun Modifier.sessionReplay(
    id: String? = null,
    isSensitive: Boolean? = null,
    positionInList: Int? = null
): Modifier
```

- `id`: Optional stable element ID.
- `isSensitive`: Optional sensitivity override.
- `positionInList`: Optional list position for target path metadata.

### `SessionReplay.recordingMask`

Manual rectangular mask configuration for native rendering mode.

```kotlin
var recordingMask: RecordingMask?
```

### `RecordingMask`

Defines screen-space rectangles to cover or uncover.

```kotlin
data class RecordingMask(
    val elements: List<RecordingMask.Element>
)
```

```kotlin
RecordingMask.Element(
    rect: Rect,
    type: RecordingMask.Element.Type = COVERING
)
```

- `COVERING`: Covers the rectangle.
- `ERASING`: Uncovers the rectangle.

## Telemetry Data Model

Session replay exports replay data as log payloads through special session replay upload handling. It also emits one recording marker event per recorded session, which the SDK exports as a zero-length internal span.

| Signal | Name | When emitted |
|---|---|---|
| Log payload | `session_replay_data` | A session replay data chunk is produced. |
| Span | `splunk.sessionReplay.isRecording` | The first replay data chunk for a session is accepted. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| `rr-web.total-chunks` | double | Yes for replay data | Total chunks in the emitted segment. Current value is `1.0`. |
| `rr-web.chunk` | double | Yes for replay data | Chunk index in the emitted segment. Current value is `1.0`. |
| `rr-web.event` | long | Yes for replay data | Event index for the replay data. |
| `rr-web.offset` | double | Yes for replay data | Offset for the replay data. |
| `segmentMetadata` | string | Yes for replay data | Serialized replay segment metadata. |
| `component` | string | Yes for recording marker | `session.replay` |
| `splunk.sessionReplay` | string | Yes for recording marker | `splunk` |
| `session.id` | string | Yes for recording marker | Session ID associated with the replay recording. |

Resource / scope:

- Replay data instrumentation scope name: `SessionReplayDataScopeName`
- Recording marker instrumentation scope name: `SplunkRum`
- Recording marker span kind: `INTERNAL`
- Recording marker span duration: zero-length
