package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.DeeplinkParams
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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateDeeplinkSpecsTask : DefaultTask() {

    @get:InputFile
    abstract val specsFileProperty: RegularFileProperty

    @get:Input
    abstract val packageNameProperty: Property<String>

    @get:Input
    abstract val moduleNameProperty: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }

    @TaskAction
    fun generateDeeplinkSpecs() {
        val specsFile = specsFileProperty.get().asFile
        val outputFile = outputDir.get().asFile

        outputFile.deleteRecursively()

        logger.quiet("$LOG_TAG processing specs file in ${specsFile.path}")

        val packageName = "${packageNameProperty.get()}.deeplinks"
        val moduleSealedInterfaceName = generateModuleSealedInterfaceName(moduleNameProperty.get())
        val moduleProcessorName = generateModuleProcessorName(moduleSealedInterfaceName)
        val deeplinkConfigs = yamlSerializer.deserializeDeeplinkConfigs(specsFile)
        val generatedSpecPropertyNames = mutableListOf<String>()

        FileSpec.builder(packageName, moduleSealedInterfaceName)
            .addType(generateModuleDeeplinkParamsType(moduleSealedInterfaceName))
            .build()
            .writeTo(outputFile)

        deeplinkConfigs.forEach { config ->
            assertValidQueryParams(config)
            val deeplinkName = config.name.toCamelCase().plus("Deeplink")
            val fileName = deeplinkName.plus("Specs").capitalize()
            val specPropertyName = deeplinkName.plus("Specs").capitalize()
            val deeplinkParamsTypeName =
                deeplinkName.plus("Params").capitalize().takeIf { config.containsTemplateParams() }
            val deeplinkParamsType = deeplinkParamsTypeName?.let {
                generateDeeplinkParamType(
                    name = it,
                    config = config,
                    moduleSealedInterfaceName = moduleSealedInterfaceName,
                    packageName = packageName
                )
            }
            val deeplinkProperty = generateDeeplinkSpecProperty(
                name = specPropertyName,
                config = config,
                packageName = packageName,
                parametersClass = deeplinkParamsTypeName
            ).build()
            generatedSpecPropertyNames.add(specPropertyName)

            FileSpec.builder(packageName, fileName)
                .addImport(Param::class.qualifiedName.orEmpty(), "")
                .apply {
                    if (config.containsTemplateParams()) {
                        addImport(ParamType::class.qualifiedName.orEmpty(), "")
                    }
                    deeplinkParamsType?.let { addType(it) }
                }
                .addProperty(deeplinkProperty)
                .build().writeTo(outputFile)

            logger.quiet("$LOG_TAG generated Deeplink spec at : ${packageName}.${fileName}")
        }

        FileSpec.builder(packageName, moduleProcessorName)
            .addType(generateModuleProcessorType(moduleProcessorName, generatedSpecPropertyNames))
            .build()
            .writeTo(outputFile)
    }

    private fun generateModuleSealedInterfaceName(moduleName: String): String {
        val normalized = moduleName
            .replace(Regex("[^A-Za-z0-9_\\-\\s]"), " ")
            .toCamelCase()
            .ifBlank { "module" }
        val typeSafeName = when {
            normalized.first().isDigit() -> "module${normalized.capitalize()}"
            else -> normalized
        }
        return "${typeSafeName.capitalize()}DeeplinkParams"
    }

    private fun generateModuleDeeplinkParamsType(name: String): TypeSpec {
        val deeplinkParamSuperInterface = ClassName(
            DeeplinkParams::class.java.packageName,
            DeeplinkParams::class.java.simpleName
        )
        return TypeSpec.interfaceBuilder(name)
            .addModifiers(KModifier.PUBLIC, KModifier.SEALED)
            .addSuperinterface(deeplinkParamSuperInterface)
            .addKdoc("Base sealed params type for this module's generated deeplinks.")
            .build()
    }

    private fun generateModuleProcessorName(moduleSealedInterfaceName: String): String {
        val prefix = moduleSealedInterfaceName.removeSuffix("DeeplinkParams")
        return "${prefix}DeeplinkProcessor"
    }

    private fun generateModuleProcessorType(
        name: String,
        generatedSpecPropertyNames: List<String>
    ): TypeSpec {
        val processorClass = ClassName(
            "com.aouledissa.deepmatch.processor",
            "DeeplinkProcessor"
        )
        val specsSetInitializer = CodeBlock.of(
            "setOf(%L)",
            generatedSpecPropertyNames.joinToString(", ")
        )

        return TypeSpec.objectBuilder(name)
            .addModifiers(KModifier.PUBLIC)
            .superclass(processorClass)
            .addSuperclassConstructorParameter("specs = %L", specsSetInitializer)
            .addKdoc("Generated deeplink processor for this module.")
            .build()
    }

    private fun generateDeeplinkSpecProperty(
        name: String,
        config: DeeplinkConfig,
        packageName: String,
        parametersClass: String?,
    ): PropertySpec.Builder {
        val deeplinkSpecClass = ClassName(
            DeeplinkSpec::class.java.packageName,
            DeeplinkSpec::class.java.simpleName
        )
        val schemes = config.scheme.joinToString(separator = ", ") { "\"$it\"" }
        // TODO: is empty host case handled?
        val hosts = config.host.joinToString(separator = ", ") { "\"$it\"" }
        val pathParams = config.pathParams?.joinToString(separator = ", ").orEmpty()
        val queryParams = config.queryParams?.joinToString(separator = ", ").orEmpty()
        val parametersClass = parametersClass?.let { ClassName(packageName, it) }
        val deeplinkExample = generateDeeplinkExample(config)

        return PropertySpec.builder(name, deeplinkSpecClass)
            .addModifiers(KModifier.PUBLIC)
            .addKdoc(
                """
                $name Deeplink specs. 
                
                exp: $deeplinkExample
                """.trimIndent()
            )
            .initializer(
                """
                %T(
                scheme = setOf($schemes),
                host = setOf($hosts),
                pathParams = setOf($pathParams),
                queryParams = setOf($queryParams),
                fragment = ${config.fragment?.let { "\"$it\"" } ?: "null"},
                parametersClass = ${parametersClass?.simpleName?.plus("::class")}
                )
                """.trimIndent(),
                deeplinkSpecClass,
            )
    }

    // TODO: generate all possible examples
    private fun generateDeeplinkExample(config: DeeplinkConfig) = buildString {
        append(config.scheme)
        append("://")
        append(config.host)
        config.pathParams?.map {
            when (it.type) {
                ParamType.NUMERIC -> "/1234"
                ParamType.ALPHANUMERIC -> "/loremipsum1234"
                ParamType.STRING -> "/loremipsum"
                else -> "/${it.name}"
            }
        }?.forEach {
            append(it)
        }
        config.queryParams?.let {
            append("?")
            it.mapIndexed { index, param ->
                val value = when (param.type) {
                    ParamType.NUMERIC -> "1234"
                    ParamType.ALPHANUMERIC -> "loremipsum1234"
                    ParamType.STRING -> "loremipsum"
                    else -> ""
                }
                if (index > 0) {
                    append("&")
                }
                append("${param.name}=${value}")
            }
        }
        config.fragment?.let {
            append("#$it")
        }
    }

    private fun generateDeeplinkParamType(
        name: String,
        config: DeeplinkConfig,
        packageName: String,
        moduleSealedInterfaceName: String
    ): TypeSpec {
        val moduleDeeplinkParamsInterface = ClassName(packageName, moduleSealedInterfaceName)
        val constructorParams = buildList {
            config.pathParams?.filter { it.type != null }?.forEach {
                add(
                    ParameterSpec.builder(
                        it.name.toCamelCase(),
                        it.type!!.getType()
                    ).build()
                )
            }
            config.queryParams?.filter { it.type != null }?.forEach {
                add(
                    ParameterSpec.builder(
                        it.name.toCamelCase(),
                        it.type!!.getType()
                    ).build()
                )
            }
            config.fragment?.let {
                add(
                    ParameterSpec.builder(
                        "fragment",
                        String::class
                    ).build()
                )
            }
        }

        return TypeSpec.classBuilder(name)
            .addModifiers(KModifier.PUBLIC, KModifier.DATA)
            .addSuperinterface(moduleDeeplinkParamsInterface)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(constructorParams)
                    .build()
            )
            .addKdoc("${config.name.toCamelCase().capitalize()} Deeplink Parameters.")
            .apply {
                constructorParams.forEach { param ->
                    addProperty(
                        PropertySpec.builder(param.name, param.type)
                            .initializer("%N", param)
                            .build()
                    )
                }
            }
            .build()
    }

    private fun assertValidQueryParams(config: DeeplinkConfig) {
        config.queryParams?.let { queryParams ->
            if (queryParams.all { it.type != null }.not()) {
                throw GradleException("DeepMatch: All queryParams should define a type: [numeric, alphanumeric, string]")
            }
        }
    }
}
