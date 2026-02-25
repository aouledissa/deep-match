# DeepMatch

DeepMatch keeps your Android deeplinks consistent from configuration to runtime. The Gradle plugin
parses a `.deeplinks.yml` file and generates Kotlin sources plus optional manifest entries, while the
runtime library matches incoming URIs and returns strongly-typed params.

## Key Components

- **deepmatch-plugin** — Gradle plugin you apply to Android modules to parse specs and generate code.
- **deepmatch-processor** — Runtime matcher that maps URIs to specs and builds parameter objects.
- **deepmatch-api** — Shared spec/parameter data models used across plugin and runtime.
- **deepmatch-testing** — Reusable fixtures that assist in unit testing DeepMatch integrations.

## Getting Started

1. Apply the plugin alongside your Android/Kotlin plugins and enable manifest generation if desired.
2. Describe deeplinks in `.deeplinks.yml`; both module-level and variant-specific files are supported.
3. Call the generated module processor (for example, `AppDeeplinkProcessor`) and use `match(uri)`
   to retrieve parsed params. Generated params classes share a module-level sealed interface (for
   example, `AppDeeplinkParams`) so your app can use exhaustive `when` matching.

For detailed configuration options, see the navigation links for the Gradle plugin and YAML schema.

## Upgrading

- Check **Release Notes** for version-specific changes.
- For the runtime refactor in `0.2.0-alpha`, follow **Migration Guide**.

## Building the Documentation

```bash
pip install -r docs/requirements.txt  # once
mkdocs serve                            # start live-reloading docs site
```

The documentation is built with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).
