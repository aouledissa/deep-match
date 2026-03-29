# Security Policy

## Official Distribution

Official DeepMatch releases are published **exclusively** to the following authorized channels:

- **[Sonatype Central Portal (Maven Central)](https://central.sonatype.com/namespace/com.aouledissa.deepmatch)** — for the runtime library and API artifacts (`com.aouledissa.deepmatch`)
- **[Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.aouledissa.deepmatch.gradle)** — for the Gradle plugin (`com.aouledissa.deepmatch.gradle`)

These are the only authorized sources for DeepMatch artifacts. **The authors bear no responsibility whatsoever for the integrity, safety, or correctness of artifacts obtained from any other source.** This includes but is not limited to tampered binaries, malicious code injection, or broken functionality. You use such artifacts entirely at your own risk.

If you encounter DeepMatch artifacts distributed outside of these official channels, please report it by [opening an issue](https://github.com/aouledissa/deep-match/issues).

## Artifact Signing

All official DeepMatch artifacts are signed with a PGP key. Verify the signature before using any artifact in a production or security-sensitive context.

### Current Signing Key

| Field       | Value                                                    |
|-------------|----------------------------------------------------------|
| Fingerprint | `80D0 DA79 427D A034 593F  2F35 0F14 8D47 0842 C013`    |
| Valid from  | 1.1.0                                                    |

### Verifying the Key Fingerprint

Cross-check the fingerprint against at least two independent sources before trusting it:

| Source | Details |
|--------|---------|
| **GitHub release asset** | Public key attached to the [1.1.0 GitHub release](https://github.com/aouledissa/deep-match/releases/tag/1.1.0) |
| **DNS TXT record** | Query `aouledissa.com` for a TXT record containing the fingerprint |
| **Keyservers** | `keys.openpgp.org`, `keyserver.ubuntu.com`, `pgp.mit.edu` |

To verify via DNS:

```bash
dig TXT aouledissa.com +short
```

To fetch and import from a keyserver:

```bash
gpg --keyserver keys.openpgp.org --recv-keys 80D0DA79427DA034593F2F350F148D470842C013
```

### Revoked Key

The previous signing key (`5283 67B0 1C0B 54E0 55A6  96E0 4D0B DAAD C6F8 86DB`) was compromised
and revoked with reason **1 — Key has been compromised**. It must no longer be trusted. Do not
verify artifacts against it. See the [1.1.0 release notes](CHANGELOG.md#110---2026-03-29) for
details.

### Verifying an Artifact

Maven Central publishes `.asc` signature files alongside each artifact. To verify:

```bash
# Download the artifact and its signature, then:
gpg --verify deepmatch-processor-<version>.jar.asc deepmatch-processor-<version>.jar
```

A valid signature from the current key fingerprint confirms the artifact is authentic and
unmodified.

## YAML Schema

The official JSON Schema for `.deeplinks.yml` files is served exclusively from:

```
https://raw.githubusercontent.com/aouledissa/deep-match/main/schemas/deeplinks.schema.json
```

Do not use schemas hosted at any other URL. A schema from an unofficial source could silently misconfigure your IDE validation (for example, marking required fields as optional or accepting invalid configurations). If you suspect the schema URL has been replicated or tampered with elsewhere, please report it by [opening an issue](https://github.com/aouledissa/deep-match/issues).
