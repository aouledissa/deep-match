# Changelog

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

