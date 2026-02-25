package com.aouledissa.deepmatch.gradle.internal.config

import org.gradle.api.GradleException

internal class MissingSpecsFileException(
    private val path: String,
    private val variantName: String
) : GradleException() {
    override val message: String
        get() = """
                DeepMatch Configuration Error: Failed to find the '.deeplinks.yml' deeplink specification file.
                To resolve this, create a '.deeplinks.yml' file in ONE of the following locations (checked in this order):
                  - For variant-specific configuration: $path/src/$variantName/.deeplinks.yml
                  - For a global fallback: $path/.deeplinks.yml

                No '.deeplinks.yml' was found for the '$variantName' build variant in its source directory or at the project root.
            """.trimIndent()
}