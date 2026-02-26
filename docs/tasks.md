# Generated Tasks

DeepMatch adds Gradle tasks under the `deepmatch` group.

## `generate<Variant>DeeplinkSpecs`

Parses discovered deeplink spec files and generates Kotlin sources for a variant.

- Generates `<ModuleName>DeeplinkParams` sealed interface.
- Generates `<ModuleName>DeeplinkProcessor` object.
- Generates one `*DeeplinkSpecs` property per spec.
- Generates one `*DeeplinkParams` class per spec.
- Reads `.deeplinks.yml` and `*.deeplinks.yml` from module root and `src/<variant>/`.
- Merges root first, then variant; same-name specs in later sources override earlier ones.

Example:

```bash
./gradlew :app:generateDebugDeeplinkSpecs
```

## `generate<Variant>DeeplinksManifest`

Generates deeplink `<intent-filter>` entries for a variant.

- Available when `deepMatch { generateManifestFiles = true }`.
- Writes a generated manifest file under `build/generated/manifests/<Variant>/`.
- Output is merged by AGP into the final app manifest.

Example:

```bash
./gradlew :app:generateDebugDeeplinksManifest
```

## `validateDeeplinks`

Validates a URI against merged specs from discovered deeplink YAML files.

- Requires `--uri`.
- Prints `[MATCH]` and `[MISS]` per spec.
- Prints extracted params for matching specs.
- Useful for local checks and CI debugging.

Example:

```bash
./gradlew :app:validateDeeplinks --uri='app://example.com/profile/42?ref=promo'
```

## `validate<Variant>CompositeSpecsCollisions`

Validates URI-shape collisions across composed module specs for a variant.

- Collects generated spec-shape metadata from the current module and DeepMatch-enabled dependency modules.
- Fails the build when two modules declare specs with the same normalized URI shape.
- Runs automatically as part of variant build/check flow.

Example:

```bash
./gradlew :app:validateDebugCompositeSpecsCollisions
```

## `generateDeeplinkReport`

Generates a single self-contained HTML report from local specs plus composed dependency-module specs.

- Enabled with:
  - `deepMatch { report { enabled = true } }`
- Output defaults to `build/reports/deeplinks.html`.
- Output can be overridden with:
  - `deepMatch { report { output = layout.buildDirectory.file("reports/custom.html") } }`
- Includes:
  - Full catalog (combined).
  - Module/file catalog entries when multiple sources are present.
  - Live URI validator.
  - Near-miss diagnostics (for example, missing required query params).
  - Quick test URI buttons generated from specs.
  - Browser-side URI validation against generated specs without running the app.

Example:

```bash
./gradlew :app:generateDeeplinkReport
```

## List Tasks

To inspect all DeepMatch tasks in a module:

```bash
./gradlew :app:tasks --group deepmatch
```
