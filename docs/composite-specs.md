# Composite Specs

DeepMatch supports both:

- Multiple deeplink specs in one module.
- Multiple Android modules, each with its own DeepMatch-generated processor.

## How to Structure Specs

Each module can define:

- Module root sources: `.deeplinks.yml` and `*.deeplinks.yml`
- Variant sources: `src/<variant>/.deeplinks.yml` and `src/<variant>/*.deeplinks.yml`

Discovery and merge rules:

1. Root files are loaded first (sorted by file name).
2. Variant files are loaded next (sorted by file name).
3. If two sources define the same spec `name`, the later source overrides the earlier one.

## Multi-Module Setup

Example project:

```text
:app
:feature-profile
:feature-series
```

Apply the plugin in each participating module:

```kotlin
plugins {
    id("com.android.library") // or com.android.application
    // AGP 9+: Kotlin is built into AGP (do not apply org.jetbrains.kotlin.android)
    // AGP 8.x and below: add id("org.jetbrains.kotlin.android")
    id("com.aouledissa.deepmatch.gradle")
}
```

If `:app` depends on `:feature-profile` and `:feature-series`, the app's generated processor
automatically composes their generated processors.

No extra plugin configuration is required.

## What Gets Generated

For each module:

- `<ModuleName>DeeplinkParams` (sealed interface)
- `<ModuleName>DeeplinkProcessor` (generated processor object)
- One `*DeeplinkSpecs` + one `*DeeplinkParams` per spec

For app modules with DeepMatch-enabled dependencies, `<ModuleName>DeeplinkProcessor` is generated
as a `CompositeDeeplinkProcessor`.

## Resolution Order

When you call:

```kotlin
val result = AppDeeplinkProcessor.match(uri)
```

DeepMatch resolves in this order:

1. Local module specs are evaluated first.
2. Local specs come from merged discovered files (root first, then variant with override-by-name).
3. If local specs do not match, composed dependency processors are tried (deterministic order).
4. The first non-null match wins.
5. If nothing matches, result is `null`.

### Practical Implication

If two modules can match the same URI shape, precedence is determined by composition order.
Dependency processors are composed in deterministic FQCN order. Keep specs unique across modules
whenever possible to avoid ambiguous ownership.

## Example

```kotlin
when (val params = AppDeeplinkProcessor.match(uri)) {
    is OpenProfileDeeplinkParams -> { /* app module flow */ }
    is com.example.feature.profile.deeplinks.OpenFeatureProfileDeeplinkParams -> { /* feature flow */ }
    null -> { /* no match */ }
}
```

In composed mode, result type is `DeeplinkParams?` and can come from dependent modules too.
Use concrete generated params types from each participating module, or call module processors
directly when you want module-local exhaustive `when`.

## Collision Validation

DeepMatch also validates composed URI-shape collisions per variant.

- Task name: `validate<Variant>CompositeSpecsCollisions` (for example, `validateDebugCompositeSpecsCollisions`).
- It compares normalized spec shapes across app + composed dependency modules.
- If two modules declare the same shape, build fails with a detailed collision report.

This protects multi-module projects from accidental first-match routing conflicts.
