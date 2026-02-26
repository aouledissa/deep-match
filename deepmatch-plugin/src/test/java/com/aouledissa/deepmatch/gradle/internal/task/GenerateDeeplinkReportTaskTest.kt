package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class GenerateDeeplinkReportTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `report includes specs metadata and valid example uris`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile(".deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    autoVerify: true
                    scheme: [https, app]
                    host: ["example.com"]
                    pathParams:
                      - name: users
                      - name: userId
                        type: alphanumeric
                    queryParams:
                      - name: campaign
                        type: string
                        required: true
                      - name: ref
                        type: string
                    fragment: "details"
                  - name: "open hostless profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: []
                    pathParams:
                      - name: profile
                      - name: profileId
                        type: numeric
                """.trimIndent()
            )
        }
        val outputFile = File(temporaryFolder.root, "reports/deeplinks.html")

        val task = project.tasks.register("generateReport", GenerateDeeplinkReportTask::class.java).get()
        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.moduleNameProperty.set("app")
        task.additionalModuleSourcesProperty.set(emptyList())
        task.outputFile.set(project.layout.file(project.provider { outputFile }))

        task.generate()

        assertThat(outputFile.exists()).isTrue()
        val html = outputFile.readText()

        assertThat(html).contains("DeepMatch Report - ")
        assertThat(html).contains("open profile")
        assertThat(html).contains("open hostless profile")
        assertThat(html).contains("Near miss:")
        assertThat(html).contains("Missing required query param:")
        assertThat(html).doesNotContain("<script src=")
        assertThat(html).doesNotContain("<link rel=\"stylesheet\" href=")

        val payload = extractPayload(html)

        assertThat(payload.module).isEqualTo("app")
        assertThat(payload.modules).hasSize(1)
        assertThat(payload.modules.first().name).isEqualTo("app")
        assertThat(payload.specs).hasSize(2)

        val openProfile = payload.specs.first { it.name == "open profile" }
        assertThat(openProfile.generatedSpec).isEqualTo("OpenProfileDeeplinkSpecs")
        assertThat(openProfile.generatedParams).isEqualTo("OpenProfileDeeplinkParams")
        assertThat(openProfile.module).isEqualTo("app")
        assertThat(openProfile.source).contains(".deeplinks.yml")
        assertThat(openProfile.autoVerify).isTrue()
        assertThat(openProfile.pathTemplate).isEqualTo("/users/{userId}")
        assertThat(openProfile.regex).contains("\\Q")
        assertThat(openProfile.queryParams).containsExactly(
            DeeplinkReportParam(name = "campaign", type = "string", required = true),
            DeeplinkReportParam(name = "ref", type = "string", required = false)
        ).inOrder()

        payload.specs.forEach { reportSpec ->
            assertExampleMatchesSpec(reportSpec)
        }

        val nearMiss = matchLikeReport(
            specs = payload.specs,
            uriValue = "app://example.com/users/abc123#details"
        )
        assertThat(nearMiss.matched).isFalse()
        assertThat(nearMiss.nearMissSpec).isEqualTo("open profile")
        assertThat(nearMiss.reason).contains("Missing required query param: campaign")
    }

    @Test
    fun `report generation is idempotent`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("idempotent.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open series"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                    pathParams:
                      - name: series
                      - name: seriesId
                        type: numeric
                """.trimIndent()
            )
        }
        val outputFile = File(temporaryFolder.root, "reports/idempotent.html")
        val task = project.tasks.register("generateIdempotentReport", GenerateDeeplinkReportTask::class.java).get()
        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.moduleNameProperty.set("sample")
        task.additionalModuleSourcesProperty.set(emptyList())
        task.outputFile.set(project.layout.file(project.provider { outputFile }))

        task.generate()
        val first = outputFile.readText()
        task.generate()
        val second = outputFile.readText()

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `report groups specs by module and source while keeping full catalog`() {
        val project = ProjectBuilder.builder().build()
        val appSpecsFile = temporaryFolder.newFile("app.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                    pathParams:
                      - name: profile
                """.trimIndent()
            )
        }
        val featureSpecsFile = temporaryFolder.newFile("feature.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open series"
                    activity: com.example.feature.MainActivity
                    scheme: [app]
                    host: ["feature.example.com"]
                    pathParams:
                      - name: series
                """.trimIndent()
            )
        }
        val outputFile = File(temporaryFolder.root, "reports/multi-module.html")
        val task = project.tasks.register("generateMultiModuleReport", GenerateDeeplinkReportTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { appSpecsFile }))
        task.moduleNameProperty.set("app")
        task.additionalSpecsFilesProperty.setFrom(project.layout.file(project.provider { featureSpecsFile }))
        task.additionalModuleSourcesProperty.set(
            listOf("feature::${featureSpecsFile.absolutePath}")
        )
        task.outputFile.set(project.layout.file(project.provider { outputFile }))

        task.generate()

        val payload = extractPayload(outputFile.readText())
        assertThat(payload.specs).hasSize(2)
        assertThat(payload.modules.map { it.name }).containsExactly("app", "feature")
        assertThat(payload.modules.first { it.name == "app" }.sources).hasSize(1)
        assertThat(payload.modules.first { it.name == "feature" }.sources).hasSize(1)
    }

    private fun assertExampleMatchesSpec(spec: DeeplinkReportSpec) {
        val deeplinkSpec = DeeplinkSpec(
            name = spec.name,
            scheme = spec.schemes.toSet(),
            host = spec.hosts.toSet(),
            port = spec.port,
            pathParams = spec.pathParams.map { param ->
                Param(name = param.name, type = toParamType(param.type), required = param.required)
            },
            queryParams = spec.queryParams.map { param ->
                Param(name = param.name, type = toParamType(param.type), required = param.required)
            }.toSet(),
            fragment = spec.fragment,
            paramsFactory = null
        )
        val uri = URI(spec.exampleUri)
        val normalized = uri.toStructuralString()
        val jsPattern = normalizeJavaRegexPatternForJs(spec.regex)

        assertThat(Regex("^$jsPattern$", RegexOption.IGNORE_CASE).matches(normalized)).isTrue()
        assertThat(deeplinkSpec.matcher.matches(normalized)).isTrue()

        val queryMap = uri.queryMap()
        assertThat(deeplinkSpec.matchesQueryParams { queryMap[it] }).isTrue()
    }

    private fun toParamType(type: String): ParamType? {
        return when (type) {
            "numeric" -> ParamType.NUMERIC
            "alphanumeric" -> ParamType.ALPHANUMERIC
            "string" -> ParamType.STRING
            else -> null
        }
    }

    private fun extractPayload(html: String): DeeplinkReportPayload {
        val markerStart = "const reportData = "
        val markerEnd = ";\nconst modules ="
        val start = html.indexOf(markerStart)
        val end = html.indexOf(markerEnd)
        val json = html.substring(start + markerStart.length, end)
        return Json.decodeFromString(DeeplinkReportPayload.serializer(), json)
    }

    private fun URI.toStructuralString(): String {
        val pathValue = path.orEmpty()
            .split("/")
            .filter { it.isNotBlank() }
            .joinToString("/")
            .trimEnd('/')
            .let { if (it.isNotEmpty()) "/$it" else "" }
        val hostValue = host.orEmpty()
        val portValue = if (port >= 0) ":$port" else ""
        val fragmentValue = fragment?.let { "#$it" }.orEmpty()
        return "${scheme}://$hostValue$portValue$pathValue$fragmentValue"
    }

    private fun URI.queryMap(): Map<String, String?> {
        val rawQuery = rawQuery ?: return emptyMap()
        return rawQuery
            .split("&")
            .mapNotNull { entry ->
                if (entry.isBlank()) return@mapNotNull null
                val parts = entry.split("=", limit = 2)
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
                val value = if (parts.size > 1) {
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                } else {
                    null
                }
                key to value
            }
            .toMap()
    }

    private fun normalizeJavaRegexPatternForJs(pattern: String): String {
        return Regex("""\\Q([\s\S]*?)\\E""")
            .replace(pattern) { match ->
                escapeRegexLiteral(match.groupValues[1])
            }
    }

    private fun escapeRegexLiteral(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '.', '*', '+', '?', '^', '$', '{', '}', '(', ')', '|', '[', ']', '\\' -> {
                        append('\\')
                        append(char)
                    }
                    else -> append(char)
                }
            }
        }
    }

    private fun matchLikeReport(
        specs: List<DeeplinkReportSpec>,
        uriValue: String
    ): ReportMatchResult {
        val uri = URI(uriValue)
        val structural = uri.toStructuralString()
        val queryMap = uri.queryMap()
        var nearMiss: ReportMatchResult? = null

        specs.forEach { spec ->
            val regex = Regex("^${spec.regex}$", setOf(RegexOption.IGNORE_CASE))
            if (!regex.matches(structural)) {
                return@forEach
            }

            var queryValid = true
            spec.queryParams.forEach { param ->
                val value = queryMap[param.name]
                if (value == null && param.required) {
                    queryValid = false
                    if (nearMiss == null) {
                        nearMiss = ReportMatchResult(
                            matched = false,
                            nearMissSpec = spec.name,
                            reason = "Missing required query param: ${param.name}"
                        )
                    }
                }
            }
            if (!queryValid) return@forEach

            return ReportMatchResult(matched = true, nearMissSpec = null, reason = null)
        }

        return nearMiss ?: ReportMatchResult(matched = false, nearMissSpec = null, reason = null)
    }

    private data class ReportMatchResult(
        val matched: Boolean,
        val nearMissSpec: String?,
        val reason: String?
    )
}
