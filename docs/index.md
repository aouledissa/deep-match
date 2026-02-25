# DeepMatch

DeepMatch keeps your Android deeplinks consistent from configuration to runtime. The Gradle plugin
parses a `.deeplinks.yml` file and generates Kotlin sources plus optional manifest entries, while the
runtime library matches incoming URIs and returns strongly-typed params.

## Key Components

- **deepmatch-plugin** — Gradle plugin you apply to Android modules to parse specs and generate code.
- **deepmatch-processor** — Runtime matcher that maps URIs to specs and builds parameter objects.
- **deepmatch-api** — Shared spec/parameter data models used across plugin and runtime.
- **deepmatch-testing** — Reusable fixtures that assist in unit testing DeepMatch integrations.

## Getting Started

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

3. Configure DeepMatch:

```kotlin
deepMatch {
    generateManifestFiles = true
}
```

4. Create `.deeplinks.yml` in your module root (or `src/<variant>/.deeplinks.yml`):

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
      - name: ref
        type: string
```

Typed query params are validated by key and type, so query ordering does not matter.
For example, `?ref=promo&page=1` and `?page=1&ref=promo` are treated the same.

5. Generate sources (or run a normal build):

```bash
./gradlew :app:generateDebugDeeplinkSpecs
```

6. Use the generated processor:

```kotlin
intent.data?.let { uri ->
    when (val params = AppDeeplinkProcessor.match(uri) as? AppDeeplinkParams) {
        is OpenSeriesDeeplinkParams -> {
            // use parsed params
        }
        null -> {
            // no match
        }
    }
}
```

7. Optional device test with ADB:

```bash
adb shell am start -W \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "app://example.com/series/42?ref=promo"
```

For an end-to-end sample app flow, see the repository sample at
`samples/android-app/README.md`. For schema/plugin details, see
[YAML Spec](config_file.md) and [Gradle Plugin](gradle_plugin.md).

## Upgrading

- Check **Release Notes** for version-specific changes.
- For the runtime refactor in `0.2.0-alpha`, follow **Migration Guide**.

## Building the Documentation

```bash
pip install -r docs/requirements.txt  # once
zensical serve                          # start live-reloading docs site
```

The documentation is built with [Zensical](https://zensical.io/).
