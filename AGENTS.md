# AI Agent Guidelines for Splunk Android RUM SDK

This repo is the Splunk Android RUM SDK: a lightweight, multi-module Gradle/Kotlin library built on OpenTelemetry and embedded in customer Android apps. Keep this file concise; move long module notes to focused docs only when this guide can no longer stay scannable.

## Non-Negotiables

- **No new dependencies** unless the user explicitly asks. Even dependencies listed in `buildSrc/src/main/kotlin/Dependencies.kt` need explicit approval because SDK size and consumer conflicts matter.
- **Public API is stable.** Treat `agent/` and any `integration/` module exposed via `:agent` `api` dependencies as customer-facing.
- **Do not change public API or default behavior without confirmation.** This includes signatures, defaults, visibility, constants, data classes, errors, side effects, deprecations, removals, and telemetry semantics.
- **Maintain backward compatibility.** SDK consumers should be able to upgrade without source changes unless a breaking change is intentional, documented, and approved.
- **SDK runtime paths must not crash host apps for predictable conditions.** Prefer graceful fallback plus internal diagnostics over `throw`, `error`, or `require` in production runtime paths.
- **Production mobile performance is a primary correctness concern.** Optimize for low startup, main-thread, memory, CPU, network, storage, and battery impact.

## Repo Map

| Path | Purpose |
| --- | --- |
| `agent/` | Main SDK entry point and stable public API |
| `integration/` | Feature instrumentation; public when exposed through `:agent` |
| `integration/{anr,applicationlifecycle,crash,customtracking,httpurlconnection-auto,interactions,lifecycle,navigation,networkmonitor,okhttp3-*,sessionreplay,slowrendering,startup,webview}/` | Instrumentation modules |
| `integration/agent/api/` | Public API interfaces |
| `integration/agent/common/` | Shared agent implementation |
| `integration/agent/internal/` | Internal agent implementation |
| `common/otel/`, `common/storage/`, `common/utils/` | Internal shared helpers |
| `instrumentation/buildtime/` | Gradle plugins and build-time instrumentation |
| `instrumentation/runtime/` | Runtime hooks used by instrumentation |
| `buildSrc/src/main/kotlin/Dependencies.kt` | Allowed dependency catalog, not approval to use dependencies |
| `app/` | Sample application |

## Common Commands

Use the narrowest module command that validates the change.

| Task | Command |
| --- | --- |
| Format module | `./gradlew :<module>:ktlintFormat` |
| Check module | `./gradlew :<module>:check` |
| Build module | `./gradlew :<module>:build` |
| Device/emulator tests | `./gradlew :<module>:connectedCheck` |
| Whole repo build | `./gradlew build` |
| Whole repo format | `./gradlew ktlintFormat` |

## Change Rules

- Follow existing Kotlin, Gradle, threading, storage, and module patterns before adding abstractions.
- Keep changes inside the smallest reasonable module boundary.
- Prefer `internal` for SDK implementation details.
- Document public APIs with KDoc.
- Deprecated APIs must continue to work and include migration guidance.
- New features should be opt-in unless the user explicitly approves a default behavior change.
- Do not add coroutine, threading, reactive, serialization, or utility dependencies to solve local implementation problems.
- GitHub Actions must be pinned to commit SHAs, not mutable version tags.
- When runtime state is invalid or optional components are absent, no-op or partially degrade when safe.
- Exceptions are acceptable only for invalid developer usage at API boundaries with no safe fallback, tests/test utilities, or build-time tooling.

## PR Review Priorities

Review findings in this order. Lead with customer impact, not style.

### P1 - Host App Safety and Production Performance

- Block or request measurement for avoidable overhead in customer apps at scale.
- Watch hot paths: SDK init, lifecycle callbacks, UI instrumentation, ANR/crash handling, network interceptors, span/log/event creation, processors, exporters, and background work.
- Flag any `throw`, `error`, `require`, forced unwrap, unchecked cast, or non-null assertion in SDK runtime paths when the state can predictably happen in production.
- Flag repeated allocations, reflection, regex parsing, per-signal encoder/formatter creation, disk/network I/O, locks, O(n) scans over growing data, retained `Context`/`Activity`, and large retained objects.
- Flag unbounded queues, retries, timers, listeners, observers, or background work that grows with sessions, spans, events, logs, screens, requests, or lifecycle churn.
- Consider aggregate parent-app impact, not just local method cost. Ask for benchmarks, profiling, limits, or a safer design when cost is unclear.

### P2 - Public API and Compatibility

- Public API changes must be backward compatible unless the PR clearly states an intentional breaking change.
- Surface removed/renamed symbols, changed signatures/defaults/visibility, new thrown exceptions, behavior changes, constants, schemas, exported data classes, and telemetry semantics.
- For intentional client-visible changes, require compatibility rationale, migration guidance, `CHANGELOG.md`, and tests covering relevant old/new behavior.
- If risk is unclear, raise concrete customer failures: compile breaks, changed telemetry, startup failure, disabled instrumentation, or unexpected runtime behavior.

### P3 - Pattern Fit and Maintainability

- New code should look like neighboring code in the same module.
- Prefer existing batching, buffering, scheduling, storage, naming, visibility, and helper patterns.
- Flag broad refactors, shared utility changes, or cross-module behavior changes unrelated to the PR goal.

### P4 - Android Lifecycle and Concurrency

- Shared mutable state must be synchronized using repo-standard patterns.
- Verify callbacks, processors, exporters, storage, listeners, and lifecycle hooks are safe under repeated, concurrent, or reentrant calls.
- Avoid main-thread blocking and background work without lifecycle, cancellation, and queue-size bounds.
- Treat singleton state, lazy init, volatile state, retained `Context`, process death, app foreground/background transitions, shutdown, and reconfiguration as risk areas.

### P5 - Telemetry Correctness

- Most non-Session-Replay logs and events are converted into spans before upload; Session Replay logs are exported and uploaded as OTLP log payloads.
- Review telemetry changes against their actual export path: final span data for span-backed signals, and log payload/body, scope, timestamps, attributes, session data, endpoint, buffering, and upload behavior for Session Replay.
- Changes to processors, exporters, interceptors, global attributes, retry behavior, buffering, endpoints, or required attributes can affect multiple signal types.

### P6 - Test Value

- Tests should prove observable behavior, not only execute code paths.
- Prefer coverage for emitted spans, state transitions, configuration effects, degraded runtime paths, lifecycle ordering, concurrency edges, API compatibility, telemetry semantics, and performance-sensitive boundaries.
- Flag tests that only assert `not null`, `no throw`, implementation details, or sleeps without synchronization.
- If risky behavior lacks meaningful coverage, call out the missing scenario directly.

### P7 - Style and Docs

- Style issues are last unless they obscure correctness or future maintenance.
- Keep docs and `CHANGELOG.md` updates focused on customer-visible behavior.

## Design Assumptions to Surface

Call these out as design choices when a PR relies on or changes them:

1. The SDK runs inside customer apps and must not add substantial startup, main-thread, memory, battery, storage, or network overhead.
2. Most logs/events become spans before upload, but Session Replay remains log-backed; signal-specific assumptions must be checked against the actual payload that is uploaded.
3. SDK init, shutdown, and reconfiguration can race with Android lifecycle callbacks and instrumentation hooks.
4. Network, storage, process lifetime, clocks, retries, and background execution are unreliable on mobile devices.
5. Unenforced assumptions should be enforced, documented, tested, or handled with graceful fallback.

## When to Stop and Ask

- Adding any dependency.
- Changing public API, default behavior, or telemetry semantics.
- Deprecating/removing customer-visible behavior.
- Introducing runtime exceptions on predictable production paths.
- Making a change that may break backward compatibility.

## Keep This Guide Short

Keep this file as the short entry point. If guidance grows too detailed, move focused material into linked docs such as `docs/agent-pr-review.md` for review examples or `docs/agent-module-map.md` for module inventories.
