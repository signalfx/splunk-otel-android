# Proposed iOS Skill Alignment Changes

This document summarizes the changes we'd propose for the iOS skill
([signalfx/splunk-otel-ios#681](https://github.com/signalfx/splunk-otel-ios/pull/681))
to align strategy across the Android and iOS Splunk RUM SDK skills.

The goal is **shared agent behavior patterns** across platforms while preserving
platform-specific content. We are NOT asking for identical files — only for consistent
routing, terminology, and skill structure so agents trained on either skill behave
predictably.

---

## 1. Consolidate Reference Files (per manager feedback)

The iOS branch currently has 26 docs (~2,000 lines). Per the manager's feedback,
consolidate into a smaller routed set:

| Proposed iOS Reference | Merges from current files |
|----------------------|--------------------------|
| `skill-references/installation.md` | `install/fresh-install.md`, `install/migration.md`, relevant parts of `inspection.md` |
| `skill-references/swift-instrumentation.md` | `instrumentation/manual-spans.md`, `instrumentation/navigation.md`, `instrumentation/network.md`, relevant Swift-specific content |
| `skill-references/objc-integration.md` | `instrumentation/objc-interop.md` |
| `skill-references/session-replay.md` | `instrumentation/session-replay.md` |
| `skill-references/webview.md` | `instrumentation/webview.md` |
| `skill-references/network-and-privacy.md` | `instrumentation/network.md` (privacy parts), `privacy-and-security.md` |
| `skill-references/crash-and-symbolication.md` | Crash/dSYM docs |
| `skill-references/other-instrumentations.md` | `instrumentation/app-lifecycle.md`, `instrumentation/slow-rendering.md`, `instrumentation/custom-url-session.md`, any remaining instrumentation files |
| `skill-references/verification-troubleshooting.md` | `verification/build-verification.md`, `verification/runtime-verification.md`, `verification/data-verification.md` |

**Delete:**
- `metrics-and-reporting.md` — not needed for agentic onboarding
- `sample-app-workflow.md` — better as part of testing documentation

**Fold into main `SKILLS.md`:**
- `inspection.md` — detection commands belong in the router's Detect phase
- `privacy-and-security.md` — fold into Hard Gates section and session-replay reference

---

## 2. Add "Load when" / "Do not load when" Headers

Each reference file should include routing metadata at the top so the agent
knows when to load it. This avoids loading all 2,000 lines for every prompt.

**Format we use on Android:**

```markdown
> **Load when:** User requests visual session recording or comprehensive depth is selected.
>
> **Do not load when:** User has not requested Session Replay and depth is baseline.
>
> **Source files to verify:**
> - `Sources/SplunkRum/SessionReplay/...` — relevant Swift source files
```

This is already partially done in the iOS skill with the routing table in `SKILLS.md`.
The proposal is to move the routing hints into each reference file itself so they're
self-documenting.

---

## 3. Align Top-Level Router Structure

Both `SKILLS.md` files should have the same major sections in the same order:

| Section | Android | iOS (proposed) |
|---------|---------|---------------|
| Frontmatter (name, description, triggers) | Yes | Already has |
| **Modes** (plan/review/apply/verify) | Yes | Already has — keep as-is |
| **Instrumentation Depth** (baseline/targeted/comprehensive) | Yes | Already has — keep as-is |
| **Hard Gates** (secrets, dependencies, privacy, architecture) | Yes | Already has — keep as-is |
| **Detect** (platform-specific commands) | Yes | Fold in from `inspection.md` |
| **Recommend** (feature recommendation logic) | Yes | Add recommendation table with iOS features |
| **Guide** (reference routing table) | Yes | Already has — align column names |
| **Review** (checklist for existing integrations) | Yes | Add review checklist |
| **Config Quick Reference** (API surface summary) | Yes | Add `SplunkRumBuilder` quick reference |
| **Troubleshooting Quick Reference** (common issues table) | Yes | Fold in from verification files |

The iOS skill already has Modes, Depth, and Hard Gates — these are well done and were
the inspiration for adding them to Android. The main gaps are:

- **Detect phase:** Currently in a separate `inspection.md`. Move the detection commands
  into the main `SKILLS.md` so the agent always runs them.
- **Recommend phase:** Add a recommendation logic table (similar to Android's) that maps
  detected features to suggested modules.
- **Review phase:** Add a review checklist for existing integrations (similar to Android's
  Phase 4).
- **Config Quick Reference:** Add a quick-reference table of `SplunkRumBuilder` properties
  and their defaults directly in `SKILLS.md`.

---

## 4. Align Terminology

| Concept | Android term | iOS term (current) | Proposed alignment |
|---------|-------------|--------------------|--------------------|
| Main router file | `SKILLS.md` | `SKILLS.md` | Already aligned |
| Reference directory | `skill-references/` | `skill-references/` | Already aligned |
| Depth levels | `baseline` / `targeted` / `comprehensive` | `baseline` / `targeted` / `comprehensive` | Already aligned |
| Modes | `plan` / `review` / `apply` / `verify` | `plan` / `review` / `apply` / `verify` / `sample` | iOS has `sample` mode — consider keeping it as iOS-specific or adding to Android |
| Routing headers | "Load when" / "Do not load when" | Various | Standardize to "Load when" / "Do not load when" |
| Config summary | "Configuration Quick Reference" | (none) | Add to iOS |
| Review checklist | "Phase 4: Review Existing Integration" | (none) | Add to iOS |

---

## 5. Specific Content Suggestions

### Keep from iOS (good patterns the Android skill adopted)
- Explicit `Modes` section with table — we adopted this
- `Instrumentation Depth` levels — we adopted this
- `Hard Gates` section — we adopted this, with Android-specific gates
- `sample` mode for generating example projects — iOS-specific, keep it

### Add to iOS
- **Recommendation logic table** — maps detected project features (e.g., "Uses URLSession"
  → "Recommend network instrumentation") to specific modules. Helps the agent propose a
  tailored plan rather than dumping all options.
- **Review checklist** — structured checklist for reviewing existing integrations. Covers
  initialization, token safety, duplicate setup, module configuration, privacy, and
  performance.
- **Troubleshooting quick reference** — a compact issue/solution table in the main
  `SKILLS.md` for the most common problems, with detailed troubleshooting in the reference
  file.

### Remove from iOS
- `metrics-and-reporting.md` — per manager feedback
- `sample-app-workflow.md` — per manager feedback

---

## 6. Directory Structure Comparison

**Android (current):**
```
SKILLS.md
skill-references/
  installation.md
  screen-navigation.md
  network-instrumentation.md
  custom-events.md
  session-replay.md
  crash-and-symbolication.md
  verification-troubleshooting.md
docs/
  ios-skill-alignment-proposal.md    ← this file
```
7 reference files, ~1,200 lines total.

**iOS (proposed):**
```
SKILLS.md
skill-references/
  installation.md
  swift-instrumentation.md
  objc-integration.md
  session-replay.md
  webview.md
  network-and-privacy.md
  crash-and-symbolication.md
  other-instrumentations.md
  verification-troubleshooting.md
```
9 reference files (down from 26). iOS has more because of Swift/ObjC split and WebView
being a bigger surface on iOS.

---

## Summary of Asks

1. **Consolidate** from 26 files to ~9 reference files (per manager feedback)
2. **Add "Load when" headers** to each reference file
3. **Move detection/inspection** into the main `SKILLS.md` Detect phase
4. **Add a Recommend phase** with detection-to-module mapping
5. **Add a Review phase** with integration review checklist
6. **Add a Config Quick Reference** table to `SKILLS.md`
7. **Add a Troubleshooting Quick Reference** table to `SKILLS.md`
8. **Delete** `metrics-and-reporting.md` and `sample-app-workflow.md`
9. **Fold** `inspection.md` and `privacy-and-security.md` into router / topic refs

Everything else in the iOS skill (modes, depth, hard gates, platform-specific content)
is well structured and should stay as-is.
