package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.model.CompositeSpecsMetadata
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

internal abstract class ValidateCompositeSpecsCollisionsTask : DefaultTask() {

    @get:InputFiles
    abstract val metadataFiles: ConfigurableFileCollection

    @get:Input
    abstract val variantNameProperty: Property<String>

    private val jsonSerializer by lazy { Json { ignoreUnknownKeys = true } }

    @TaskAction
    fun validate() {
        val existingMetadataFiles = metadataFiles.files
            .filter { it.exists() }
            .sortedBy { it.path }

        if (existingMetadataFiles.isEmpty()) {
            logger.quiet("$LOG_TAG no deeplink metadata files found for variant '${variantNameProperty.get()}'")
            return
        }

        val bySignature = linkedMapOf<String, MutableList<SpecOrigin>>()

        existingMetadataFiles.forEach { file ->
            val metadata = jsonSerializer.decodeFromString(
                CompositeSpecsMetadata.serializer(),
                file.readText()
            )
            metadata.specs.forEach { spec ->
                bySignature.getOrPut(spec.signature) { mutableListOf() }
                    .add(SpecOrigin(modulePath = metadata.modulePath, specName = spec.name, example = spec.example))
            }
        }

        val collisions = bySignature
            .filter { (_, origins) -> origins.map { it.modulePath }.toSet().size > 1 }
            .toSortedMap()

        if (collisions.isNotEmpty()) {
            throw GradleException(buildCollisionMessage(collisions))
        }
    }

    private fun buildCollisionMessage(
        collisions: Map<String, List<SpecOrigin>>
    ): String {
        val details = collisions.entries.joinToString(separator = "\n\n") { (signature, origins) ->
            val originsText = origins.joinToString(separator = "\n") { origin ->
                "  - module=${origin.modulePath}, spec='${origin.specName}', example='${origin.example}'"
            }
            "Signature: $signature\n$originsText"
        }
        return "DeepMatch: Found deeplink URI-shape collisions across composed modules:\n\n$details"
    }

    private data class SpecOrigin(
        val modulePath: String,
        val specName: String,
        val example: String
    )
}
