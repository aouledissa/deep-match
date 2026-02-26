package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.capitalize
import com.aouledissa.deepmatch.gradle.internal.deserializeDeeplinkConfigs
import com.aouledissa.deepmatch.gradle.internal.deserializeMergedDeeplinkConfigs
import com.aouledissa.deepmatch.gradle.internal.generatedModuleProcessorName
import com.aouledissa.deepmatch.gradle.internal.generatedModuleSealedInterfaceName
import com.aouledissa.deepmatch.gradle.internal.model.CompositeSpecShape
import com.aouledissa.deepmatch.gradle.internal.model.CompositeSpecsMetadata
import com.aouledissa.deepmatch.gradle.internal.model.DeeplinkConfig
import com.aouledissa.deepmatch.gradle.internal.model.toCollisionSignature
import com.aouledissa.deepmatch.gradle.internal.toCamelCase
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateDeeplinkSpecsTask : DefaultTask() {

    @get:InputFile
    abstract val specsFileProperty: RegularFileProperty

    @get:InputFiles
    abstract val additionalSpecsFilesProperty: ConfigurableFileCollection

    @get:Input
    abstract val packageNameProperty: Property<String>

    @get:Input
    abstract val moduleNameProperty: Property<String>

    @get:Input
    abstract val projectPathProperty: Property<String>

    @get:Input
    abstract val variantNameProperty: Property<String>

    @get:Input
    abstract val compositeProcessorsProperty: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputFile
    abstract val metadataOutputFile: RegularFileProperty

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }
    private val jsonSerializer by lazy { Json { prettyPrint = false } }

    init {
        projectPathProperty.convention(project.path)
        variantNameProperty.convention("main")
        additionalSpecsFilesProperty.setFrom(emptyList<String>())
        metadataOutputFile.convention(
            project.layout.buildDirectory.file("generated/deepmatch/specs/${name}/spec-shapes.json")
        )
    }

    @TaskAction
    fun generateDeeplinkSpecs() {
        val specsFiles = buildList {
            add(specsFileProperty.get().asFile)
            addAll(additionalSpecsFilesProperty.files.sortedBy { it.absolutePath })
        }
        val outputFile = outputDir.get().asFile

        outputFile.deleteRecursively()

        logger.quiet("$LOG_TAG processing specs files: ${specsFiles.joinToString { it.path }}")

        val packageName = "${packageNameProperty.get()}.deeplinks"
        val moduleSealedInterfaceName = generatedModuleSealedInterfaceName(moduleNameProperty.get())
        val moduleProcessorName = generatedModuleProcessorName(moduleNameProperty.get())
        val compositeProcessors = compositeProcessorsProperty.getOrElse(emptyList())
        specsFiles.forEach { file ->
            assertUniqueNames(yamlSerializer.deserializeDeeplinkConfigs(file))
        }
        val deeplinkConfigs = yamlSerializer.deserializeMergedDeeplinkConfigs(specsFiles)
        assertUniqueNames(deeplinkConfigs)
        val generatedSpecPropertyNames = mutableListOf<String>()

        FileSpec.builder(packageName, moduleSealedInterfaceName)
            .addType(generateModuleDeeplinkParamsType(moduleSealedInterfaceName))
            .build()
            .writeTo(outputFile)

        deeplinkConfigs.forEach { config ->
            assertRequiredFields(config)
            assertValidQueryParams(config)
            val deeplinkName = config.name.toCamelCase().plus("Deeplink")
            val fileName = deeplinkName.plus("Specs").capitalize()
            val specPropertyName = deeplinkName.plus("Specs").capitalize()
            val deeplinkParamsTypeName = deeplinkName.plus("Params").capitalize()
            val deeplinkParamsType = generateDeeplinkParamType(
                name = deeplinkParamsTypeName,
                config = config,
                moduleSealedInterfaceName = moduleSealedInterfaceName,
                packageName = packageName
            )
            val deeplinkProperty = generateDeeplinkSpecProperty(
                name = specPropertyName,
                config = config,
                packageName = packageName,
                paramsType = deeplinkParamsTypeName
            ).build()
            generatedSpecPropertyNames.add(specPropertyName)

            FileSpec.builder(packageName, fileName)
                .addImport(Param::class.qualifiedName.orEmpty(), "")
                .apply {
                    if (config.hasTypedParams()) {
                        addImport(ParamType::class.qualifiedName.orEmpty(), "")
                    }
                    addType(deeplinkParamsType)
                }
                .addProperty(deeplinkProperty)
                .build().writeTo(outputFile)

            logger.quiet("$LOG_TAG generated Deeplink spec at : ${packageName}.${fileName}")
        }

        FileSpec.builder(packageName, moduleProcessorName)
            .addType(
                generateModuleProcessorType(
                    name = moduleProcessorName,
                    generatedSpecPropertyNames = generatedSpecPropertyNames,
                    compositeProcessorNames = compositeProcessors
                )
            )
            .build()
            .writeTo(outputFile)

        writeSpecsMetadata(deeplinkConfigs)
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

    private fun generateModuleProcessorType(
        name: String,
        generatedSpecPropertyNames: List<String>,
        compositeProcessorNames: List<String>
    ): TypeSpec {
        val processorClass = ClassName(
            "com.aouledissa.deepmatch.processor",
            "DeeplinkProcessor"
        )
        val localSpecsSetInitializer = if (generatedSpecPropertyNames.isEmpty()) {
            CodeBlock.of("emptySet()")
        } else {
            CodeBlock.of("setOf(%L)", generatedSpecPropertyNames.joinToString(", "))
        }

        if (compositeProcessorNames.isEmpty()) {
            return TypeSpec.objectBuilder(name)
                .addModifiers(KModifier.PUBLIC)
                .superclass(processorClass)
                .addSuperclassConstructorParameter("specs = %L", localSpecsSetInitializer)
                .addKdoc("Generated deeplink processor for this module.")
                .build()
        }

        val compositeProcessorClass = ClassName(
            "com.aouledissa.deepmatch.processor",
            "CompositeDeeplinkProcessor"
        )
        val constructorArgs = CodeBlock.builder()
            .add("%T(specs = %L)", processorClass, localSpecsSetInitializer)
            .apply {
                compositeProcessorNames.forEach { fqcn ->
                    add(", %T", ClassName.bestGuess(fqcn))
                }
            }
            .build()

        return TypeSpec.objectBuilder(name)
            .addModifiers(KModifier.PUBLIC)
            .superclass(compositeProcessorClass)
            .addSuperclassConstructorParameter("%L", constructorArgs)
            .addKdoc("Generated deeplink processor composed from local and discovered module processors.")
            .build()
    }

    private fun writeSpecsMetadata(configs: List<DeeplinkConfig>) {
        val metadataFile = metadataOutputFile.get().asFile
        metadataFile.parentFile.mkdirs()

        val metadata = CompositeSpecsMetadata(
            modulePath = projectPathProperty.get(),
            variant = variantNameProperty.get(),
            specs = configs.map { config ->
                CompositeSpecShape(
                    name = config.name,
                    signature = config.toCollisionSignature(),
                    example = generateDeeplinkExample(config)
                )
            }
        )

        metadataFile.writeText(jsonSerializer.encodeToString(CompositeSpecsMetadata.serializer(), metadata))
    }

    private fun generateDeeplinkSpecProperty(
        name: String,
        config: DeeplinkConfig,
        packageName: String,
        paramsType: String,
    ): PropertySpec.Builder {
        val deeplinkSpecClass = ClassName(
            DeeplinkSpec::class.java.packageName,
            DeeplinkSpec::class.java.simpleName
        )
        val schemes = config.scheme.joinToString(separator = ", ") { "\"$it\"" }
        val hosts = config.host.joinToString(separator = ", ") { "\"$it\"" }
        val port = config.port?.toString() ?: "null"
        val pathParams = config.pathParams?.joinToString(separator = ", ").orEmpty()
        val queryParams = config.queryParams?.joinToString(separator = ", ").orEmpty()
        val paramsTypeClass = ClassName(packageName, paramsType)
        val deeplinkExample = generateDeeplinkExample(config)
        val escapedName = config.name.replace("\"", "\\\"")

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
                name = "$escapedName",
                scheme = setOf($schemes),
                host = setOf($hosts),
                port = $port,
                pathParams = listOf($pathParams),
                queryParams = setOf($queryParams),
                fragment = ${config.fragment?.let { "\"$it\"" } ?: "null"},
                paramsFactory = ${paramsTypeClass.simpleName}.Companion::fromMap
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
        config.port?.let { append(":$it") }
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
        val paramsClassName = ClassName(packageName, name)
        val moduleDeeplinkParamsInterface = ClassName(packageName, moduleSealedInterfaceName)
        val constructorParams = mutableListOf<ParameterSpec>()
        val constructorArgFactories = mutableListOf<CodeBlock>()

        config.pathParams?.filter { it.type != null }?.forEach { param ->
            val constructorName = param.name.toCamelCase()
            constructorParams += ParameterSpec.builder(
                constructorName,
                param.type!!.getType()
            ).build()
            constructorArgFactories += CodeBlock.of(
                "%N = %L",
                constructorName,
                fromMapValueFactory(
                    sourceKey = param.name.lowercase(),
                    type = param.type!!,
                    required = true
                )
            )
        }
        config.queryParams?.filter { it.type != null }?.forEach { param ->
            val constructorName = param.name.toCamelCase()
            constructorParams += ParameterSpec.builder(
                constructorName,
                param.type!!.getType().asTypeName().copy(nullable = !param.required)
            ).build()
            constructorArgFactories += CodeBlock.of(
                "%N = %L",
                constructorName,
                fromMapValueFactory(
                    sourceKey = param.name.lowercase(),
                    type = param.type!!,
                    required = param.required
                )
            )
        }
        config.fragment?.let {
            constructorParams += ParameterSpec.builder("fragment", String::class).build()
            constructorArgFactories += CodeBlock.of("fragment = params[%S]!!", "fragment")
        }

        val fromMapFun = FunSpec.builder("fromMap")
            .addModifiers(KModifier.INTERNAL)
            .addParameter(
                "params",
                MAP.parameterizedBy(STRING, STRING.copy(nullable = true))
            )
            .returns(paramsClassName.copy(nullable = true))
            .addCode(
                CodeBlock.builder()
                    .beginControlFlow("return try")
                    .apply {
                        if (constructorArgFactories.isEmpty()) {
                            addStatement("%T()", paramsClassName)
                        } else {
                            add("  %T(\n", paramsClassName)
                            constructorArgFactories.forEachIndexed { index, argument ->
                                add("    %L", argument)
                                if (index != constructorArgFactories.lastIndex) {
                                    add(",")
                                }
                                add("\n")
                            }
                            add("  )\n")
                        }
                    }
                    .nextControlFlow("catch (e: Exception)")
                    .addStatement("null")
                    .endControlFlow()
                    .build()
            )
            .build()

        return TypeSpec.classBuilder(name)
            .addModifiers(KModifier.PUBLIC)
            .addSuperinterface(moduleDeeplinkParamsInterface)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(constructorParams)
                    .build()
            )
            .addKdoc("${config.name.toCamelCase().capitalize()} Deeplink Parameters.")
            .apply {
                if (constructorParams.isNotEmpty()) {
                    addModifiers(KModifier.DATA)
                }
                constructorParams.forEach { param ->
                    addProperty(
                        PropertySpec.builder(param.name, param.type)
                            .initializer("%N", param)
                            .build()
                    )
                }
                addType(
                    TypeSpec.companionObjectBuilder()
                        .addModifiers(KModifier.INTERNAL)
                        .addFunction(fromMapFun)
                        .build()
                )
            }
            .build()
    }

    private fun fromMapValueFactory(
        sourceKey: String,
        type: ParamType,
        required: Boolean
    ): CodeBlock {
        return when (type) {
            ParamType.NUMERIC -> if (required) {
                CodeBlock.of("params[%S]!!.toInt()", sourceKey)
            } else {
                CodeBlock.of("params[%S]?.toInt()", sourceKey)
            }

            ParamType.ALPHANUMERIC,
            ParamType.STRING -> if (required) {
                CodeBlock.of("params[%S]!!", sourceKey)
            } else {
                CodeBlock.of("params[%S]", sourceKey)
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

    private fun assertRequiredFields(config: DeeplinkConfig) {
        if (config.scheme.isEmpty()) {
            throw GradleException(
                "DeepMatch: Spec '${config.name}' must define at least one scheme."
            )
        }
    }

    private fun assertUniqueNames(configs: List<DeeplinkConfig>) {
        val duplicates = configs.groupBy { it.name }
            .filter { it.value.size > 1 }
            .keys
        if (duplicates.isNotEmpty()) {
            throw GradleException(
                "DeepMatch: Duplicate deeplink spec names found: ${duplicates.joinToString()}. " +
                        "Each spec must have a unique name."
            )
        }
    }
}
