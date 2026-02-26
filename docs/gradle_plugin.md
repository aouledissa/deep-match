### Core Capabilities

The primary responsibilities and capabilities of the DeepMatch Gradle plugin are:

1.  **YAML Configuration Parsing:**
    *   The plugin automatically locates and reads your `.deeplinks.yml` file (by default, from the root of the module, e.g., `app/.deeplinks.yml`, but this is configurable).
    *   It parses the YAML content, validating its structure against the expected format (implicitly defined by how it generates code and manifest entries, aligning with the `DeeplinkConfig` structure).

2.  **Android Manifest Generation:**
    *   Based on the parsed `deeplinkSpecs` from your YAML file, the plugin dynamically generates the necessary `<intent-filter>` entries within your app's `AndroidManifest.xml`.
    *   For each `DeeplinkConfig` entry in your YAML, it creates an `<activity>` (or merges with an existing one if the `activity` name matches) and adds an `<intent-filter>` to it.
    *   This automation means you **do not need to manually write or maintain these `<intent-filter>` tags** in your main `AndroidManifest.xml` for the deep links defined in your YAML. The plugin handles keeping them in sync with your YAML configuration.
    *   The generated manifest entries are typically merged into the final manifest during the build process (e.g., visible in `app/build/intermediates/merged_manifests/debug/AndroidManifest.xml`).

3.  **Code Generation (`DeeplinkSpecs`):**
    *   The plugin generates Kotlin source code, most notably a class named something like `[DeeplinkName]DeeplinkSpecs` (the exact name might vary based on the deeplink name in the `.yml` file).
    *   The plugin also generates params classes for deeplinks with typed path/query/fragment values.
    *   It additionally generates a module-level sealed interface (for example, module `app` -> `AppDeeplinkParams`) implemented by all generated params classes, enabling exhaustive `when` matching in app code.
    *   It generates a module-level processor object (for example, module `app` -> `AppDeeplinkProcessor`) wired with all generated specs so no manual registration is required.

4.  **Integration with Build Process:**
    *   The plugin hooks into the Android Gradle Plugin's build lifecycle.
    *   Its tasks (like YAML parsing, manifest generation, code generation) typically run before Java/Kotlin compilation, ensuring that the generated code and manifest entries are available when the rest of your app's code (including the codegen process) is processed.

5.  **Configuration Options:**
    *   The plugin provides a DSL (Domain Specific Language) extension in your `build.gradle` (`deepMatch { ... }` block) to customize its behavior:
        *   **generateManifestFiles:** Specifying whether or not the plugin should generate `AndroidManifest.xml` file based on the deeplink config `yaml` file.

### Benefits of Using the Plugin

*   **Single Source of Truth:** Your `.deeplinks.yml` becomes the definitive source for all your deep link definitions.
*   **Reduced Boilerplate:** No need to manually write complex and error-prone `<intent-filter>` tags in the manifest.
*   **Consistency:** Ensures that your manifest and the runtime parsing logic are always in sync with your declared specifications.
*   **Improved Maintainability:** Adding, removing, or modifying deep links is as simple as editing the YAML file and rebuilding.
*   **Build-Time Checks (Implicit):** While not a full validation suite, the parsing step can catch basic syntax errors in your YAML file early in the build process.

### Getting Started

1. Apply the plugin alongside your usual Android and Kotlin plugins:

    ```kotlin
    plugins {
        id("com.android.application")
        id("org.jetbrains.kotlin.android")
        id("com.aouledissa.deepmatch.gradle") version "0.1.0"
    }
    ```

2. Configure the extension in the same module:

    ```kotlin
    deepMatch {
        generateManifestFiles = true
    }
    ```

3. Add a `.deeplinks.yml` file at the module root (or under `src/<variant>/.deeplinks.yml`). See `docs/config_file.md` for the full schema.

During the build the plugin generates Kotlin sources under `build/generated/` and, when enabled, a manifest snippet under `build/generated/manifests/<variant>/AndroidManifest.xml`.

### Generated Artifacts

- `<ModuleName>DeeplinkParams.kt` — module-level sealed interface implemented by generated params classes.
- `<ModuleName>DeeplinkProcessor.kt` — module-level processor object extending `DeeplinkProcessor` and preconfigured with all generated specs.
- `*DeeplinkSpecs.kt` — exposes a `DeeplinkSpec` property per configuration entry.
  Generated specs use `pathParams` as an ordered list to preserve YAML-declared path segment order.
- `*DeeplinkParams.kt` — optional data class emitted when a deeplink defines typed template/query params or a fragment requirement (including fragment-only specs).
- Generated manifest file — contains `<intent-filter>` definitions that Gradle merges into the final manifest.

### Available Tasks

Each Android variant gets two dedicated tasks:

- `generate<Variant>DeeplinkSpecs` — parses YAML and produces Kotlin sources.
- `generate<Variant>DeeplinksManifest` — writes the manifest file when `generateManifestFiles` is `true`.

Inspect the generated output with:

```bash
./gradlew :app:generateDebugDeeplinkSpecs
``` 

### Testing & CI

```bash
./gradlew publishToMavenLocal  # Optional; handy when consuming the plugin from another project locally
./gradlew test                 # Runs JVM tests for the plugin, runtime, and shared fixtures
```

Runtime behaviour is validated with Robolectric-based tests in `deepmatch-processor/src/test`, backed by reusable fakes from the `deepmatch-testing` module. The GitHub Action workflow (`.github/workflows/ci.yml`) runs the same `test` task on every push and pull request; enable branch protection to require the **CI** workflow before merging.
