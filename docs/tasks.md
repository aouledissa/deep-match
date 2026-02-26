# Generated Tasks

DeepMatch adds Gradle tasks under the `deepmatch` group.

## `generate<Variant>DeeplinkSpecs`

Parses `.deeplinks.yml` and generates Kotlin sources for a variant.

- Generates `<ModuleName>DeeplinkParams` sealed interface.
- Generates `<ModuleName>DeeplinkProcessor` object.
- Generates one `*DeeplinkSpecs` property per spec.
- Generates one `*DeeplinkParams` class per spec.

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

Validates a URI against specs in `.deeplinks.yml`.

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

## List Tasks

To inspect all DeepMatch tasks in a module:

```bash
./gradlew :app:tasks --group deepmatch
```
