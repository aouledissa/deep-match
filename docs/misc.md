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
