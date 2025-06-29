package com.aouledissa.deepmatch.gradle.internal.config

import com.android.build.api.variant.AndroidComponentsExtension
import com.aouledissa.deepmatch.gradle.DeepMatchPluginConfig
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkSpecsTask
import org.gradle.api.Project

internal fun configureAndroidVariants(project: Project, config: DeepMatchPluginConfig) {
    val android = project.extensions.getByType(AndroidComponentsExtension::class.java)
    val specsFilePath = when {
        config.specsFile.isPresent -> config.specsFile.get()
        else -> project.rootDir.resolve(".deeplinks.yml").path
    }
    val specsFile = project.layout.projectDirectory.file(specsFilePath)

    project.logger.error("> DeepMatch: specs file at: $specsFile")
    when {
        specsFile.asFile.exists().not() -> {
            project.logger.error("> DeepMatch: Could not parse specs file! Make sure you profile the path using the deepMatch gradle closure in you build file or provide the default config file \".deeplinks.yml\" directly in the root directory.")
        }

        else -> {
            android.onVariants { variant ->
                val variantName = variant.name.capitalize()

                /**
                 * Prepare and registers variant specific deeplinks specs codegen task.
                 */
                val generateVariantDeeplinkSpecsTask = project.tasks.register(
                    "generate${variantName}DeeplinkSpecs",
                    GenerateDeeplinkSpecsTask::class.java
                ) {
                    it.specsFileProperty.set(specsFile)
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
        }
    }
}