# Instrumentation README Guidelines

Every instrumentation module should have a generated module-local README at `integration/<module>/README.md` that follows `docs/instrumentation-readme-template.md`.

## Scope

Use these guidelines when adding or updating README files for instrumentation modules under `integration/`.

## Content Rules

- Document implemented behavior only. Inspect the module code, tests, and public API before writing.
- Keep public API details accurate: configuration defaults, function signatures, extension properties, annotations, interfaces, and Java/Kotlin access patterns where relevant.
- Clearly state default enabled or disabled behavior and any opt-in requirements.
- Include runtime requirements, optional API requirements, and graceful fallback behavior.
- Describe source priority, ignored elements, filters, suppression rules, and deduplication rules when applicable.
- Document emitted telemetry using exact signal names, attribute names, types, required status, scope, span kind, and duration semantics.
- Keep examples minimal and compilable in spirit. Do not add setup that is unrelated to the instrumentation.
- Do not add dependencies or imply dependencies that are not already part of the SDK.
- Do not change public API or default behavior to make the documentation easier to write.

## Review Expectations

When a PR changes instrumentation behavior, setup, public API, defaults, filtering, fallback behavior, or telemetry, reviewers should check that `integration/<module>/README.md` was added or updated.

If a PR changes instrumentation code but does not update the README, the PR should explain why the change has no documentation impact.
