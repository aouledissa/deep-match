package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.deserializeMergedDeeplinkConfigs
import com.aouledissa.deepmatch.gradle.internal.model.Action
import com.aouledissa.deepmatch.gradle.internal.model.AdvancedPatternPath
import com.aouledissa.deepmatch.gradle.internal.model.AndroidActivity
import com.aouledissa.deepmatch.gradle.internal.model.AndroidApplication
import com.aouledissa.deepmatch.gradle.internal.model.AndroidManifest
import com.aouledissa.deepmatch.gradle.internal.model.Category
import com.aouledissa.deepmatch.gradle.internal.model.ExactPath
import com.aouledissa.deepmatch.gradle.internal.model.Host
import com.aouledissa.deepmatch.gradle.internal.model.IntentFilter
import com.aouledissa.deepmatch.gradle.internal.model.IntentFilterCategory
import com.aouledissa.deepmatch.gradle.internal.model.PatternPath
import com.aouledissa.deepmatch.gradle.internal.model.Port
import com.aouledissa.deepmatch.gradle.internal.model.PrefixPath
import com.aouledissa.deepmatch.gradle.internal.model.Scheme
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import nl.adaptivity.xmlutil.serialization.XML
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateDeeplinkManifestFile : DefaultTask() {

    @get:InputFile
    abstract val specFileProperty: RegularFileProperty

    @get:InputFiles
    abstract val additionalSpecsFilesProperty: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val compileSdkProperty: Property<Int>

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }
    private val xmlSerializer by lazy { XML { indentString = " " } }
    private val useAdvancedPattern: Boolean
        get() = compileSdkProperty.get() >= 31

    @TaskAction
    fun generateDeeplinkManifest() {
        val specsFiles = buildList {
            add(specFileProperty.asFile.get())
            addAll(additionalSpecsFilesProperty.files.sortedBy { it.absolutePath })
        }

        logger.quiet(
            "$LOG_TAG generating Android manifest file for deeplink specs in ${
                specsFiles.joinToString { it.path }
            }"
        )

        val deeplinkConfigs = yamlSerializer.deserializeMergedDeeplinkConfigs(specsFiles)
        val activities = deeplinkConfigs.groupBy { it.activity }.map { activity ->
            AndroidActivity(
                name = activity.key,
                intentFilter = activity.value.map { config ->
                    val pathData = buildPathData(config.pathParams.orEmpty())
                    IntentFilter(
                        autoVerify = config.autoVerify.takeIf { it == true },
                        action = listOf(Action("android.intent.action.VIEW")),
                        category = getFilterCategories(
                            categories = config.categories,
                            autoVerify = config.autoVerify
                        ).toList(),
                        scheme = config.scheme.map { Scheme(name = it) },
                        hosts = config.host
                            .filter { it.isNotEmpty() }
                            .map { Host(name = it) },
                        port = config.port?.let { Port(number = it.toString()) },
                        exactPaths = pathData.exactPaths,
                        prefixPaths = pathData.prefixPaths,
                        patternPaths = pathData.patternPaths,
                        advancedPatternPaths = pathData.advancedPatternPaths
                    )
                }
            )
        }

        val manifest = AndroidManifest(application = AndroidApplication(activity = activities))
        val manifestXmlCode = xmlSerializer.encodeToString(AndroidManifest.serializer(), manifest)
        val manifestFile = outputFile.asFile.get()
        manifestFile.writeText(manifestXmlCode)
    }

    private fun getFilterCategories(
        categories: List<IntentFilterCategory>,
        autoVerify: Boolean
    ): Set<Category> {
        return categories.toMutableSet()
            .apply {
                if (autoVerify) {
                    add(IntentFilterCategory.DEFAULT)
                    add(IntentFilterCategory.BROWSABLE)
                }
            }
            .map { Category(it.manifestName) }
            .toSet()
    }

    private fun buildPathData(pathParams: List<Param>): PathDataGroups {
        if (pathParams.isEmpty()) return PathDataGroups()

        val allStatic = pathParams.all { it.type == null }
        if (allStatic) {
            val path = pathParams.joinToString(prefix = "/", separator = "/") { it.name }
            return PathDataGroups(
                exactPaths = listOf(
                    ExactPath(path = path),
                    ExactPath(path = "$path/")
                )
            )
        }

        val allTyped = pathParams.all { it.type != null }
        if (allTyped) {
            if (useAdvancedPattern) {
                val advancedPattern = pathParams.joinToString(prefix = "/", separator = "/") {
                    "[^/]+"
                }
                return PathDataGroups(
                    advancedPatternPaths = listOf(AdvancedPatternPath(pattern = advancedPattern))
                )
            }
            return PathDataGroups()
        }

        val lastTypedIndex = pathParams.indexOfLast { it.type != null }
        val hasStaticAfterLastTyped = pathParams
            .subList(lastTypedIndex + 1, pathParams.size)
            .any { it.type == null }

        return if (!hasStaticAfterLastTyped) {
            val staticPrefix = pathParams.takeWhile { it.type == null }
            PathDataGroups(
                prefixPaths = listOf(
                    PrefixPath(
                        prefix = staticPrefix.joinToString(
                            prefix = "/",
                            separator = "/",
                            postfix = "/"
                        ) { it.name }
                    )
                )
            )
        } else {
            val coarsePattern = pathParams.joinToString(prefix = "/", separator = "/") { param ->
                if (param.type == null) param.name else ".*"
            }
            val advancedPattern = if (useAdvancedPattern) {
                listOf(
                    AdvancedPatternPath(
                        pattern = pathParams.joinToString(prefix = "/", separator = "/") { param ->
                            if (param.type == null) param.name else "[^/]+"
                        }
                    )
                )
            } else {
                emptyList()
            }
            PathDataGroups(
                patternPaths = listOf(PatternPath(pattern = coarsePattern)),
                advancedPatternPaths = advancedPattern
            )
        }
    }

    private data class PathDataGroups(
        val exactPaths: List<ExactPath> = emptyList(),
        val prefixPaths: List<PrefixPath> = emptyList(),
        val patternPaths: List<PatternPath> = emptyList(),
        val advancedPatternPaths: List<AdvancedPatternPath> = emptyList()
    )
}
