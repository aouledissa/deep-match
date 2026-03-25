---
description: Additional DeepMatch features including IDE autocomplete support for YAML spec files and other developer experience improvements.
---

# Miscellaneous

## IDE Autocomplete Support

DeepMatch provides a JSON Schema for `.deeplinks.yml` and `.deeplinks.yaml` files that enables syntax autocomplete and validation in your IDE.

### VS Code

- Install the [YAML extension](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-yaml) by Red Hat
- Autocomplete and validation are automatically enabled for `*.deeplinks.yml`, `.deeplinks.yml`, `*.deeplinks.yaml`, and `.deeplinks.yaml` files

### Android Studio / IntelliJ IDEA

- Schema support is automatically configured when you open the project
- Autocomplete and validation are enabled for `*.deeplinks.yml`, `.deeplinks.yml`, `*.deeplinks.yaml`, and `.deeplinks.yaml` files

### Other Tools

The schema is also registered with [SchemaStore](https://www.schemastore.org/), so any tool that supports SchemaStore will automatically provide autocomplete for deeplink files.

## Official Distribution

Official DeepMatch releases are published **exclusively** to the following authorized channels:

- **[Sonatype Central Portal (Maven Central)](https://central.sonatype.com/)** — for the runtime library and API artifacts (`com.aouledissa.deepmatch`)
- **[Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.aouledissa.deepmatch.gradle)** — for the Gradle plugin (`com.aouledissa.deepmatch.gradle`)

These are the only authorized sources for DeepMatch artifacts.

> **Only consume artifacts sourced from Maven Central or the Gradle Plugin Portal.**
> Any artifact distributed through any other channel — including but not limited to third-party Maven repositories, file hosting, or unofficial mirrors — is not authorized and has not been verified by the maintainers.
>
> **The authors bear no responsibility whatsoever for the integrity, safety, or correctness of artifacts obtained from any source other than Sonatype Central Portal or the Gradle Plugin Portal.** This includes but is not limited to tampered binaries, malicious code injection, or broken functionality. You use such artifacts entirely at your own risk.

If you encounter DeepMatch artifacts distributed outside of these official channels, please report it to the project owners by [opening an issue](https://github.com/aouledissa/deep-match/issues).
