package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.model.DeeplinkConfig
import com.aouledissa.deepmatch.gradle.internal.model.Specs
import com.aouledissa.deepmatch.gradle.internal.toCamelCase
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

internal abstract class GenerateDeeplinkSpecsTask : DefaultTask() {

    @get:InputFile
    abstract val specsFileProperty: RegularFileProperty

    @get:Input
    abstract val packageNameProperty: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }

    @TaskAction
    fun generateDeeplinkSpecs() {
        val specsFile = specsFileProperty.get().asFile
        val outputFile = outputDir.get().asFile

        outputFile.deleteRecursively()

        logger.quiet("> DeepMatch: processing specs file in ${specsFile.path}")

        val packageName = packageNameProperty.get()
        val deeplinkConfigs = deserializeSpecs(specsFile)

        deeplinkConfigs.forEach { config ->
            val fileName = config.name.toCamelCase().plus("DeeplinkSpecs").capitalize()
            val deeplinkProperty = generateDeeplinkProperty(
                name = config.name.toCamelCase().capitalize(),
                config = config
            ).build()

            FileSpec.builder("$packageName.deeplinks", fileName)
                .addProperty(deeplinkProperty)
                .build().writeTo(outputFile)
        }
    }

    private fun deserializeSpecs(file: File): List<DeeplinkConfig> {
        val content = file.readText()
        return yamlSerializer.decodeFromString(Specs.serializer(), content).deeplinkSpecs
    }

    private fun generateDeeplinkProperty(
        name: String,
        config: DeeplinkConfig,
    ): PropertySpec.Builder {
        val deeplinkSpecClass = ClassName(
            DeeplinkSpec::class.java.packageName,
            DeeplinkSpec::class.java.simpleName
        )
        return PropertySpec.builder(name, deeplinkSpecClass)
            .addModifiers(KModifier.PUBLIC)
            .initializer(
                """
                %T(
                scheme = "${config.scheme}",
                host = "${config.host}"
                )
                """.trimIndent(),
                deeplinkSpecClass
            )
    }
}