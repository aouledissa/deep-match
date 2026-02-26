# DeepMatch

DeepMatch keeps your Android deeplinks consistent from configuration to runtime. The Gradle plugin
parses deeplink YAML spec files and generates Kotlin sources plus optional manifest entries, while the
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

By default, DeepMatch auto-composes processors from project dependencies that also apply the plugin.

4. Create one or more spec files in your module:
- Module root: `.deeplinks.yml` or `*.deeplinks.yml`
- Variant folder: `src/<variant>/.deeplinks.yml` or `src/<variant>/*.deeplinks.yml`
- Merge precedence is root first, then variant. Same-name specs in later sources override earlier ones.

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
```

Typed query params are validated by key and type, so query ordering does not matter.
For example, `?ref=promo&page=1` and `?page=1&ref=promo` are treated the same.
Query params are optional by default; use `required: true` for mandatory keys.
Path params are ordered and matched by position as declared in YAML.
If `port` is declared, it is matched at runtime and emitted in generated manifest filters.

5. Generate sources (or run a normal build):

```bash
./gradlew :app:generateDebugDeeplinkSpecs
```

Need task details or URI validation? See [Tasks](tasks.md).
Need a human-friendly catalog and browser validator (full catalog + source/module views)? See [Report](report.md).
The report validates URIs against your specs in-browser, so no app run is required.

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

Runtime APIs are also available when you want custom wiring:

- `CompositeDeeplinkProcessor` to chain processors and return the first match.

7. Optional device test with ADB:

```bash
adb shell am start -W \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "app://example.com/series/42?ref=promo"
```

For an end-to-end sample app flow, see the repository sample at
`samples/android-app/README.md`. For schema/plugin details, see
[Deeplink Specs](deeplink-specs.md), [Plugin](plugin.md), and
[Composite Specs](composite-specs.md).

## Upgrading

- Check **Release Notes** for version-specific changes.
- For multi-file specs/report/override precedence in `0.3.0-beta`, follow **Migration Guide**.
- For the runtime refactor in `0.2.0-alpha`, follow **Migration Guide**.

## Building the Documentation

```bash
pip install -r docs/requirements.txt  # once
zensical serve                          # start live-reloading docs site
```

The documentation is built with [Zensical](https://zensical.io/).
