# Upgrade the aligned OpenTelemetry Java stack to API 1.62

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with the ExecPlan requirements and guidelines in the `execution-plan` skill.

## Purpose / Big Picture

The Splunk Android RUM SDK currently obtains OpenTelemetry Java 1.49 through `opentelemetry-instrumentation-bom-alpha:2.15.0-alpha`. This work upgrades the Java API, SDK, context, common, exporters, and instrumentation APIs as one supported dependency family: OpenTelemetry Java 1.62 with instrumentation 2.28. The SDK does not consume `opentelemetry-android`, so this work must preserve Android API 21 support and Kotlin 1.8 compatibility.

After the change, developers can build the SDK with the aligned dependencies, OkHttp and HttpURLConnection continue producing equivalent network spans using compatibility implementations for the removed peer-service APIs, logs and Session Replay still serialize correctly, and consumer applications can resolve compatible OpenTelemetry dependencies. The change is demonstrated by module tests, consumer dependency tests, API 21/22 build or runtime checks where an emulator is available, and before/after size and initialization measurements.

## Progress

- [x] (2026-08-24) Create branch `upgrade/otel-api` from `develop` while preserving unrelated untracked files.
- [x] (2026-08-24) Record the implementation and validation plan.
- [x] (2026-08-24) Upgrade `opentelemetry-instrumentation-bom-alpha` from 2.15.0-alpha to 2.28.0-alpha; the full agent release compile succeeds with the aligned graph.
- [x] (2026-08-24) Replace removed OkHttp and HttpURLConnection peer-service APIs with local compatibility extractors that preserve programmatic mapping and `peer.service` telemetry.
- [x] (2026-08-24) Preserve the existing exhaustive W3C trace-flags regression test; include it in final validation.
- [x] (2026-08-24) Add network instrumentation tests for span name, method, status, server address and port, headers, errors, and programmatic peer-service mapping.
- [x] (2026-08-24) Validate general log-to-span conversion and Session Replay OTLP log serialization, including the explicit protobuf encoding for an empty-string attribute.
- [x] (2026-08-24) Replace the internal `ExtendedLogRecordData` event-name cast with stable `LogRecordData.getEventName()`; retain the `event.name` fallback because all current producers and compatible host logs still rely on it.
- [x] (2026-08-24) Add a consumer dependency fixture and compile the sample app with competing OpenTelemetry BOMs at 1.49.0 and 1.64.0.
- [x] (2026-08-24) Preserve minSdk 21 and Kotlin 1.8, assemble the app, and pass an OTel initialization/span smoke test under Robolectric API 21 and 22.
- [x] (2026-08-24) Measure final AAR/APK size and cold-start impact against the pre-upgrade artifacts.
- [x] (2026-08-24) Run the narrow module checks and a successful whole-repository build; document the API 21/22 emulator limitation.
- [x] (2026-08-24) Commit the audited implementation and validation artifacts without including unrelated workspace changes.

## Surprises & Discoveries

- Observation: The SDK does not have any `io.opentelemetry.android` dependency in `:agent:releaseRuntimeClasspath`.
  Evidence: `./gradlew :agent:dependencyInsight --dependency io.opentelemetry.android --configuration releaseRuntimeClasspath` reports no matching dependencies.

- Observation: The first aligned dependency probe failed only on three removed Java instrumentation incubator classes: `HttpClientPeerServiceAttributesExtractor`, `PeerServiceAttributesExtractor`, and `PeerServiceResolver`.
  Evidence: Compilation under instrumentation BOM 2.28.0-alpha resolved Java core artifacts to 1.62.0 and then failed in the OkHttp and HttpURLConnection peer-service integrations.

- Observation: OpenTelemetry Java 1.60 changed empty string OTLP attributes from an unset `AnyValue` oneof to an explicitly present `string_value: ""`; 1.62's repeated-string size fix is profiles-specific and does not describe attribute arrays.
  Evidence: The SDK directly invokes `TraceRequestMarshaler` and `LogsRequestMarshaler`, and interaction telemetry can emit empty strings through `orEmpty()`.

- Observation: The 2.28 service-peer extractor reads `otel.instrumentation.common.peer-service-mapping` only through `ExtendedOpenTelemetry`; the repository constructs a plain `OpenTelemetrySdk` and configures peer mapping through its own public builders.
  Evidence: Using `HttpClientServicePeerAttributesExtractor.create(getter, openTelemetry)` would ignore `setPeerServiceMapping(Map)` and would select newer `service.peer.name` semantics rather than preserving `peer.service`.

- Observation: OpenTelemetry Java 1.62 exposes event name through stable `LogRecordData.getEventName()` and `LogRecordBuilder.setEventName()` APIs.
  Evidence: `javap` against the resolved 1.62 API and SDK logs artifacts shows both default methods; every current SDK producer still sets the `event.name` attribute instead of the stable event-name field.

- Observation: A host's older 1.49.0 OTel BOM is raised to the SDK's 1.62.0 platform constraints, while a newer 1.64.0 host BOM wins dependency mediation; the app compiles in both cases.
  Evidence: The checked-in Gradle init fixture compiles `:app:compileDebugKotlin` for both versions. `dependencyInsight` selects API 1.62.0 for the older host and 1.64.0 for the newer host.

- Observation: No API 21 or 22 hardware emulator image is installed in this environment; installed AVDs target API 33 and 36.
  Evidence: The API 21/22 Robolectric initialization smoke test passes, but an on-device API 21/22 smoke run cannot be performed without downloading additional Android system images.

- Observation: The final thin `agent` AAR remains 1,392 bytes, while the R8-minified sample release APK grows from 3,333,812 to 3,349,212 bytes: +15,400 bytes or +0.46%.
  Evidence: Both artifacts were built with the same release commands before and after the BOM change.

- Observation: Emulator cold-start timing does not show a repeatable initialization regression. An unnormalized alternating 20-run sample moved from 370 ms to 387 ms median, but after AOT-compiling each APK the 20-run median moved from 327 ms to 297.5 ms; run-to-run standard deviations were 33.4 ms and 50.5 ms.
  Evidence: Android `am start -W -S` measurements on the same API 33 Pixel_6 AVD changed direction when JIT noise was controlled, so the result is treated as no detectable regression rather than a speedup.

- Observation: The renamed manual OkHttp span-name customizer initially triggered Android lint because its adapter invokes the existing public `java.util.function.Function` API, which requires API 24 or core-library desugaring.
  Evidence: The sample application already enables core-library desugaring, and a scoped `NewApi` lint suppression on this existing public API allows minSdk 21 to remain unchanged. The targeted lint and whole-repository build both pass.

## Decision Log

- Decision: Upgrade the instrumentation BOM rather than force only API/context/common to 1.62.
  Rationale: OpenTelemetry recommends version alignment because its SDK and exporters use internal APIs. The requested work is the full supported upgrade, not the temporary mixed-version security workaround.
  Date/Author: 2026-08-24 / Codex

- Decision: Do not add `opentelemetry-android`, raise minSdk, or upgrade Kotlin as part of this work.
  Rationale: The repository ports selected Android instrumentation code and has no runtime dependency on the upstream Android artifacts. Those compatibility changes are unrelated to the Java BOM upgrade.
  Date/Author: 2026-08-24 / Codex

- Decision: Treat peer-service migration as a telemetry compatibility change, not merely an import rename.
  Rationale: The newer semantic convention emits `service.peer.name` where the old extractor emitted `peer.service`, and its configuration path cannot consume the SDK's programmatic map. Local compatibility extractors preserve both the configured map and current schema.
  Date/Author: 2026-08-24 / Codex

- Decision: Use stable `LogRecordData.getEventName()` but retain the `event.name` attribute fallback and current producer behavior.
  Rationale: Removing the cast eliminates reliance on an internal 1.62 type without changing telemetry. Removing the attribute now would break span naming for every current SDK producer and could break compatible host-created records; migrating emitted log schema requires a separate approved compatibility change.
  Date/Author: 2026-08-24 / Codex

- Decision: Keep the current Splunk crash, ANR, and slow-rendering signal models unless separately approved.
  Rationale: Their upstream Android span-to-event migrations are not required by the Java dependency upgrade and would change customer-visible telemetry semantics.
  Date/Author: 2026-08-24 / Codex

## Outcomes & Retrospective

The upgrade aligns OpenTelemetry Java API, context, SDK, SDK common, traces, logs, and OTLP exporters at 1.62.0 and instrumentation components at 2.28.0/2.28.0-alpha. No `opentelemetry-android` artifact is introduced, and the repository retains minSdk 21 and Kotlin 1.8.

OkHttp and HttpURLConnection compile against the new instrumentation API and preserve the existing programmatic peer mapping and `peer.service` schema through small local compatibility extractors. Network tests cover span name, method, status, address, port, headers, errors, and peer mapping. The existing exhaustive `traceparent` flags test remains intact. Common OTel tests cover general log-to-span conversion and Session Replay OTLP logs, including explicit empty-string attribute encoding. `AndroidLogRecordExporter` now uses the stable event-name API but keeps the legacy `event.name` fallback to avoid changing current producer and host-log behavior.

The sample host application compiles with competing OTel BOM declarations at 1.49.0 and 1.64.0; dependency mediation selects 1.62.0 and 1.64.0 respectively. API 21 and 22 Robolectric initialization and span-creation smoke tests pass. No API 21/22 emulator image is installed, so a physical-emulator runtime smoke test remains an environment limitation rather than a passed check.

The thin agent AAR remains 1,392 bytes. The minified sample release APK changes from 3,333,812 to 3,349,212 bytes, an increase of 15,400 bytes or 0.46%. Alternating API 33 emulator cold-start samples changed direction after AOT normalization, so the measurements show no detectable repeatable initialization regression; they do not establish a speedup or replace production-device performance testing.

## Context and Orientation

`buildSrc/src/main/kotlin/Dependencies.kt` defines the instrumentation BOM. Core OpenTelemetry dependencies are intentionally versionless in module build files and receive versions from that BOM. `common/otel` constructs and exports the SDK and uses OpenTelemetry's internal OTLP marshalers to write traces and logs to Splunk storage. `instrumentation/runtime/okhttp3-*` and `instrumentation/runtime/httpurlconnection-auto` implement network instrumentation. Integration modules publish these runtime implementations through the main `agent` artifact.

A bill of materials, abbreviated BOM, is a Gradle platform that constrains a family of dependency versions so they are tested together. A peer-service mapping converts a network destination into a logical backend service name. In the older instrumentation API that value is represented by `peer.service`; newer experimental semantic conventions use `service.peer.name`.

The SDK exposes OpenTelemetry transitively through `api(...)` dependencies. This makes dependency mediation part of customer compatibility: a host application may declare its own OpenTelemetry version, and Gradle selects one version for both the application and SDK.

## Plan of Work

First update the BOM constant in `buildSrc/src/main/kotlin/Dependencies.kt` to 2.28.0-alpha and inspect `:agent:releaseRuntimeClasspath`. The accepted graph must contain OpenTelemetry Java API, context, common, SDK, and OTLP exporters at 1.62.0 and instrumentation components at 2.28.0. No `io.opentelemetry.android` module may appear.

Next migrate the network code in `instrumentation/runtime/httpurlconnection-auto` and `instrumentation/runtime/okhttp3-auto` from the removed peer-service resolver and extractor classes. Keep the existing public Splunk configuration, mapping input, and `peer.service` output unchanged through local compatibility extractors because the replacement upstream API reads only declarative configuration from `ExtendedOpenTelemetry`. Update tests to validate observable spans rather than implementation class names. Exercise successful responses, HTTP errors, connection failures, configured request and response headers, default and explicit ports, span names, methods, status codes, server addresses, and mapped service names.

Then validate propagation and export. Locate the existing random trace flag tests and retain their assertions. Extend common OTel tests so a general log becomes one zero-duration span with the intended event name and attributes. Decode Session Replay bytes as an OTLP `ExportLogsServiceRequest` and verify body, scope, timestamps, session data, and an explicitly empty string attribute. If `LogRecordData.getEventName()` is stable in 1.62, replace the cast to `ExtendedLogRecordData`. Remove the fallback `event.name` attribute only if every producer uses the stable event-name setter or compatibility behavior requires no fallback; otherwise keep the fallback and document why.

Add small consumer fixtures or Gradle TestKit coverage that consumes the published project modules under three dependency graphs: no host override, host OpenTelemetry 1.62, and an older host OpenTelemetry declaration. The default and 1.62 cases must compile and resolve a single aligned family. The older case must either be safely upgraded by the SDK's BOM constraints or fail with a clear documented incompatibility; it must not silently produce a mixture of core versions.

Finally build API 21 and API 22 variants and use installed emulators for runtime smoke tests when available. Measure released AARs and a minified sample APK before and after the dependency change. Use the repository's existing initialization performance tooling if present; otherwise add a repeatable Android benchmark or test harness only if it can measure SDK initialization without introducing a production dependency. Report median and spread from repeated runs and avoid claiming performance equivalence from a single timing.

## Concrete Steps

Run all commands from `/Users/aditis3/codeRepos/splunk-otel-android`.

Inspect and update dependencies:

    ./gradlew :agent:dependencyInsight --dependency opentelemetry-api --configuration releaseRuntimeClasspath
    ./gradlew :agent:dependencyInsight --dependency opentelemetry-sdk --configuration releaseRuntimeClasspath
    ./gradlew :agent:dependencyInsight --dependency io.opentelemetry.android --configuration releaseRuntimeClasspath

Compile and test the smallest affected modules first:

    ./gradlew :instrumentation:runtime:httpurlconnection-auto:check
    ./gradlew :instrumentation:runtime:okhttp3-common:check
    ./gradlew :instrumentation:runtime:okhttp3-auto:check
    ./gradlew :instrumentation:runtime:okhttp3-manual:check
    ./gradlew :common:otel:check

Then validate the published agent and repository:

    ./gradlew :agent:assembleRelease
    ./gradlew build

Device checks depend on installed emulator destinations. Discover them before running `connectedCheck` and record clearly when API 21 or 22 is unavailable rather than substituting a newer API level.

## Validation and Acceptance

Dependency acceptance requires one aligned Java OpenTelemetry family at API/SDK/exporter 1.62.0 and instrumentation 2.28.0, with no OpenTelemetry Android artifact. All affected modules must compile without references to the removed peer-service APIs.

Network acceptance requires tests that inspect exported span data and prove the method, name, status, address, port, configured headers, error status, and peer mapping for both OkHttp and HttpURLConnection. Existing public configuration and default instrumentation enablement must remain unchanged.

Export acceptance requires decoded OTLP payload tests. A normal general log must become the same zero-duration span as before. Session Replay must remain an OTLP log payload. An empty string attribute must decode as a present string with an empty value. Event names must be preserved without depending on an internal SDK data implementation when the stable API supports that behavior.

Compatibility acceptance requires API 21 and 22 builds to succeed. Runtime smoke tests should initialize the SDK and create at least one network span without crashing on installed API 21/22 emulators. Consumer fixtures must demonstrate deterministic OpenTelemetry version mediation.

Performance acceptance requires exact before/after artifact sizes and a repeatable initialization measurement. Regressions must be reported rather than hidden; material startup or size increases require a follow-up decision before release.

## Idempotence and Recovery

Gradle dependency and test commands are safe to repeat. Changes stay on `upgrade/otel-api`. Unrelated untracked directories present before work must not be added, removed, or modified. If an aligned dependency produces an unexpected compile failure, preserve the failing output in `Surprises & Discoveries`, fix the smallest affected module, and rerun its narrow check before continuing. Do not lower dependency versions piecemeal to make the build pass because that would recreate an unsupported mixed stack.

## Artifacts and Notes

The pre-upgrade baseline is instrumentation BOM 2.15.0-alpha with OpenTelemetry Java 1.49.0, minSdk 21, Kotlin 1.8, and no `io.opentelemetry.android` dependency. Exact baseline and final size/performance numbers will be appended after measurement.

## Interfaces and Dependencies

The final implementation must use `io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:2.28.0-alpha`. It must preserve Splunk's programmatic peer mapping and `peer.service` output without introducing a new dependency, `opentelemetry-android`, coroutine library, serializer, or test framework.

The Splunk public APIs, Gradle plugin IDs, default instrumentation behavior, and Android/Kotlin compatibility baseline must remain unchanged. Any discovered need to change them stops implementation until explicitly approved.

Plan revision note: Initial plan created on 2026-08-24 to implement the nine-item OpenTelemetry 1.62 upgrade checklist while separating Java-stack work from upstream OpenTelemetry Android adoption. Updated after implementation to record the local peer compatibility decision, remove an out-of-scope baggage claim, capture final compatibility, size, performance, and API 21/22 environment results, document the API-21 lint handling discovered by the full build, and close the plan after the audited commit.
