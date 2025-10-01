# DeepMatch

DeepMatch keeps your Android deeplinks consistent from configuration to runtime. The Gradle plugin
parses a `.deeplinks.yml` file and generates Kotlin sources plus optional manifest entries, while the
runtime library matches incoming URIs and dispatches to strongly-typed handlers.

## Key Components

- **deepmatch-plugin** — Gradle plugin you apply to Android modules to parse specs and generate code.
- **deepmatch-processor** — Runtime router that maps URIs to handlers and builds parameter objects.
- **deepmatch-api** — Shared spec/parameter data models used across plugin and runtime.
- **deepmatch-testing** — Reusable fixtures that assist in unit testing DeepMatch integrations.

## Getting Started

1. Apply the plugin alongside your Android/Kotlin plugins and enable manifest generation if desired.
2. Describe deeplinks in `.deeplinks.yml`; both module-level and variant-specific files are supported.
3. Register generated specs with `DeeplinkProcessor` to match incoming URIs at runtime.

For detailed configuration options, see the navigation links for the Gradle plugin and YAML schema.

## Building the Documentation

```bash
pip install -r docs/requirements.txt  # once
mkdocs serve                            # start live-reloading docs site
```

The documentation is built with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).
