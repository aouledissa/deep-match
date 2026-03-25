# Security Policy

## Official Distribution

Official DeepMatch releases are published **exclusively** to the following authorized channels:

- **[Sonatype Central Portal (Maven Central)](https://central.sonatype.com/namespace/com.aouledissa.deepmatch)** — for the runtime library and API artifacts (`com.aouledissa.deepmatch`)
- **[Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.aouledissa.deepmatch.gradle)** — for the Gradle plugin (`com.aouledissa.deepmatch.gradle`)

These are the only authorized sources for DeepMatch artifacts. **The authors bear no responsibility whatsoever for the integrity, safety, or correctness of artifacts obtained from any other source.** This includes but is not limited to tampered binaries, malicious code injection, or broken functionality. You use such artifacts entirely at your own risk.

If you encounter DeepMatch artifacts distributed outside of these official channels, please report it by [opening an issue](https://github.com/aouledissa/deep-match/issues).

## YAML Schema

The official JSON Schema for `.deeplinks.yml` files is served exclusively from:

```
https://raw.githubusercontent.com/aouledissa/deep-match/main/schemas/deeplinks.schema.json
```

Do not use schemas hosted at any other URL. A schema from an unofficial source could silently misconfigure your IDE validation (for example, marking required fields as optional or accepting invalid configurations). If you suspect the schema URL has been replicated or tampered with elsewhere, please report it by [opening an issue](https://github.com/aouledissa/deep-match/issues).
