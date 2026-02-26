package com.aouledissa.deepmatch.gradle.internal.config

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.aouledissa.deepmatch.gradle.DeepMatchPluginConfig
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.generatedModuleProcessorName
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkManifestFile
import com.aouledissa.deepmatch.gradle.internal.task.GenerateDeeplinkSpecsTask
import com.aouledissa.deepmatch.gradle.internal.task.ValidateCompositeSpecsCollisionsTask
import com.aouledissa.deepmatch.gradle.internal.task.ValidateDeeplinksTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.TaskProvider

internal fun configureAndroidVariants(project: Project, config: DeepMatchPluginConfig) {
    val android = project.extensions.getByType(AndroidComponentsExtension::class.java)
    val compileSdk = (project.extensions.getByName("android") as CommonExtension<*, *, *, *, *, *>)
        .compileSdk
        ?: 0

    android.onVariants { variant ->
        val specsFile = getSpecsFile(project = project, variant = variant)
        val composedDependencyProjects = discoverDeepMatchDependencyProjects(
            project = project,
            variantName = variant.name
        )

        val generateVariantDeeplinkSpecsTask = registerDeeplinkSpecsSourcesTask(
            project = project,
            variant = variant,
            specsFile = specsFile,
            compositeProcessors = composedDependencyProjects
                .mapNotNull(::toGeneratedProcessorFqcnOrNull)
                .sorted()
        )

        registerValidateDeeplinksTask(
            project = project,
            specsFile = specsFile
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
            specsFile = specsFile,
            compileSdk = compileSdk,
            generateManifestFiles = config.generateManifestFiles.getOrElse(false)
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
        it.packageNameProperty.set(variantPackageName)
        it.moduleNameProperty.set(project.name)
        it.projectPathProperty.set(project.path)
        it.variantNameProperty.set(variant.name)
        it.metadataOutputFile.set(deeplinkSpecsMetadataFile(project, variant.name))
        it.compositeProcessorsProperty.set(compositeProcessors)
        it.group = "deepmatch"
        it.description = "Generates deeplink specs, params, and processor for the ${variant.name} variant."
    }

    /**
     * This api will automatically add the sources to the variant's main sourceSet,
     * and also make the variant build pipeline depend on this task.
     */
    variant.sources.java?.addGeneratedSourceDirectory(
        generateVariantDeeplinkSpecsTask,
        GenerateDeeplinkSpecsTask::outputDir
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
        it.description = "Validates deeplink URI-shape collisions across composed specs for the ${variant.name} variant."
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
    specsFile: RegularFile
) {
    if (project.tasks.findByName("validateDeeplinks") != null) return
    project.tasks.register(
        "validateDeeplinks",
        ValidateDeeplinksTask::class.java
    ) {
        it.specsFileProperty.set(specsFile)
        it.group = "deepmatch"
        it.description = "Validates a URI against deeplink specs declared in .deeplinks.yml."
    }
}

private fun registerDeeplinkManifestTask(
    generateManifestFiles: Boolean,
    project: Project,
    variant: Variant,
    specsFile: RegularFile,
    compileSdk: Int
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
            it.compileSdkProperty.set(compileSdk)
            it.group = "deepmatch"
            it.description = "Generates deeplink intent-filter manifest entries for the ${variant.name} variant."
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
