# DeepMatch

DeepMatch is an Android deep-linking toolkit that turns a `.deeplinks.yml` file into typed Kotlin
code, optional manifest entries, and runtime routing logic. Point the plugin at your configuration
and it will:

- Generate strongly-typed deeplink specs (plus parameter classes when the link declares template,
  query, or fragment values).
- Optionally emit manifest snippets so you never hand-write `<intent-filter>` entries again.
- Provide a lightweight runtime (`deepmatch-processor`) that matches URIs against the generated
  specs and dispatches to handlers.

The YAML file becomes the single source of truth for everything deeplink-related.

## Modules

- `deepmatch-plugin` – Gradle plugin (`com.aouledissa.deepmatch.gradle`) that parses specs,
  generates Kotlin sources, and (optionally) produces manifest entries for each variant.
- `deepmatch-processor` – Android library that provides `DeeplinkProcessor` and handler abstractions
  for runtime matching.
- `deepmatch-api` – Shared model classes (`DeeplinkSpec`, `Param`, `ParamType`, `DeeplinkParams`).
- `deepmatch-testing` – Shared test fixtures (fake handlers/processors and spec builders) used by
  the runtime and plugin tests.

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

3. **Register the generated specs** with the runtime processor:

   ```kotlin
   val processor = DeeplinkProcessor.Builder()
       .register(OpenSeriesDeeplinkSpecs, OpenSeriesDeeplinkHandler)
       .build()

   intent.data?.let { uri ->
       processor.match(uri, activity = this)
   }
   ```

See `docs/gradle_plugin.md` and `docs/config_file.md` for detailed configuration options and
generated output.

## Testing

```bash
./gradlew publishToMavenLocal  # (Optional) publish plugin + libraries to ~/.m2 for downstream testing
./gradlew test                 # JVM unit tests for all modules (includes Robolectric coverage)
```

Runtime behaviour is exercised with Robolectric tests in `deepmatch-processor/src/test`. Shared
fixtures (fake handlers, processors, and spec builders) live in the `deepmatch-testing` module and
can be reused in downstream projects.

## Documentation

- `docs/gradle_plugin.md` – Plugin capabilities, setup, and build integration details.
- `docs/config_file.md` – YAML specification reference with examples.
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
