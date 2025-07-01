package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.deserializeDeeplinkConfigs
import com.aouledissa.deepmatch.gradle.internal.model.DeeplinkConfig
import com.aouledissa.deepmatch.gradle.internal.toCamelCase
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.squareup.kotlinpoet.ClassName
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

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }

    @TaskAction
    fun generateDeeplinkSpecs() {
        val specsFile = specsFileProperty.get().asFile
        val outputFile = outputDir.get().asFile

        outputFile.deleteRecursively()

        logger.quiet("> DeepMatch: processing specs file in ${specsFile.path}")

        val packageName = "${packageNameProperty.get()}.deeplinks"
        val deeplinkConfigs = yamlSerializer.deserializeDeeplinkConfigs(specsFile)

        deeplinkConfigs.forEach { config ->
            assertValidQueryParams(config)

            val fileName = config.name.toCamelCase().plus("DeeplinkSpecs").capitalize()
            val deeplinkParamsType = when {
                config.containsTemplateParams() -> generateDeeplinkParamType(
                    name = config.name.toCamelCase().plus("Params").capitalize(),
                    config = config
                ).build()

                else -> null
            }
            val deeplinkProperty = generateDeeplinkSpecProperty(
                name = config.name.toCamelCase().capitalize(),
                config = config,
                packageName = packageName,
                parametersClass = deeplinkParamsType?.name
            ).build()

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

            logger.quiet("> DeepMatch: generated Deeplink spec at : ${packageName}.${fileName}")
        }
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
                scheme = "${config.scheme}",
                host = "${config.host}",
                pathParams = setOf(${pathParams}),
                queryParams = setOf(${queryParams}),
                fragment = ${config.fragment?.let { "\"$it\"" } ?: "null"},
                parametersClass = ${parametersClass?.simpleName?.plus("::class")}
                )
                """.trimIndent(),
                deeplinkSpecClass,
            )
    }

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
    ): TypeSpec.Builder {
        val deeplinkParamSuperInterface = ClassName(
            DeeplinkParams::class.java.packageName,
            DeeplinkParams::class.java.simpleName
        )

        val pathParams = config.pathParams?.filter { it.type != null }?.map {
            ParameterSpec.builder(
                it.name.toCamelCase(),
                it.type!!.getType()
            ).build()
        }

        val queryParams = config.queryParams?.filter { it.type != null }?.map {
            ParameterSpec.builder(
                it.name.toCamelCase(),
                it.type!!.getType()
            ).build()
        }

        val fragmentParam = config.fragment?.let {
            ParameterSpec.builder(
                "fragment",
                String::class
            ).build()
        }

        return TypeSpec.classBuilder(name)
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.DATA)
            .addSuperinterface(deeplinkParamSuperInterface)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .apply {
                        pathParams?.forEach { addParameter(it) }
                        queryParams?.forEach { addParameter(it) }
                        fragmentParam?.let { addParameter(it) }
                    }
                    .build()
            )
            .addKdoc("${config.name.toCamelCase().capitalize()} Deeplink Parameters.")
            .apply {
                pathParams?.forEach {
                    addProperty(
                        PropertySpec.builder(it.name, it.type)
                            .initializer(it.name)
                            .build()
                    )
                }
                queryParams?.forEach {
                    addProperty(
                        PropertySpec.builder(it.name, it.type)
                            .initializer(it.name)
                            .build()
                    )
                }
                fragmentParam?.let {
                    addProperty(
                        PropertySpec.builder(it.name, it.type)
                            .initializer(it.name)
                            .build()
                    )
                }
            }
    }

    private fun assertValidQueryParams(config: DeeplinkConfig) {
        config.queryParams?.let { queryParams ->
            if (queryParams.all { it.type != null }.not()) {
                throw GradleException("DeepMatch: All queryParams should define a type: [numeric, alphanumeric, string]")
            }
        }
    }
}