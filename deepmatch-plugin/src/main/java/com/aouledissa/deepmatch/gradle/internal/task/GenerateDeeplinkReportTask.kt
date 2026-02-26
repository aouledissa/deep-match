package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.deserializeDeeplinkConfigs
import com.aouledissa.deepmatch.gradle.internal.model.DeeplinkConfig
import com.aouledissa.deepmatch.gradle.internal.toCamelCase
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateDeeplinkReportTask : DefaultTask() {

    @get:InputFile
    abstract val specsFileProperty: RegularFileProperty

    @get:Input
    abstract val moduleNameProperty: Property<String>

    @get:InputFiles
    abstract val additionalSpecsFilesProperty: ConfigurableFileCollection

    @get:Input
    abstract val additionalModuleSourcesProperty: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }
    private val jsonSerializer by lazy { Json { prettyPrint = false } }

    @TaskAction
    fun generate() {
        val specsJson = buildSpecsJson()

        val template = javaClass.classLoader
            .getResourceAsStream(REPORT_TEMPLATE_RESOURCE)
            ?.bufferedReader()
            ?.readText()
            ?: throw GradleException(
                "DeepMatch: Missing report template resource '$REPORT_TEMPLATE_RESOURCE'."
            )

        val html = template.replace(REPORT_JSON_PLACEHOLDER, specsJson)
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(html)

        logger.quiet("$LOG_TAG generated deeplink report at ${destination.path}")
    }

    private fun buildSpecsJson(): String {
        val moduleSources = linkedMapOf(
            moduleNameProperty.get() to mutableListOf(specsFileProperty.get().asFile.absolutePath)
        )
        additionalModuleSourcesProperty.getOrElse(emptyList()).forEach { entry ->
            val moduleAndPath = entry.split("::", limit = 2)
            if (moduleAndPath.size != 2) return@forEach
            val moduleName = moduleAndPath[0]
            val sourcePath = moduleAndPath[1]
            moduleSources.getOrPut(moduleName) { mutableListOf() }.add(sourcePath)
        }

        val modules = moduleSources.entries.map { (moduleName, sourcePaths) ->
            DeeplinkReportModule(
                name = moduleName,
                sources = sourcePaths
                    .distinct()
                    .map { sourcePath ->
                        val sourceFile = project.file(sourcePath)
                        val sourceConfigs = if (sourceFile.exists()) {
                            yamlSerializer.deserializeDeeplinkConfigs(sourceFile)
                        } else {
                            emptyList()
                        }
                        DeeplinkReportSource(
                            module = moduleName,
                            source = sourcePath,
                            specs = sourceConfigs.map { config ->
                                config.toReportSpec(
                                    module = moduleName,
                                    source = sourcePath
                                )
                            }
                        )
                    }
            )
        }
        val fullCatalog = modules.flatMap { module ->
            val mergedModuleSpecs = linkedMapOf<String, DeeplinkReportSpec>()
            module.sources.forEach { source ->
                source.specs.forEach { spec ->
                    // Later sources override earlier ones for same module/name.
                    mergedModuleSpecs[spec.name] = spec
                }
            }
            mergedModuleSpecs.values
        }

        return jsonSerializer.encodeToString(
            DeeplinkReportPayload(
                module = moduleNameProperty.get(),
                modules = modules,
                specs = fullCatalog
            )
        )
    }

    private fun DeeplinkConfig.toReportSpec(
        module: String,
        source: String
    ): DeeplinkReportSpec {
        return DeeplinkReportSpec(
            name = name,
            module = module,
            source = source,
            schemes = scheme,
            hosts = host,
            port = port,
            pathTemplate = buildPathTemplate(),
            regex = toRegexString(),
            pathParams = pathParams.orEmpty().map { param ->
                DeeplinkReportParam(
                    name = param.name,
                    type = param.type?.name?.lowercase() ?: "static",
                    required = true
                )
            },
            queryParams = queryParams.orEmpty().map { param ->
                DeeplinkReportParam(
                    name = param.name,
                    type = param.type?.name?.lowercase() ?: "string",
                    required = param.required
                )
            },
            fragment = fragment,
            autoVerify = autoVerify,
            generatedSpec = toSpecClassName(),
            generatedParams = toParamsClassName(),
            exampleUri = generateExampleUri()
        )
    }

    private fun DeeplinkConfig.toRegexString(): String {
        return DeeplinkSpec(
            name = name,
            scheme = scheme.toSet(),
            host = host.toSet(),
            port = port,
            pathParams = pathParams.orEmpty(),
            queryParams = queryParams.orEmpty().toSet(),
            fragment = fragment,
            paramsFactory = null
        ).matcher.pattern
    }

    private fun DeeplinkConfig.buildPathTemplate(): String {
        val segments = pathParams.orEmpty().joinToString("/") { param ->
            when (param.type) {
                null -> param.name
                else -> "{${param.name}}"
            }
        }
        return when {
            segments.isEmpty() -> "/"
            else -> "/$segments"
        }
    }

    private fun DeeplinkConfig.toSpecClassName(): String {
        val deeplinkName = name.toCamelCase().plus("Deeplink")
        return deeplinkName.plus("Specs").capitalize()
    }

    private fun DeeplinkConfig.toParamsClassName(): String {
        val deeplinkName = name.toCamelCase().plus("Deeplink")
        return deeplinkName.plus("Params").capitalize()
    }

    private fun DeeplinkConfig.generateExampleUri(): String {
        val schemeValue = scheme.firstOrNull().orEmpty()
        val hostValue = host.firstOrNull().orEmpty()
        val portValue = port?.let { ":$it" }.orEmpty()
        val pathValue = pathParams.orEmpty().joinToString(separator = "") { param ->
            when (param.type) {
                ParamType.NUMERIC -> "/123"
                ParamType.ALPHANUMERIC -> "/abc123"
                ParamType.STRING -> "/example"
                null -> "/${param.name}"
            }
        }
        val queryValue = queryParams.orEmpty()
            .filter { it.required }
            .joinToString("&") { param ->
                "${param.name}=${sampleValueFor(param)}"
            }
            .let { if (it.isBlank()) "" else "?$it" }
        val fragmentValue = fragment?.let { "#$it" }.orEmpty()
        return "$schemeValue://$hostValue$portValue$pathValue$queryValue$fragmentValue"
    }

    private fun sampleValueFor(param: Param): String {
        return when (param.type) {
            ParamType.NUMERIC -> "123"
            ParamType.ALPHANUMERIC -> "abc123"
            ParamType.STRING -> "example"
            null -> "example"
        }
    }

    private companion object {
        private const val REPORT_TEMPLATE_RESOURCE = "deeplink-report-template.html"
        private const val REPORT_JSON_PLACEHOLDER = "/* __SPECS_JSON__ */"
    }
}

@Serializable
internal data class DeeplinkReportPayload(
    val module: String,
    val modules: List<DeeplinkReportModule>,
    val specs: List<DeeplinkReportSpec>
)

@Serializable
internal data class DeeplinkReportModule(
    val name: String,
    val sources: List<DeeplinkReportSource>
)

@Serializable
internal data class DeeplinkReportSource(
    val module: String,
    val source: String,
    val specs: List<DeeplinkReportSpec>
)

@Serializable
internal data class DeeplinkReportSpec(
    val name: String,
    val module: String,
    val source: String,
    val schemes: List<String>,
    val hosts: List<String>,
    val port: Int?,
    val pathTemplate: String,
    val regex: String,
    val pathParams: List<DeeplinkReportParam>,
    val queryParams: List<DeeplinkReportParam>,
    val fragment: String?,
    val autoVerify: Boolean,
    val generatedSpec: String,
    val generatedParams: String,
    val exampleUri: String
)

@Serializable
internal data class DeeplinkReportParam(
    val name: String,
    val type: String,
    val required: Boolean
)
