![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/aouledissa/deep-match/unit-tests.yml?branch=main)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/aouledissa/deep-match/docs.yml?branch=main&label=docs)
![Maven Central Version](https://img.shields.io/maven-central/v/com.aouledissa.deepmatch/deepmatch-processor)
![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/com.aouledissa.deepmatch.gradle)


# DeepMatch

DeepMatch is an Android deep-linking toolkit that turns deeplink YAML spec files into typed Kotlin
code, optional manifest entries, and runtime routing logic. Point the plugin at your configuration
and it will:

- Generate strongly-typed deeplink specs and one parameter class per deeplink spec.
- Generate a module-level sealed params interface (for example, module `app` generates
  `AppDeeplinkParams`) that all generated params classes implement.
- Optionally emit manifest snippets so you never hand-write `<intent-filter>` entries again.
- Optionally generate a standalone HTML deeplink report with a live URI validator.
- Provide a lightweight runtime (`deepmatch-processor`) that matches URIs against the generated
  specs and returns typed params.

The YAML file becomes the single source of truth for everything deeplink-related.

## Documentation
For full documentation please visit our [official docs page](https://aouledissa.com/deep-match/)

## Modules

- `deepmatch-plugin` – Gradle plugin (`com.aouledissa.deepmatch.gradle`) that parses specs,
  generates Kotlin sources, and (optionally) produces manifest entries for each variant.
- `deepmatch-processor` – Android library that provides `DeeplinkProcessor` for runtime URI
  matching and typed params extraction.
- `deepmatch-api` – Shared model classes (`DeeplinkSpec`, `Param`, `ParamType`, `DeeplinkParams`).
- `deepmatch-testing` – Shared test fixtures (fake processors and spec builders) used by the runtime
  and plugin tests.
- `samples/android-app` – Composite-build sample app demonstrating generated processor usage,
  manifest generation, and ADB deeplink testing. See
  [`samples/android-app/README.md`](samples/android-app/README.md).

## Quick Start

1. Add the plugin to your Android module:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.aouledissa.deepmatch.gradle") version "<DEEPMATCH_VERSION>"
}
```

2. Add the runtime dependency:

```kotlin
dependencies {
    implementation("com.aouledissa.deepmatch:deepmatch-processor:<DEEPMATCH_VERSION>")
}
```

3. Configure plugin behavior:

```kotlin
deepMatch {
    generateManifestFiles = true
    report {
        enabled = true
        // Optional override (default: build/reports/deeplinks.html)
        // output = layout.buildDirectory.file("reports/deeplinks.html")
    }
}
```

DeepMatch automatically composes processors from project dependencies that also apply the plugin.

Set `generateManifestFiles = false` if you want to manage `<intent-filter>` entries manually.

4. Create one or more spec files in your module:
- Module root: `.deeplinks.yml` or `*.deeplinks.yml`
- Variant folder: `src/<variant>/.deeplinks.yml` or `src/<variant>/*.deeplinks.yml`
- Merge precedence is deterministic: root files first, then variant files. If two files define the
  same spec `name`, the later source overrides the earlier one.
- Explicitly: when a build-type/variant file and a module-root file define the same spec `name`,
  the build-type/variant definition wins.

```yaml
deeplinkSpecs:
  - name: "open series"
    activity: com.example.app.MainActivity
    categories: [DEFAULT, BROWSABLE]
    scheme: [https, app]
    host: ["example.com"]
    pathParams:
      - name: series
      - name: seriesId
        type: numeric
    queryParams:
      - name: query
        type: string
        required: true
      - name: ref
        type: string
    fragment: "details"
```

- Typed query params are validated by key and type, so query ordering does not matter.
For example, `?ref=promo&page=1` and `?page=1&ref=promo` are treated the same.
- Query params are optional by default; use `required: true` for mandatory keys.
- Scheme and host matching are case-insensitive (for example, `HTTPS://Example.COM/...` matches
`scheme: [https]` + `host: ["example.com"]`).
- Host is optional; omit it (or set `host: []`) for hostless URIs such as `app:///profile/123`.
- Path params are ordered and matched by position as declared in YAML.
- Each deeplink spec always generates a `*DeeplinkParams` type, so a successful match is never
ambiguous with "no match". When declared, `fragment` is exposed in the generated params type.

5. Generate sources (or just build normally):

```bash
./gradlew :app:generateDebugDeeplinkSpecs
```

DeepMatch generates:
- `<ModuleName>DeeplinkProcessor` (example: `AppDeeplinkProcessor`)
- `<ModuleName>DeeplinkParams` sealed interface (example: `AppDeeplinkParams`)
- `*DeeplinkSpecs` and `*DeeplinkParams` classes

6. Use the generated processor at runtime:

```kotlin
intent.data?.let { uri ->
    when (val params = AppDeeplinkProcessor.match(uri) as? AppDeeplinkParams) {
        is OpenSeriesDeeplinkParams -> {
            // Navigate using params.seriesId / params.ref
        }
        null -> {
            // No deeplink matched
        }
    }
}
```

Advanced runtime APIs:
- `CompositeDeeplinkProcessor` to chain multiple processors and return the first match.

7. Optional real-device smoke test via ADB:

```bash
adb shell am start -W \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "app://example.com/series/42?ref=promo"
```

For an end-to-end reference app (Compose UI + generated manifest + ADB tests), see
[`samples/android-app/README.md`](samples/android-app/README.md).

8. Optional report generation:

```bash
./gradlew :app:generateDeeplinkReport
```

This generates a single self-contained HTML report at `build/reports/deeplinks.html` with:
- Full catalog merged from discovered local spec files (and composed dependency modules when present).
- Live URI validator with near-miss diagnostics.
- Quick test URI buttons generated from specs.
- URI validation directly in the browser, without building or running the app.

For full configuration/schema details, see [Plugin](docs/plugin.md) and
[Deeplink Specs](docs/deeplink-specs.md).

## Testing

```bash
./gradlew publishToMavenLocal  # (Optional) publish plugin + libraries to ~/.m2 for downstream testing
./gradlew test                 # JVM unit tests for all modules (includes Robolectric coverage)
```

Runtime behaviour is exercised with Robolectric tests in `deepmatch-processor/src/test`. Shared
fixtures (fake processors and spec builders) live in the `deepmatch-testing` module and
can be reused in downstream projects.

## Documentation

- [Plugin](docs/plugin.md) – Plugin capabilities, setup, and build integration details.
- [Deeplink Specs](docs/deeplink-specs.md) – YAML specification reference with examples.
- [Composite Specs](docs/composite-specs.md) – How module-level processors are auto-composed and how match precedence works.
- [Tasks](docs/tasks.md) – Generated Gradle tasks including `validateDeeplinks`.
- [Report](docs/report.md) – Standalone deeplink catalog + live URI validator output.
- [Migration-guide-0.3.0-beta.md](docs/migration-guide-0.3.0-beta.md) – Migration steps for multi-file specs, report generation, and override precedence.
- [Migration-guide-0.2.0-alpha.md](docs/migration-guide-0.2.0-alpha.md) – Migration steps for the return-based runtime API.
- [Release-notes/0.3.0-beta.md](docs/release-notes/0.3.0-beta.md) – Release notes for the latest beta changes.
- [Release-notes/0.2.0-alpha.md](docs/release-notes/0.2.0-alpha.md) – Release notes for the previous alpha changes.
- `deepmatch-testing/src/main/kotlin` – Reusable fakes and fixtures for tests.
- Documentation site powered by Zensical. Serve locally with:

  ```bash
  pip install -r docs/requirements.txt
  zensical serve
  ```

  The `Docs` GitHub Action publishes the site automatically to GitHub Pages (branch `gh-pages`) on
  every push to `main` and tagged release. Once Pages is enabled in the repository settings, the
  documentation is available at `https://<owner>.github.io/DeepMatch/`.

## Contributing

Issues and pull requests are welcome. Please ensure `./gradlew test` passes locally (plus any
project-specific checks) before opening a PR.
