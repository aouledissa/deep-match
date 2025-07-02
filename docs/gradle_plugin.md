### Core Capabilities

The primary responsibilities and capabilities of the `deepmatch-gradle-plugin` are:

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
    *   The plugin also generate another kotlin source for the dynamic params in the configured deeplink. This means if any of your deeplink's **path parameters** is a pattern based or if the deeplink contains at least one **query parameter**, A parameter class will be generated.
    *   This makes it easy to access these parameters later when matching and handling the deeplink. 

4.  **Integration with Build Process:**
    *   The plugin hooks into the Android Gradle Plugin's build lifecycle.
    *   Its tasks (like YAML parsing, manifest generation, code generation) typically run before Java/Kotlin compilation, ensuring that the generated code and manifest entries are available when the rest of your app's code (including the codegen process) is processed.

5.  **Configuration Options:**
    *   The plugin provides a DSL (Domain Specific Language) extension in your `build.gradle` (`deepMatch { ... }` block) to customize its behavior:
        *   **specsFile:** Specifying a custom path to your `.deeplinks.yml` file if it's not in the default location.
        *   **generateManifestFiles:** Specifying whether or not the plugin should generate `AndroidManifest.xml` file based on the deeplink config `yaml` file.

### Benefits of Using the Plugin

*   **Single Source of Truth:** Your `.deeplinks.yml` becomes the definitive source for all your deep link definitions.
*   **Reduced Boilerplate:** No need to manually write complex and error-prone `<intent-filter>` tags in the manifest.
*   **Consistency:** Ensures that your manifest and the runtime parsing logic are always in sync with your declared specifications.
*   **Improved Maintainability:** Adding, removing, or modifying deep links is as simple as editing the YAML file and rebuilding.
*   **Build-Time Checks (Implicit):** While not a full validation suite, the parsing step can catch basic syntax errors in your YAML file early in the build process.
