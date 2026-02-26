# Migration Guide: 0.2.0-alpha to 0.3.0-beta

This guide covers migration from `0.2.0-alpha` to `0.3.0-beta`.

## What Changed

- DeepMatch now discovers multiple spec files per module:
    - module root: `.deeplinks.yml` and `*.deeplinks.yml`
    - variant folder: `src/<variant>/.deeplinks.yml` and `src/<variant>/*.deeplinks.yml`
- Sources are merged in deterministic order:
    - root files first
    - variant files second
    - same spec `name` in later sources overrides earlier ones
- New optional HTML report generation:
    - `deepMatch { report { enabled = true } }`
    - generates a standalone catalogue + URI validator page
- Multi-module collision validation is wired into variant build/check.

## 1. Audit Spec File Layout

If your module only has a single `.deeplinks.yml`, no change is required.

If you use multiple files, verify naming and precedence:

- Keep shared/default specs in module root files.
- Put build-type or variant overrides under `src/<variant>/`.
- If the same spec `name` exists in both, variant/build-type overrides root.

## 2. Audit Duplicate Spec Names

Duplicate names are now treated differently depending on scope:

- Within the same file: build fails (still invalid).
- Across multiple files: later source overrides earlier source by `name`.

Action:

- Keep intentional overrides only (same `name`, different variant behavior).
- Rename accidental collisions so they remain distinct.

## 3. Optional: Enable the Deeplink Report

Add report config:

```kotlin
deepMatch {
    report {
        enabled = true
        // Optional:
        // output = layout.buildDirectory.file("reports/deeplinks.html")
    }
}
```

Generate it:

```bash
./gradlew :app:generateDeeplinkReport
```

Use it to validate URIs against specs in-browser without running the app.

## 4. Expect Collision Validation During Build

`validate<Variant>CompositeSpecsCollisions` now runs in variant build/check for composed projects.

If your build starts failing, it usually means two modules declare the same normalized URI shape.

Action:

- Make URI shapes unique across modules, or
- Consolidate ownership of overlapping routes in one module.

## 5. Optional: Use `validateDeeplinks` in CI/Debugging

You can validate a URI quickly from Gradle:

```bash
./gradlew :app:validateDeeplinks --uri='app://example.com/profile/42?ref=promo'
```

This is useful for smoke checks without launching the app.
