![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/aouledissa/deep-match/unit-tests.yml?branch=main)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/aouledissa/deep-match/docs.yml?branch=main&label=docs)
![Maven Central Version](https://img.shields.io/maven-central/v/com.aouledissa.deepmatch/deepmatch-processor)
![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/com.aouledissa.deepmatch.gradle)


# DeepMatch

DeepMatch is an Android deep-linking toolkit that turns a `.deeplinks.yml` file into typed Kotlin
code, optional manifest entries, and runtime routing logic. Point the plugin at your configuration
and it will:

- Generate strongly-typed deeplink specs (plus parameter classes when the link declares template,
  query, or fragment values).
- Generate a module-level sealed params interface (for example, module `app` generates
  `AppDeeplinkParams`) that all generated params classes implement.
- Optionally emit manifest snippets so you never hand-write `<intent-filter>` entries again.
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

1. **Apply the plugin** to your Android module and configure it:

   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("com.aouledissa.deepmatch.gradle") version "<DEEPMATCH_VERSION>"
   }

   deepMatch {
       generateManifestFiles = true
   }
   ```

   Set `generateManifestFiles` to `false` if you prefer to manage `<intent-filter>` entries
   manually.

2. **Describe your deeplinks** in `.deeplinks.yml` located at the module root or under
   `src/<variant>/.deeplinks.yml`:

   ```yaml
   deeplinkSpecs:
     - name: "open series"
       activity: com.example.app.MainActivity
       scheme: [https, app]
       host: ["example.com"]
       pathParams:
         - name: series
         - name: seriesId
           type: numeric
       queryParams:
         - name: ref
           type: string
       fragment: "details"
   ```

   Both `scheme` and `host` accept multiple values; DeepMatch generates the appropriate regex
   matcher to cover every combination.

3. **Call the generated processor** at runtime:

   ```kotlin
   intent.data?.let { uri ->
       when (val params = AppDeeplinkProcessor.match(uri)) {
           is OpenSeriesDeeplinkParams -> {
               // Perform navigation/business logic using params.
           }
           null -> {
               // No matching deeplink
           }
       }
   }
   ```

   `AppDeeplinkProcessor` and `AppDeeplinkParams` are generated from your module name.

See [docs/gradle_plugin.md](docs/gradle_plugin.md) and
[docs/config_file.md](docs/config_file.md) for detailed configuration options and generated output.

## Testing

```bash
./gradlew publishToMavenLocal  # (Optional) publish plugin + libraries to ~/.m2 for downstream testing
./gradlew test                 # JVM unit tests for all modules (includes Robolectric coverage)
```

Runtime behaviour is exercised with Robolectric tests in `deepmatch-processor/src/test`. Shared
fixtures (fake processors and spec builders) live in the `deepmatch-testing` module and
can be reused in downstream projects.

## Documentation

- [docs/gradle_plugin.md](docs/gradle_plugin.md) – Plugin capabilities, setup, and build integration details.
- [docs/config_file.md](docs/config_file.md) – YAML specification reference with examples.
- [docs/migration-guide-0.2.0-alpha.md](docs/migration-guide-0.2.0-alpha.md) – Migration steps for the return-based runtime API.
- [docs/release-notes/0.2.0-alpha.md](docs/release-notes/0.2.0-alpha.md) – Release notes for the latest alpha changes.
- `deepmatch-testing/src/main/kotlin` – Reusable fakes and fixtures for tests.
- MkDocs site powered by Material theme. Serve locally with:

  ```bash
  pip install -r docs/requirements.txt
  mkdocs serve
  ```

  The `Docs` GitHub Action publishes the site automatically to GitHub Pages (branch `gh-pages`) on
  every push to `main` and tagged release. Once Pages is enabled in the repository settings, the
  documentation is available at `https://<owner>.github.io/DeepMatch/`.

## Contributing

Issues and pull requests are welcome. Please ensure `./gradlew test` passes locally (plus any
project-specific checks) before opening a PR.
