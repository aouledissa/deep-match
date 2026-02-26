package com.aouledissa.deepmatch.gradle.internal.config

import org.gradle.api.GradleException

internal class MissingSpecsFileException(
    private val path: String,
    private val variantName: String
) : GradleException() {
    override val message: String
        get() = """
                DeepMatch Configuration Error: Failed to find deeplink specification files.
                To resolve this, add one or more '*.deeplinks.yml' files in ONE of the following locations (checked in this order):
                  - For variant-specific configuration: $path/src/$variantName/
                  - For a global fallback: $path/

                No '*.deeplinks.yml' files were found for the '$variantName' build variant in its source directory or at the project root.
            """.trimIndent()
}
