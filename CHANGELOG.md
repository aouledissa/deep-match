# Changelog

## [Unreleased]

### Changed

- `DeeplinkProcessor.match(...)` now returns parsed deeplink parameters instead of dispatching a
  handler:
  - Before: `match(uri: Uri, activity: Activity): Unit`
  - Now: `match(uri: Uri): DeeplinkParams?`
- `DeeplinkProcessor.Builder.register(...)` now registers only a `DeeplinkSpec`:
  - Before: `register(spec: DeeplinkSpec, handler: DeeplinkHandler<T>)`
  - Now: `register(spec: DeeplinkSpec)`
- `deepmatch-plugin` now generates a module-level sealed params interface named from the module
  name (for example, module `app` -> `AppDeeplinkParams`).
- Generated `*DeeplinkParams` classes now implement the module-level sealed params interface,
  allowing exhaustive `when` checks when matching deeplinks.

### Removed

- Removed `DeeplinkHandler` from the public `deepmatch-processor` API surface.
- Removed handler dispatch responsibility from `DeeplinkProcessor`.

### Migration

- Replace processor calls that passed `Activity`:
  - Before: `processor.match(uri, activity = this)`
  - Now: `val params = processor.match(uri)`
- Move navigation/handling logic to the caller after `match` returns a non-null params object.
- Update builder setup to register specs without handlers.

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
