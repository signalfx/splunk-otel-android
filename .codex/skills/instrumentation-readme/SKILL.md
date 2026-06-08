---
name: instrumentation-readme
description: Generate, update, or review Splunk Android RUM instrumentation README.md files in integration modules using the repository template and guidelines. Use when documenting integration instrumentation modules, telemetry data models, configuration APIs, quick starts, or reviewing whether instrumentation documentation changed with a PR.
---

# Instrumentation README

Use this skill when creating, updating, or reviewing documentation for Splunk Android RUM instrumentation modules under `integration/`. Generated README files belong at `integration/<module>/README.md`.

## Workflow

1. Read the canonical repository docs:
   - `docs/instrumentation-readme-guidelines.md`
   - `docs/instrumentation-readme-template.md`
2. Inspect the affected module code, tests, public configuration types, extension APIs, annotations, and telemetry constants.
3. Add or update `integration/<module>/README.md` using the template sections.
4. Keep the documentation factual. Do not invent behavior, dependencies, public APIs, defaults, telemetry, or setup steps.
5. If documenting accurately would require changing public API or default behavior, stop and ask for explicit confirmation before changing code.

## Review Use

For instrumentation PRs, check whether behavior, setup, public API, defaults, filtering, fallback behavior, or telemetry changed. If yes, verify `integration/<module>/README.md` was updated according to the repository template and guidelines.
