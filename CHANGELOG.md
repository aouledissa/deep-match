# Changelog

## [Unreleased]

### Added

- Added `validateDeeplinks` Gradle task (`--uri`) to validate one URI against declared specs and
  print per-spec `[MATCH]`/`[MISS]` diagnostics with extracted params.
- Added `validate<Variant>CompositeSpecsCollisions` Gradle task to detect URI-shape collisions
  across composed module specs and fail builds with a detailed conflict report.
- Added runtime composition support via `CompositeDeeplinkProcessor`, returning the first
  successful match across composed processors.

### Changed

- Query parameters are no longer part of `DeeplinkSpec`'s structural regex matcher.
- `DeeplinkProcessor` now validates typed query params separately after structural URI matching.
- `DeeplinkProcessor` URI normalization no longer includes query string content.
- `DeeplinkSpec` now supports optional `port` and includes it in URI matching.
- `DeeplinkSpec.pathParams` now uses `List<Param>` (instead of `Set<Param>`) so declared path
  segment order is preserved explicitly.
- Plugin code generation now emits `pathParams = listOf(...)` for generated specs to align with
  the new API shape.
- Fragment-only deeplink specs generate `*DeeplinkParams` with a `fragment` property and wire
  `paramsFactory` accordingly (alongside the new always-generate params behavior).
- Added `required` to query params (`Param.required`) to support optional-vs-required query matching.
- Optional typed query params are now generated as nullable constructor properties; required query
  params remain non-null.
- Plugin code generation now emits a `*DeeplinkParams` class for every deeplink spec, including
  static-only specs with no typed path/query/fragment fields.
- Plugin config model now treats `host` as optional (`[]` by default), enabling hostless specs.
- Manifest generation now selects tighter path attributes based on path shape (`path`,
  `pathPrefix`, `pathPattern`, and `pathAdvancedPattern` on API 31+ builds), and emits dual
  static-path entries to support trailing slash variants.
- Generated manifests now include `android:exported="true"` and `tools:node="merge"` on deeplink
  activities.
- DeepMatch Gradle tasks now register under a dedicated `deepmatch` task group with explicit task
  descriptions (`generate<Variant>DeeplinkSpecs`, `generate<Variant>DeeplinksManifest`,
  `validateDeeplinks`).
- `DeeplinkSpec` now carries `name` metadata and uses `paramsFactory` instead of reflection-based
  `parametersClass` construction.
- `DeeplinkProcessor` now invokes spec-provided `paramsFactory` callbacks.
- Plugin code generation now emits `name` and `paramsFactory = <Params>.Companion::fromMap` for
  generated specs, and generates robust `fromMap` factories for params classes.
- Plugin now auto-discovers project dependencies that also apply DeepMatch and composes their
  generated processors implicitly.
- Plugin now emits per-module spec-shape metadata during codegen and wires collision validation into
  variant build/check for composed projects.

### Fixed

- Fixed critical query matching behavior where typed query params could fail when URL query order
  differed from spec declaration order (for example, `?page=1&ref=promo` now matches
  `ref + page` specs correctly).
- Fixed query matching strictness: typed query params are optional by default, validated only when
  present, and enforced only when marked `required: true`.
- Fixed match-result ambiguity for generated processors: static deeplink matches no longer collapse
  to `null` (which was previously indistinguishable from "no match").
- Fixed hostless deeplink matching (`app:///...`) by explicitly supporting empty-host specs.
- Fixed manifest generation by removing invalid `android:fragment` output and treating fragment/query
  constraints as processor-only matching rules.
- Added build-time validation that every spec declares at least one scheme.
- Added build-time validation for duplicate deeplink spec names with a clear plugin error message.

### Documentation

- Updated README and docs pages to document order-agnostic typed query param matching semantics.
- Updated README/docs to clarify ordered path params list semantics in generated specs.
- Updated docs to cover `queryParams.required` and generated nullability semantics for optional
  query params.
- Updated docs to document manifest path strategy, optional `port`, and hostless + trailing-slash
  manifest behavior.
- Added a dedicated docs page for generated tasks and URI validation usage
  (`docs/tasks.md`), and linked it from plugin/overview docs.
- Updated README/docs to explain automatic processor composition from DeepMatch-enabled project
  dependencies.
- Added multi-module documentation (`docs/composite-specs.md`) covering multi-spec layout, automatic
  processor composition, and URI match resolution order/precedence.

### Tests

- Added plugin tests to validate fragment-only specs generate params classes and that generated
  specs emit `pathParams = listOf(...)`.
- Added processor regression coverage to ensure path matching remains positional and order-sensitive.
- Added query optionality tests for API/processor/plugin generation, including required-missing
  rejection and optional-missing acceptance.
- Added regression tests ensuring static-only specs generate params classes and return a concrete
  params instance on successful match.
- Added processor coverage for case-insensitive scheme/host matching (for example,
  `HTTPS://Example.COM/...` and `App://EXAMPLE.com/...`).
- Added coverage for hostless deeplinks, including regex match, params extraction, and manifest output without `android:host`.
- Added plugin test coverage for duplicate `name` validation in `.deeplinks.yml`.
- Added manifest generation coverage for smart path attribute selection, API 31+ advanced path
  patterns, exported/merge activity attributes, and optional port output.
- Added plugin config coverage for auto-discovered composite processors from project dependencies
  and variant-specific dependency resolution.
- Added plugin task coverage for composite-spec collision validation (success and failure paths).
- Added unit coverage for `ValidateDeeplinksTask` (`--uri` required, malformed URI handling,
  successful match flow).
- Added runtime tests for `CompositeDeeplinkProcessor` first-match behavior and null fallback.
- Expanded processor tests with error-path handling (safe null on exceptions).
- Expanded Robolectric coverage for empty/blank/scheme-less URIs and malformed typed values.

## [0.2.0-alpha] - 2026-02-25

### Added

- Added `samples/android-app`, a composite-build Android sample that consumes the local plugin and
  artifacts without publishing first.
- Added a Compose-based sample UI that demonstrates deeplink matching results for profile/series/no-match flows.
- Added sample-level documentation for real-device deeplink validation via `adb`.

### Changed

- `DeeplinkProcessor` is now an open runtime class configured with a set of specs and exposes
  `match(uri: Uri): DeeplinkParams?`.
- `deepmatch-plugin` now generates a module-level sealed params interface named from the module
  name (for example, module `app` -> `AppDeeplinkParams`).
- `deepmatch-plugin` now generates a module-level processor object named from the module name
  (for example, module `app` -> `AppDeeplinkProcessor`) preconfigured with all generated specs.
- Generated `*DeeplinkParams` classes now implement the module-level sealed params interface,
  allowing exhaustive `when` checks when matching deeplinks.
- CI now validates sample codegen through the composite build flow.
- README/docs were updated to document the zero-config runtime flow and sample app usage.

### Removed

- Removed `DeeplinkHandler` from the public `deepmatch-processor` API surface.
- Removed `DeeplinkProcessor.Builder`.
- Removed `DeeplinkProcessorImpl`; matching logic now lives directly in `DeeplinkProcessor`.

### Migration

- Replace manual runtime setup:
  - Before: `DeeplinkProcessor.Builder().register(...).build()`
  - Now: use the generated `<ModuleName>DeeplinkProcessor`.
- Replace handler-based dispatch with caller-controlled branching:
  - Before: `processor.match(uri, activity = this)`
  - Now: `when (val params = <ModuleName>DeeplinkProcessor.match(uri)) { ... }`
- For local integration testing, use the composite sample build in `samples/android-app`
  (plugin resolved via `includeBuild("../..")`).

## [0.1.0-alpha] - 2025-02-14

### Added

- Introduced the DeepMatch Gradle plugin (`com.aouledissa.deepmatch.gradle`) to parse
  `.deeplinks.yml`,
  generate Kotlin sources, and optionally emit manifest entries per Android variant.
- Added the DeepMatch Processor runtime library to match URIs, construct typed parameter objects,
  and dispatch to strongly typed handlers on the main thread.
- Published the shared DeepMatch API module containing reusable spec and parameter data models.
- Delivered a DeepMatch Testing module providing fake handlers/processors and spec fixtures for unit
  test support.
