# DeepMatch

DeepMatch is an Android deep-linking toolkit designed to offset deeplink validation & handling using:

- A Gradle plugin that turns a `.deeplinks.yml` specification into Kotlin sources and optional manifest entries.
- A lightweight runtime (`deepmatch-processor`) that matches URIs against the generated specs and routes them to strongly-typed handlers.

The plugin keeps deep links in sync across build-time metadata, generated code, and runtime handling so teams can treat the YAML file as their single source of truth.

## Modules

- `deepmatch-plugin` – Gradle plugin (`com.aouledissa.deepmatch.plugin.android`) that parses specs, generates Kotlin sources, and (optionally) produces manifest entries for each variant.
- `deepmatch-processor` – Android library that provides `DeeplinkProcessor` and handler abstractions for runtime matching.
- `deepmatch-api` – Shared model classes (`DeeplinkSpec`, `Param`, `ParamType`, `DeeplinkParams`).

## Quick Start

1. **Apply the plugin** to your Android module and configure it:

   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("com.aouledissa.deepmatch.plugin.android") version "<DEEP_MATCH_VERSION>"
   }

   deepMatch {
       generateManifestFiles = true
   }
   ```

   Set `generateManifestFiles` to `false` if you prefer to manage `<intent-filter>` entries manually.

2. **Describe your deeplinks** in `.deeplinks.yml` located at the module root or under `src/<variant>/.deeplinks.yml`:

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

3. **Register the generated specs** with the runtime processor:

   ```kotlin
   val processor = DeeplinkProcessor.Builder()
       .register(OpenSeriesDeeplinkSpecs, OpenSeriesDeeplinkHandler)
       .build()

   intent.data?.let { uri ->
       processor.match(uri, activity = this)
   }
   ```

See `docs/gradle_plugin.md` and `docs/config_file.md` for detailed configuration options and generated output.

## Testing

```bash
./gradlew deepmatch-plugin:publishToMavenLocal       # Publish plugin/runtime artifacts required by the build
./gradlew test                      # JVM unit tests for all modules
./gradlew connectedDebugAndroidTest # Instrumentation tests (requires emulator/device)
```

Instrumentation tests live under `deepmatch-processor/src/androidTest` and exercise the runtime against real `Activity` instances.

## Documentation

- `docs/gradle_plugin.md` – Plugin capabilities, setup, and build integration details.
- `docs/config_file.md` – YAML specification reference with examples.

## Contributing

Issues and pull requests are welcome. Please ensure the CI workflow passes locally (`./gradlew test` and `./gradlew connectedDebugAndroidTest`) before opening a PR.
