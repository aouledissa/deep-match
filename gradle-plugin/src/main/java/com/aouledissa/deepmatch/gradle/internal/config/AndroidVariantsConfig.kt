package com.aouledissa.deepmatch.gradle.internal.config

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.aouledissa.deepmatch.gradle.DeepMatchPluginConfig
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkManifestFile
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkSpecsTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile

internal fun configureAndroidVariants(project: Project, config: DeepMatchPluginConfig) {
    val android = project.extensions.getByType(AndroidComponentsExtension::class.java)

    android.onVariants { variant ->
        val specsFile = getSpecsFile(project = project, variant = variant)
        val variantName = variant.name.capitalize()
        val variantPackageName = variant.namespace

        /**
         * Prepare and registers variant specific deeplinks specs codegen task.
         */
        val generateVariantDeeplinkSpecsTask = project.tasks.register(
            "generate${variantName}DeeplinkSpecs",
            GenerateDeeplinkSpecsTask::class.java
        ) {
            it.specsFileProperty.set(specsFile)
            it.packageNameProperty.set(variantPackageName)
        }

        /**
         * This api will automatically add the sources to the variant's main sourceSet,
         * and also make the variant build pipeline depend on this task.
         */
        variant.sources.java?.addGeneratedSourceDirectory(
            generateVariantDeeplinkSpecsTask,
            GenerateDeeplinkSpecsTask::outputDir
        )

        if (config.generateManifestFiles.isPresent) {
            val generateVariantManifestFile = project.tasks.register(
                "generate${variantName.capitalize()}DeeplinksManifest",
                GenerateDeeplinkManifestFile::class.java
            ) {
                it.specFileProperty.set(specsFile)
                it.outputFile.set(project.layout.buildDirectory.file("generated/manifests/${variant.name}"))
            }

            /**
             * This api will automatically add the sources to the variant's main manifests
             * source set, and also make the variant manifest processing depend on this task.
             */
            variant.sources.manifests.addGeneratedManifestFile(
                generateVariantManifestFile,
                GenerateDeeplinkManifestFile::outputFile
            )
        }
    }
}

private fun getSpecsFile(project: Project, variant: Variant): RegularFile {
    val variantSpecsFile =
        project.layout.projectDirectory.file("src/${variant.name}/.deeplinks.yml")
    val defaultSpecsFile = project.layout.projectDirectory.file(".deeplinks.yml")
    return when {
        variantSpecsFile.asFile.exists() -> variantSpecsFile
        defaultSpecsFile.asFile.exists() -> defaultSpecsFile
        else -> {
            val errorMessage = """
                DeepMatch Configuration Error: Failed to find the '.deeplinks.yml' deeplink specification file.
                To resolve this, create a '.deeplinks.yml' file in ONE of the following locations (checked in this order):
                  - For variant-specific configuration: ${project.projectDir.absolutePath}/src/${variant.name}/.deeplinks.yml
                  - For a global fallback: ${project.projectDir.absolutePath}/.deeplinks.yml

                No '.deeplinks.yml' was found for the '${variant.name}' build variant in its source directory or at the project root.
            """.trimIndent()
            throw GradleException(errorMessage)
        }
    }
}