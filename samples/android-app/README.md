# DeepMatch Sample App

This sample demonstrates zero-config runtime usage with generated code and real deeplink ingestion:

- `.deeplinks.yml` is the source of truth.
- The plugin generates `AppDeeplinkProcessor` and typed params classes.
- The app is implemented with Jetpack Compose and calls `AppDeeplinkProcessor.match(uri)` with exhaustive `when` handling.
- Deeplink intent filters are generated from YAML (`generateManifestFiles = true`).

## Run from this repository

From repo root:

```bash
./gradlew -p samples/android-app generateDebugDeeplinkSpecs
./gradlew -p samples/android-app generateDebugDeeplinksManifest
./gradlew -p samples/android-app assembleDebug
```

## Real deeplink smoke test (ADB)

Install and launch deeplinks against the app:

```bash
adb install -r samples/android-app/build/outputs/apk/debug/android-app-debug.apk
```

```bash
adb shell am start -W \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "app://sample.deepmatch.dev/series/42?ref=adb"
```

```bash
adb shell am start -W \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "app://sample.deepmatch.dev/profile/john123?ref=campaign#details"
```

The screen should render parsed output for `OpenSeriesDeeplinkParams` and `OpenProfileDeeplinkParams`.

## Notes

- The sample uses composite build (`includeBuild("../..")`) to consume the local plugin and modules.
- No `publishToMavenLocal` step is required for local development.
