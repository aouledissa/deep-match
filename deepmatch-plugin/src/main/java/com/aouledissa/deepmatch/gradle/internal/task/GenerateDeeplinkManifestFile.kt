package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.deserializeDeeplinkConfigs
import com.aouledissa.deepmatch.gradle.internal.model.Action
import com.aouledissa.deepmatch.gradle.internal.model.AndroidActivity
import com.aouledissa.deepmatch.gradle.internal.model.AndroidApplication
import com.aouledissa.deepmatch.gradle.internal.model.AndroidManifest
import com.aouledissa.deepmatch.gradle.internal.model.Category
import com.aouledissa.deepmatch.gradle.internal.model.Fragment
import com.aouledissa.deepmatch.gradle.internal.model.Host
import com.aouledissa.deepmatch.gradle.internal.model.IntentFilter
import com.aouledissa.deepmatch.gradle.internal.model.IntentFilterCategory
import com.aouledissa.deepmatch.gradle.internal.model.PathPattern
import com.aouledissa.deepmatch.gradle.internal.model.Scheme
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import nl.adaptivity.xmlutil.serialization.XML
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateDeeplinkManifestFile : DefaultTask() {

    @get:InputFile
    abstract val specFileProperty: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }
    private val xmlSerializer by lazy { XML { indentString = " " } }

    @TaskAction
    fun generateDeeplinkManifest() {
        val specsFile = specFileProperty.asFile.get()

        logger.quiet("$LOG_TAG generating Android manifest file for deeplink specs in ${specsFile.path}")

        val deeplinkConfigs = yamlSerializer.deserializeDeeplinkConfigs(specsFile)
        val activities = deeplinkConfigs.groupBy { it.activity }.map { activity ->
            AndroidActivity(
                name = activity.key,
                intentFilter = activity.value.map { config ->
                    IntentFilter(
                        autoVerify = config.autoVerify,
                        action = listOf(Action("android.intent.action.VIEW")),
                        category = getFilterCategories(
                            categories = config.categories,
                            autoVerify = config.autoVerify
                        ).toList(),
                        scheme = config.scheme.map { Scheme(name = it) },
                        hosts = config.host.map { Host(name = it) },
                        pathPattern = buildPathPattern(config.pathParams.orEmpty()),
                        fragment = config.fragment?.let { Fragment(name = it) }
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

    private fun buildPathPattern(pathParams: List<Param>): PathPattern? {
        return when {
            pathParams.isEmpty() -> null
            pathParams.all { it.type == null } -> PathPattern(
                pattern = pathParams.joinToString(
                    prefix = "/",
                    separator = "/"
                ) { it.name }
            )

            else -> PathPattern(
                pattern = pathParams.joinToString(
                    prefix = "/",
                    separator = "/"
                ) { param ->
                    if (param.type == null) {
                        param.name
                    } else {
                        ".*"
                    }
                })
        }
    }
}