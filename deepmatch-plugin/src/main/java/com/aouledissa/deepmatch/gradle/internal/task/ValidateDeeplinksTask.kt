package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.internal.deserializeMergedDeeplinkConfigs
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal abstract class ValidateDeeplinksTask : DefaultTask() {

    @get:InputFile
    abstract val specsFileProperty: RegularFileProperty

    @get:InputFiles
    abstract val additionalSpecsFilesProperty: ConfigurableFileCollection

    @get:Input
    @get:Option(option = "uri", description = "URI to validate against deeplink specs")
    abstract val uriProperty: Property<String>

    private val yamlSerializer by lazy { Yaml(configuration = YamlConfiguration(strictMode = false)) }

    @TaskAction
    fun validate() {
        val uriValue = uriProperty.orNull?.takeIf { it.isNotBlank() }
            ?: throw GradleException("DeepMatch: --uri option is required for validateDeeplinks")
        val uri = try {
            URI(uriValue)
        } catch (e: Exception) {
            logger.quiet("$LOG_TAG validating '$uriValue'")
            logger.quiet("  Invalid URI: ${e.message}")
            return
        }
        val specsFiles = buildList {
            add(specsFileProperty.asFile.get())
            addAll(additionalSpecsFilesProperty.files.sortedBy { it.absolutePath })
        }
        val configs = yamlSerializer.deserializeMergedDeeplinkConfigs(specsFiles)

        logger.quiet("$LOG_TAG validating '$uriValue'")
        var hasMatch = false

        configs.forEach { config ->
            val spec = DeeplinkSpec(
                name = config.name,
                scheme = config.scheme.toSet(),
                host = config.host.toSet(),
                port = config.port,
                pathParams = config.pathParams.orEmpty(),
                queryParams = config.queryParams.orEmpty().toSet(),
                fragment = config.fragment,
                paramsFactory = null
            )

            when (val result = tryMatch(spec, uri)) {
                is MatchResult.Success -> {
                    hasMatch = true
                    logger.quiet("  [MATCH] ${config.name}")
                    result.params.forEach { (key, value) ->
                        logger.quiet("    $key = $value")
                    }
                }

                is MatchResult.Failed -> {
                    logger.quiet("  [MISS] ${config.name} - ${result.reason}")
                }
            }
        }

        if (!hasMatch) {
            logger.quiet("  No spec matched.")
        }
    }

    private fun tryMatch(spec: DeeplinkSpec, uri: URI): MatchResult {
        val decoded = uri.decoded() ?: return MatchResult.Failed("malformed URI (missing scheme)")
        if (!spec.matcher.matches(decoded)) {
            return MatchResult.Failed("URI pattern did not match")
        }

        val queryParams = uri.queryMap()
        val queryMatches = spec.matchesQueryParams { key -> queryParams[key] }
        if (!queryMatches) {
            return MatchResult.Failed("query params validation failed")
        }

        val extractedParams = linkedMapOf<String, String?>()
        val pathSegments = uri.pathSegments()

        spec.pathParams.forEachIndexed { index, param ->
            val paramType = param.type ?: return@forEachIndexed
            if (index >= pathSegments.size) {
                return MatchResult.Failed("path length mismatch")
            }
            val value = pathSegments[index]
            if (!paramType.regex.matches(value)) {
                return MatchResult.Failed("path param '${param.name}' failed type validation")
            }
            extractedParams[param.name.lowercase()] = value
        }

        spec.queryParams.forEach { param ->
            val paramType = param.type ?: return@forEach
            val value = queryParams[param.name]
            if (param.required && value == null) {
                return MatchResult.Failed("missing required query param '${param.name}'")
            }
            if (value != null && !paramType.regex.matches(value)) {
                return MatchResult.Failed("query param '${param.name}' failed type validation")
            }
            extractedParams[param.name.lowercase()] = value
        }

        spec.fragment?.let { extractedParams["fragment"] = uri.fragment }
        return MatchResult.Success(extractedParams)
    }

    private fun URI.decoded(): String? {
        val decodedScheme = scheme ?: return null
        val decodedPath = pathSegments()
            .joinToString("/")
            .trimEnd('/')
            .let { if (it.isNotEmpty()) "/$it" else "" }
        val decodedHost = host.orEmpty()
        val decodedPort = if (port >= 0) ":$port" else ""
        val decodedFragment = fragment?.let { "#$it" }.orEmpty()
        return "$decodedScheme://$decodedHost$decodedPort$decodedPath$decodedFragment"
    }

    private fun URI.pathSegments(): List<String> {
        return path.orEmpty()
            .split('/')
            .filter { it.isNotBlank() }
    }

    private fun URI.queryMap(): Map<String, String?> {
        val rawQuery = rawQuery ?: return emptyMap()
        return rawQuery
            .split("&")
            .mapNotNull { entry ->
                if (entry.isBlank()) return@mapNotNull null
                val parts = entry.split("=", limit = 2)
                val key = decode(parts[0])
                val value = if (parts.size > 1) decode(parts[1]) else null
                key to value
            }
            .toMap()
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }

    private sealed interface MatchResult {
        data class Success(val params: Map<String, String?>) : MatchResult
        data class Failed(val reason: String) : MatchResult
    }
}
