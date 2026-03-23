/*
 * Copyright 2026 DeepMatch Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aouledissa.deepmatch.gradle.internal.config

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Sources
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.utils.KOTLIN_ANDROID_PLUGIN_ID
import com.aouledissa.deepmatch.gradle.DeepMatchPluginConfig
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.ManifestSyncViolation
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.generatedModuleProcessorName
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkManifestFile
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkReportTask
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkSpecsTask
import com.aouledissa.deepmatch.gradle.internal.task.ValidateCompositeSpecsCollisionsTask
import com.aouledissa.deepmatch.gradle.internal.task.ValidateDeeplinksTask
import com.aouledissa.deepmatch.gradle.internal.task.WarnManifestOutOfSyncTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

internal fun configureAndroidVariants(project: Project, config: DeepMatchPluginConfig) {
    val android = project.extensions.getByType(AndroidComponentsExtension::class.java)
    val compileSdk = (project.extensions.getByName("android") as CommonExtension).compileSdk ?: 0

    android.onVariants { variant ->
        val specsFiles = getSpecsFiles(project = project, variant = variant)
        val primarySpecsFile = specsFiles.first()
        val additionalSpecsFiles = specsFiles.drop(1)
        val composedDependencyProjects = discoverDeepMatchDependencyProjects(
            project = project,
            variantName = variant.name
        )

        val generateVariantDeeplinkSpecsTask = registerDeeplinkSpecsSourcesTask(
            project = project,
            variant = variant,
            specsFile = primarySpecsFile,
            additionalSpecsFiles = additionalSpecsFiles,
            compositeProcessors = composedDependencyProjects
                .mapNotNull(::toGeneratedProcessorFqcnOrNull)
                .sorted()
        )

        registerValidateDeeplinksTask(
            project = project,
            specsFile = primarySpecsFile,
            additionalSpecsFiles = additionalSpecsFiles
        )
        registerDeeplinkReportTask(
            project = project,
            variantName = variant.name,
            moduleName = project.name,
            composedDependencyProjects = composedDependencyProjects,
            enabled = config.report.enabled.getOrElse(false),
            outputFileProvider = config.report.output.orElse(
                project.layout.buildDirectory.file("reports/deepmatch/deeplinks-catalogue.html")
            )
        )

        registerCompositeSpecsCollisionTask(
            project = project,
            variant = variant,
            generateVariantDeeplinkSpecsTask = generateVariantDeeplinkSpecsTask,
            composedDependencyProjects = composedDependencyProjects
        )

        registerDeeplinkManifestTask(
            project = project,
            variant = variant,
            specsFile = primarySpecsFile,
            additionalSpecsFiles = additionalSpecsFiles,
            compileSdk = compileSdk,
            generateManifestFiles = config.generateManifestFiles.getOrElse(false),
            manifestSyncViolation = config.manifestSyncViolation.getOrElse(ManifestSyncViolation.WARN)
        )
    }
}

internal fun resolveCompositeProcessorFqcns(
    project: Project,
    variantName: String
): List<String> {
    return discoverDeepMatchDependencyProjects(
        project = project,
        variantName = variantName
    ).mapNotNull(::toGeneratedProcessorFqcnOrNull)
        .sorted()
}

internal fun discoverDeepMatchDependencyProjects(
    project: Project,
    variantName: String
): List<Project> {
    val dependencyProjects = linkedSetOf<Project>()
    compositeCandidateConfigurationNames(variantName).forEach { configurationName ->
        val configuration = project.configurations.findByName(configurationName) ?: return@forEach
        configuration.dependencies
            .withType(ProjectDependency::class.java)
            .forEach { dependency ->
                project.rootProject.findProject(dependency.path)?.let { dependencyProjects += it }
            }
    }

    return dependencyProjects
        .filter { dependencyProject ->
            dependencyProject.extensions.findByName(DeepMatchPluginConfig.NAME) != null
        }
        .sortedBy { it.path }
}

private fun compositeCandidateConfigurationNames(variantName: String): Set<String> {
    val capitalizedVariantName = variantName.capitalize()
    return linkedSetOf(
        "implementation",
        "api",
        "compileOnly",
        "runtimeOnly",
        "${variantName}Implementation",
        "${variantName}Api",
        "${variantName}CompileOnly",
        "${variantName}RuntimeOnly",
        "${capitalizedVariantName}Implementation",
        "${capitalizedVariantName}Api",
        "${capitalizedVariantName}CompileOnly",
        "${capitalizedVariantName}RuntimeOnly"
    )
}

private fun toGeneratedProcessorFqcnOrNull(project: Project): String? {
    val namespace = project.resolveAndroidNamespaceOrNull() ?: return null
    val generatedProcessorName = generatedModuleProcessorName(project.name)
    return "$namespace.deeplinks.$generatedProcessorName"
}

private fun Project.resolveAndroidNamespaceOrNull(): String? {
    val androidExtension = extensions.findByName("android") ?: return null
    val namespaceGetter = androidExtension.javaClass.methods.firstOrNull {
        it.name == "getNamespace" && it.parameterCount == 0
    } ?: return null
    val namespace = runCatching { namespaceGetter.invoke(androidExtension) as? String }
        .getOrNull()
        .orEmpty()
    return namespace.takeIf { it.isNotBlank() }
}

private fun registerDeeplinkSpecsSourcesTask(
    project: Project,
    variant: Variant,
    specsFile: RegularFile,
    additionalSpecsFiles: List<RegularFile>,
    compositeProcessors: List<String>
): TaskProvider<GenerateDeeplinkSpecsTask> {
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
        it.additionalSpecsFilesProperty.setFrom(additionalSpecsFiles)
        it.packageNameProperty.set(variantPackageName)
        it.moduleNameProperty.set(project.name)
        it.projectPathProperty.set(project.path)
        it.variantNameProperty.set(variant.name)
        it.metadataOutputFile.set(deeplinkSpecsMetadataFile(project, variant.name))
        it.compositeProcessorsProperty.set(compositeProcessors)
        it.group = "deepmatch"
        it.description =
            "Generates deeplink specs, params, and processor for the ${variant.name} variant."
    }

    /**
     * This api will automatically add the sources to the variant's main sourceSet,
     * and also make the variant build pipeline depend on this task.
     */
    variant.sources.addGeneratedSourceDirectory(
        project = project,
        taskProvider = generateVariantDeeplinkSpecsTask,
        wiredWith = GenerateDeeplinkSpecsTask::outputDir
    )

    return generateVariantDeeplinkSpecsTask
}

private fun deeplinkSpecsMetadataFile(project: Project, variantName: String) =
    project.layout.buildDirectory.file("generated/deepmatch/specs/$variantName/spec-shapes.json")

private fun registerCompositeSpecsCollisionTask(
    project: Project,
    variant: Variant,
    generateVariantDeeplinkSpecsTask: TaskProvider<GenerateDeeplinkSpecsTask>,
    composedDependencyProjects: List<Project>
) {
    val variantName = variant.name
    val capitalizedVariantName = variantName.capitalize()
    val taskName = "validate${capitalizedVariantName}CompositeSpecsCollisions"

    val validateCompositeSpecsTask = project.tasks.register(
        taskName,
        ValidateCompositeSpecsCollisionsTask::class.java
    ) {
        it.variantNameProperty.set(variantName)
        it.metadataFiles.from(deeplinkSpecsMetadataFile(project, variantName))
        composedDependencyProjects.forEach { dependencyProject ->
            it.metadataFiles.from(deeplinkSpecsMetadataFile(dependencyProject, variantName))
        }
        it.group = "deepmatch"
        it.description =
            "Validates deeplink URI-shape collisions across composed specs for the ${variant.name} variant."
        it.dependsOn(generateVariantDeeplinkSpecsTask)
    }

    composedDependencyProjects.forEach { dependencyProject ->
        val dependencySpecsTaskName = "generate${capitalizedVariantName}DeeplinkSpecs"
        dependencyProject.tasks
            .matching { it.name == dependencySpecsTaskName }
            .configureEach { dependencyGenerateSpecsTask ->
                validateCompositeSpecsTask.configure {
                    it.dependsOn(dependencyGenerateSpecsTask)
                }
            }
    }

    project.tasks
        .matching { it.name == "pre${capitalizedVariantName}Build" }
        .configureEach { preBuildTask ->
            preBuildTask.dependsOn(validateCompositeSpecsTask)
        }

    project.tasks
        .matching { it.name == "check" }
        .configureEach { checkTask ->
            checkTask.dependsOn(validateCompositeSpecsTask)
        }
}

private fun registerValidateDeeplinksTask(
    project: Project,
    specsFile: RegularFile,
    additionalSpecsFiles: List<RegularFile>
) {
    if (project.tasks.findByName("validateDeeplinks") != null) return
    project.tasks.register(
        "validateDeeplinks",
        ValidateDeeplinksTask::class.java
    ) {
        it.specsFileProperty.set(specsFile)
        it.additionalSpecsFilesProperty.setFrom(additionalSpecsFiles)
        it.group = "deepmatch"
        it.description = "Validates a URI against deeplink specs declared in .deeplinks.yml."
    }
}

private fun registerDeeplinkReportTask(
    project: Project,
    variantName: String,
    moduleName: String,
    composedDependencyProjects: List<Project>,
    enabled: Boolean,
    outputFileProvider: Provider<RegularFile>
) {
    if (!enabled || project.tasks.findByName("generateDeeplinkReport") != null) return

    val localReportSpecFiles = collectSpecsFiles(project, variantName)
    if (localReportSpecFiles.isEmpty()) return

    val moduleSourceEntries = mutableListOf<String>()
    val additionalSpecFiles = mutableListOf<RegularFile>()
    localReportSpecFiles.drop(1).forEach { localSource ->
        additionalSpecFiles += localSource
        moduleSourceEntries += "${project.name}::${localSource.asFile.absolutePath}"
    }
    composedDependencyProjects.forEach { dependencyProject ->
        collectSpecsFiles(dependencyProject, variantName).forEach { reportSpecFile ->
            additionalSpecFiles += reportSpecFile
            moduleSourceEntries += "${dependencyProject.name}::${reportSpecFile.asFile.absolutePath}"
        }
    }

    val reportTask = project.tasks.register(
        "generateDeeplinkReport",
        GenerateDeeplinkReportTask::class.java
    ) {
        it.specsFileProperty.set(localReportSpecFiles.first())
        it.moduleNameProperty.set(moduleName)
        it.additionalSpecsFilesProperty.setFrom(additionalSpecFiles)
        it.additionalModuleSourcesProperty.set(moduleSourceEntries)
        it.outputFile.set(outputFileProvider)
        it.group = "deepmatch"
        it.description =
            "Generates a standalone deeplink catalog and live URI validator HTML report."
    }

    project.tasks.matching { it.name == "check" }.configureEach { checkTask ->
        checkTask.dependsOn(reportTask)
    }
}

private fun registerDeeplinkManifestTask(
    generateManifestFiles: Boolean,
    project: Project,
    variant: Variant,
    specsFile: RegularFile,
    additionalSpecsFiles: List<RegularFile>,
    compileSdk: Int,
    manifestSyncViolation: ManifestSyncViolation
) {
    val variantName = variant.name.capitalize()
    val taskName = "generate${variantName}DeeplinksManifest"
    if (generateManifestFiles) {
        val generateVariantManifestFile = project.tasks.register(
            taskName,
            GenerateDeeplinkManifestFile::class.java
        ) {
            it.specFileProperty.set(specsFile)
            it.additionalSpecsFilesProperty.setFrom(additionalSpecsFiles)
            it.outputFile.set(project.layout.buildDirectory.file("generated/manifests/${taskName}/AndroidManifest.xml"))
            it.outputDir.set(project.layout.buildDirectory.dir("generated/manifests/$taskName"))
            it.compileSdkProperty.set(compileSdk)
            it.group = "deepmatch"
            it.description =
                "Generates deeplink intent-filter manifest entries for the ${variant.name} variant."
        }

        /**
         * This api will automatically add the sources to the variant's main manifests
         * source set, and also make the variant manifest processing depend on this task.
         */
        variant.sources.manifests.addGeneratedManifestFile(
            generateVariantManifestFile,
            GenerateDeeplinkManifestFile::outputFile
        )
        variant.sources.addGeneratedSourceDirectory(
            project = project,
            taskProvider = generateVariantManifestFile,
            wiredWith = GenerateDeeplinkManifestFile::outputDir
        )
    } else {
        val fileToDelete = project.layout.buildDirectory.dir("generated/manifests/$taskName")
            .get().asFile
        if (fileToDelete.exists()) {
            project.logger.quiet("$LOG_TAG cleaning up ${fileToDelete.path}")
            fileToDelete.deleteRecursively()
        }

        val warnTask = project.tasks.register(
            "warn${variantName}ManifestOutOfSync",
            WarnManifestOutOfSyncTask::class.java
        ) {
            it.specFileProperty.set(specsFile)
            it.additionalSpecsFilesProperty.setFrom(additionalSpecsFiles)
            it.mergedManifestProperty.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            it.violationProperty.set(manifestSyncViolation)
            it.group = "deepmatch"
            it.description =
                "Warns about deeplink intent filters missing from the merged AndroidManifest.xml for the ${variant.name} variant."
        }

        project.tasks.matching { it.name == "check" }.configureEach { checkTask ->
            checkTask.dependsOn(warnTask)
        }
    }
}

private fun getSpecsFiles(project: Project, variant: Variant): List<RegularFile> {
    return collectSpecsFiles(project, variant.name)
        .ifEmpty {
            throw MissingSpecsFileException(
                path = project.projectDir.absolutePath,
                variantName = variant.name
            )
        }
}

private fun collectSpecsFiles(project: Project, variantName: String): List<RegularFile> {
    val variantDir = project.layout.projectDirectory.dir("src/$variantName").asFile
    val rootDir = project.layout.projectDirectory.asFile

    val rootFiles = listSpecFiles(rootDir)
        .map { project.layout.projectDirectory.file(it.name) }
    val variantFiles = listSpecFiles(variantDir)
        .map { project.layout.projectDirectory.file("src/$variantName/${it.name}") }

    return (rootFiles + variantFiles)
        .distinctBy { it.asFile.absolutePath }
}

private fun listSpecFiles(directory: java.io.File): List<java.io.File> {
    if (!directory.exists() || !directory.isDirectory) return emptyList()
    return directory.listFiles()
        .orEmpty()
        .filter { file ->
            file.isFile && (
                    file.name == ".deeplinks.yml" ||
                            file.name == ".deeplinks.yaml" ||
                            file.name.endsWith(".deeplinks.yml") ||
                            file.name.endsWith(".deeplinks.yaml")
                    )
        }
        .sortedBy { it.name }
}

/**
 * Adds a generated source directory to the appropriate source set based on the Kotlin setup.
 *
 * AGP >= 9 ships with built-in Kotlin support and its [Sources.kotlin] is always non-null, so
 * generated sources are wired directly to the **kotlin** source set.
 *
 * AGP < 9 does **not** add the Kotlin source set to the compilation classpath automatically —
 * even when `org.jetbrains.kotlin.android` is applied, AGP only exposes a [Sources.kotlin]
 * container but does not promote it to the final classpath. Sources must therefore be wired via
 * [Sources.java], which KGP then picks up and promotes to the Kotlin compilation classpath.
 *
 * Note: on AGP < 9, [Sources.kotlin] may be non-null even without `org.jetbrains.kotlin.android`
 * (AGP still creates the container). The AGP major version is therefore used to distinguish
 * true built-in Kotlin support (AGP >= 9) from an empty container (AGP < 9 without KGP).
 *
 * If neither condition is met (AGP < 9 without `org.jetbrains.kotlin.android` applied), an
 * exception is thrown with an actionable message. To fix this, either apply
 * `org.jetbrains.kotlin.android` if on AGP < 9, or upgrade to AGP >= 9.
 *
 * @param TASK the type of the task that produces the generated source directory.
 * @param project the Gradle project used to check the AGP version and applied plugins.
 * @param taskProvider the provider for the task that generates the sources.
 * @param wiredWith a function that extracts the [DirectoryProperty] output from [TASK]; this
 *   directory is registered as the generated source root.
 */
private fun <TASK : Task> Sources.addGeneratedSourceDirectory(
    project: Project,
    taskProvider: TaskProvider<TASK>,
    wiredWith: (TASK) -> DirectoryProperty
) {
    val isAgp9Plus = project.extensions.getByType(AndroidComponentsExtension::class.java)
        .pluginVersion.major >= 9
    when {
        project.plugins.hasPlugin(KOTLIN_ANDROID_PLUGIN_ID) && java != null -> java?.addGeneratedSourceDirectory(
            taskProvider,
            wiredWith
        )

        isAgp9Plus && kotlin != null -> kotlin?.addGeneratedSourceDirectory(
            taskProvider,
            wiredWith
        )

        else -> throw GradleException(
            "DeepMatch requires Kotlin support to generate sources. " +
                    "Apply the 'org.jetbrains.kotlin.android' plugin if you are on AGP < 9, " +
                    "or upgrade to AGP >= 9."
        )
    }
}
