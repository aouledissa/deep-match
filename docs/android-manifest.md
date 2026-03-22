---
description: How DeepMatch automatically generates and validates AndroidManifest.xml intent filters from your YAML deeplink specs, keeping them always in sync.
---

# AndroidManifest Integration

DeepMatch keeps your app's `AndroidManifest.xml` intent filters in sync with your deeplink specs. This guide explains how the plugin manages manifest generation and validation.

## Overview

For each deeplink spec in your YAML, the plugin generates an `<intent-filter>` entry. These entries tell Android which URIs your app can handle and which activity should receive them.

The plugin offers two workflows:

1. **Automatic (recommended)** — The plugin generates manifest entries; you maintain only the YAML specs.
2. **Manual** — You maintain both YAML specs and manifest entries yourself; the plugin validates they stay in sync.

## Automatic Generation (generateManifestFiles = true)

When `generateManifestFiles = true`, the plugin:

1. Parses all `.deeplinks.yml` and `*.deeplinks.yml` files in your module
2. Generates an `<intent-filter>` for each spec
3. Writes the result to `build/generated/manifests/<Variant>/AndroidManifest.xml`
4. AGP automatically merges this file into your app's final manifest

### Configuration

```kotlin
deepMatch {
    generateManifestFiles = true  // Enable automatic generation
}
```

### Generated Output

For a spec like:

```yaml
deeplinkSpecs:
  - name: "profile"
    activity: com.example.app.ProfileActivity
    scheme: [https, app]
    host: ["example.com"]
    pathParams:
      - name: userId
        type: numeric
```

The plugin generates:

```xml
<activity android:name="com.example.app.ProfileActivity" android:exported="true" tools:node="merge">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="https" android:host="example.com" android:pathPrefix="/profile/" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="app" android:host="example.com" android:pathPrefix="/profile/" />
    </intent-filter>
</activity>
```

### Where Generated Files Are Merged

The generated manifest snippet is merged by AGP at:

```
app/build/intermediates/merged_manifests/<variant>/AndroidManifest.xml
```

You can inspect this file to see the final merged result after a build.

### Key Details

- **Order:** Each combination of `(scheme, host)` gets its own intent filter for granular routing control.
- **Path Handling:** Path types are chosen automatically based on your spec:
  - Static paths → `android:path` (with trailing-slash variant)
  - Typed-at-end paths → `android:pathPrefix`
  - Typed-in-middle paths → `android:pathPattern`
  - Typed paths on SDK 31+ → `android:pathAdvancedPattern`
- **Query & Fragment:** Not emitted in the manifest; validated at runtime by `DeeplinkProcessor`.
- **Port:** If declared in the spec, `android:port` is generated.
- **Hostless Specs:** Omit `android:host`; for custom schemes, include `tools:ignore="AppLinkUrlError"` for AGP 9+ lint compatibility.
- **autoVerify:** For specs with `autoVerify: true` that mix web schemes (http/https) with custom schemes, web schemes are split into separate intent filters marked `android:autoVerify="true"`.

---

## Manual Maintenance (generateManifestFiles = false)

When you disable automatic generation, you manually add intent filters to `AndroidManifest.xml` instead:

```kotlin
deepMatch {
    generateManifestFiles = false  // Disable automatic generation
}
```

You then maintain `<intent-filter>` entries directly in your manifest:

```xml
<activity android:name="com.example.app.ProfileActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="https" android:host="example.com" android:pathPrefix="/profile/" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="app" android:host="example.com" android:pathPrefix="/profile/" />
    </intent-filter>
</activity>
```

### Why Use Manual Mode?

- You have custom manifest logic that doesn't fit the YAML spec pattern
- You're migrating from a non-DeepMatch app and want to keep existing manifest structure
- You prefer explicit manifest control over code generation

### Responsibilities

When manual, **you are responsible for**:

- Keeping every deeplink spec in your YAML matched by an `<intent-filter>` in the manifest
- Using the correct scheme, host, and path attributes
- Maintaining both as your specs evolve

This is error-prone — if you add a spec but forget the intent filter, the deeplink won't work at runtime.

---

## Sync Validation

To catch mismatches, the plugin registers a `warn<Variant>ManifestOutOfSync` task that runs during `check`.

This task:

1. Compares every `(activity, scheme, host)` tuple from your YAML specs
2. Checks the variant's **merged** manifest for matching intent filters
3. Reports any that are missing

### Configuration

The `manifestSyncViolation` field controls how the plugin responds to missing intent filters:

```kotlin
deepMatch {
    generateManifestFiles = false

    // Log warnings (default)
    manifestSyncViolation = ManifestSyncViolation.WARN

    // Or fail the build in CI
    manifestSyncViolation = ManifestSyncViolation.FAIL
}
```

| Value  | Behavior |
|--------|----------|
| `WARN` | Logs a warning per missing intent filter. Build succeeds. **(default)** |
| `FAIL` | Throws an exception listing all missing filters. Build fails. |

### Running Manually

```bash
./gradlew :app:warnDebugManifestOutOfSync
```

### Example Output (WARN mode)

```
[DeepMatch] WARNING: 'generateManifestFiles' is disabled but the following deeplink
intent filters appear to be missing from AndroidManifest.xml:
  - activity: com.example.app.ProfileActivity | scheme: https | host: example.com
  - activity: com.example.app.ProfileActivity | scheme: app | host: example.com
```

### Important: The Task Checks the Merged Manifest

The validation reads your variant's **merged** manifest, which includes:

- `src/main/AndroidManifest.xml`
- `src/<buildType>/AndroidManifest.xml` (e.g., `src/debug/`)
- `src/<flavor>/AndroidManifest.xml`
- Intent filters contributed by library dependencies

This means intent filters can come from multiple source files — only the final merged result matters.

---

## Best Practices

1. **Prefer Automatic Generation**
   - Simpler to maintain
   - Eliminates drift by design
   - Use manual mode only when you have a specific reason

2. **When Using Manual Mode, Enable Strict Checks in CI**
   - Set `manifestSyncViolation = FAIL` in CI
   - Prevents missing intent filters from reaching production

3. **Inspect Merged Manifests During Development**
   - Check `build/intermediates/merged_manifests/<variant>/AndroidManifest.xml` when debugging deeplink routing
   - This is what Android actually sees, not your source manifest

4. **Document Custom Manifest Logic**
   - If manually maintaining manifests, document why
   - Add comments explaining non-standard intent filters

5. **Test Deeplinks on Device**
   - Use `adb shell am start` to test your deeplinks
   - Validates that specs match the actual manifest at runtime

   ```bash
   adb shell am start -W \
     -a android.intent.action.VIEW \
     -c android.intent.category.BROWSABLE \
     -d "https://example.com/profile/42"
   ```
