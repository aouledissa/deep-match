package com.aouledissa.deepmatch.gradle.internal.config

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.aouledissa.deepmatch.gradle.DeepMatchPluginConfig
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkManifestFile
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkSpecsTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile

internal fun configureAndroidVariants(project: Project, config: DeepMatchPluginConfig) {
    val android = project.extensions.getByType(AndroidComponentsExtension::class.java)

    android.onVariants { variant ->
        val specsFile = getSpecsFile(project = project, variant = variant)

        registerDeeplinkSpecsSourcesTask(
            project = project,
            variant = variant,
            specsFile = specsFile
        )

        registerDeeplinkManifestTask(
            project = project,
            variant = variant,
            specsFile = specsFile,
            generateManifestFiles = config.generateManifestFiles.getOrElse(false)
        )
    }
}

private fun registerDeeplinkSpecsSourcesTask(
    project: Project,
    variant: Variant,
    specsFile: RegularFile
) {
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
        it.moduleNameProperty.set(project.name)
    }

    /**
     * This api will automatically add the sources to the variant's main sourceSet,
     * and also make the variant build pipeline depend on this task.
     */
    variant.sources.java?.addGeneratedSourceDirectory(
        generateVariantDeeplinkSpecsTask,
        GenerateDeeplinkSpecsTask::outputDir
    )
}

private fun registerDeeplinkManifestTask(
    generateManifestFiles: Boolean,
    project: Project,
    variant: Variant,
    specsFile: RegularFile
) {
    val variantName = variant.name.capitalize()
    val taskName = "generate${variantName}DeeplinksManifest"
    if (generateManifestFiles) {
        val generateVariantManifestFile = project.tasks.register(
            taskName,
            GenerateDeeplinkManifestFile::class.java
        ) {
            it.specFileProperty.set(specsFile)
            it.outputFile.set(project.layout.buildDirectory.file("generated/manifests/${variantName}"))
        }

        /**
         * This api will automatically add the sources to the variant's main manifests
         * source set, and also make the variant manifest processing depend on this task.
         */
        variant.sources.manifests.addGeneratedManifestFile(
            generateVariantManifestFile,
            GenerateDeeplinkManifestFile::outputFile
        )
    } else {
        val fileToDelete = project.layout.buildDirectory.dir("generated/manifests/$taskName")
            .get().asFile
        if (fileToDelete.exists()) {
            project.logger.quiet("$LOG_TAG cleaning up ${fileToDelete.path}")
            fileToDelete.deleteRecursively()
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
        else -> throw MissingSpecsFileException(
            path = project.projectDir.absolutePath,
            variantName = variant.name
        )
    }
}
