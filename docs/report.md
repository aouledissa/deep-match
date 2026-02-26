# Deeplink Report

DeepMatch can generate a standalone HTML report that combines:

- A full catalog merged from discovered local spec files.
- Catalog views grouped by module and source file when multiple inputs exist.
- A live URI validator.
- Near-miss diagnostics (for example, required query param missing).
- Quick test URI buttons generated from spec examples.

## Enable

```kotlin
deepMatch {
    report {
        enabled = true
        // Optional override (default: build/reports/deeplinks.html)
        // output = layout.buildDirectory.file("reports/deeplinks.html")
    }
}
```

## Generate

```bash
./gradlew :app:generateDeeplinkReport
```

## Output

Default output:

```text
<module>/build/reports/deeplinks.html
```

The file is fully self-contained (no external CSS/JS dependencies), so it can be:

- Opened locally in a browser.
- Shared as a single file.
- Uploaded as a CI artifact.
- Hosted as a static page.

## Validation Behavior

The report validator mirrors runtime matching semantics:

- Case-insensitive scheme/host matching.
- Structural matching via generated deeplink regex.
- Query params validated by key (order-independent).
- Optional query params validated only when present.
- Required query params produce a near-miss diagnostic when absent.
- Trailing slashes normalized for path matching.

## Input Discovery

`generateDeeplinkReport` uses the same file discovery rules as code generation:

- Module root: `.deeplinks.yml` and `*.deeplinks.yml`
- Variant folder: `src/<variant>/.deeplinks.yml` and `src/<variant>/*.deeplinks.yml`

In composed projects, dependency modules that apply DeepMatch are included in the report.

## Catalog Navigation

The report drawer adapts to your project layout:

- Single module + single source file: only the combined Deeplink Catalogue page is shown.
- Single module + multiple source files: child entries are shown per source file.
- Multiple modules + single source each: child entries are shown per module.
- Multiple modules + multiple sources: modules expand to nested source-file entries.
