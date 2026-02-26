package com.aouledissa.deepmatch.gradle.internal.task

import com.google.common.truth.Truth.assertThat
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GenerateDeeplinkSpecsTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `fragment-only spec generates params class and wires paramsFactory`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile(".deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                    fragment: "details"
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated")
        val task = project.tasks.register("generateSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedPackageDir = File(outputDir, "com/example/app/deeplinks")
        val generatedSpecFile = File(generatedPackageDir, "OpenProfileDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecFile.exists()).isTrue()
        assertThat(generatedSpecCode).contains("pathParams = listOf(")
        assertThat(generatedSpecCode).doesNotContain("pathParams = setOf(")
        assertThat(generatedSpecCode).contains("data class OpenProfileDeeplinkParams(")
        assertThat(generatedSpecCode).contains("fragment: String")
        assertThat(generatedSpecCode).contains("paramsFactory = OpenProfileDeeplinkParams.Companion::fromMap")
        assertThat(generatedSpecCode).contains("internal fun fromMap")
    }

    @Test
    fun `query params required flag controls generated nullability`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("query-required.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "search"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                    queryParams:
                      - name: query
                        type: string
                        required: true
                      - name: ref
                        type: string
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-query")
        val task = project.tasks.register("generateRequiredSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/SearchDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecCode).contains("query: String")
        assertThat(generatedSpecCode).contains("ref: String?")
        assertThat(generatedSpecCode).contains("Param(name = \"query\", type = ParamType.STRING, required = true)")
        assertThat(generatedSpecCode).contains("Param(name = \"ref\", type = ParamType.STRING, required = false)")
    }

    @Test
    fun `static-only spec still generates params class and paramsFactory`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("static-only.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open home"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                    pathParams:
                      - name: home
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-static")
        val task = project.tasks.register("generateStaticSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/OpenHomeDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecFile.exists()).isTrue()
        assertThat(generatedSpecCode).contains("class OpenHomeDeeplinkParams()")
        assertThat(generatedSpecCode).contains("paramsFactory = OpenHomeDeeplinkParams.Companion::fromMap")
        assertThat(generatedSpecCode).contains("internal fun fromMap")
    }

    @Test
    fun `hostless spec generates empty host set`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("hostless.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
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
        val outputDir = temporaryFolder.newFolder("generated-hostless")
        val task = project.tasks.register("generateHostlessSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/OpenProfileDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecCode).contains("host = setOf()")
        assertThat(generatedSpecCode).contains("profileId = params[\"profileid\"]!!.toInt()")
        assertThat(generatedSpecCode).contains("= try")
        assertThat(generatedSpecCode).contains("catch (e: Exception)")
    }

    @Test
    fun `host omitted spec also generates empty host set`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("host-omitted.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    pathParams:
                      - name: profile
                      - name: profileId
                        type: numeric
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-host-omitted")
        val task = project.tasks.register("generateHostOmittedSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/OpenProfileDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecCode).contains("host = setOf()")
    }

    @Test
    fun `port is wired to generated deeplink spec`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("port-spec.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "staging profile"
                    activity: com.example.app.MainActivity
                    scheme: [https]
                    host: ["staging.example.com"]
                    port: 8080
                    pathParams:
                      - name: users
                      - name: userId
                        type: alphanumeric
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-port")
        val task = project.tasks.register("generatePortSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/StagingProfileDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecCode).contains("port = 8080")
    }

    @Test
    fun `empty scheme list throws validation error`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("invalid-scheme.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "invalid"
                    activity: com.example.app.MainActivity
                    scheme: []
                    host: ["example.com"]
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-invalid")
        val task = project.tasks.register("generateInvalidSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        val error = assertThrows(GradleException::class.java) {
            task.generateDeeplinkSpecs()
        }
        assertThat(error).hasMessageThat()
            .contains("Spec 'invalid' must define at least one scheme")
    }

    @Test
    fun `duplicate spec names throws validation error`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("duplicate-names.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [https]
                    host: ["example.com"]
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-duplicate")
        val task = project.tasks.register("generateDuplicateSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        val error = assertThrows(GradleException::class.java) {
            task.generateDeeplinkSpecs()
        }
        assertThat(error).hasMessageThat()
            .contains("Duplicate deeplink spec names found: open profile")
    }

    @Test
    fun `composite processors config generates processor extending composite processor`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("composite.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                    pathParams:
                      - name: profile
                      - name: userId
                        type: alphanumeric
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-composite")
        val task = project.tasks.register("generateCompositeSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(
            listOf(
                "com.example.feature.profile.deeplinks.ProfileDeeplinkProcessor",
                "com.example.feature.series.deeplinks.SeriesDeeplinkProcessor"
            )
        )
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val processorFile = File(outputDir, "com/example/app/deeplinks/AppDeeplinkProcessor.kt")
        val processorCode = processorFile.readText()

        assertThat(processorCode).contains("object AppDeeplinkProcessor : CompositeDeeplinkProcessor(")
        assertThat(processorCode).contains("ProfileDeeplinkProcessor")
        assertThat(processorCode).contains("SeriesDeeplinkProcessor")
    }

    @Test
    fun `additional specs files are merged into generated output`() {
        val project = ProjectBuilder.builder().build()
        val mainSpecsFile = temporaryFolder.newFile("main.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                """.trimIndent()
            )
        }
        val featureSpecsFile = temporaryFolder.newFile("feature.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open series"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["example.com"]
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-multi-file")
        val task = project.tasks.register("generateMergedSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { mainSpecsFile }))
        task.additionalSpecsFilesProperty.setFrom(
            project.layout.file(project.provider { featureSpecsFile })
        )
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedPackageDir = File(outputDir, "com/example/app/deeplinks")
        val profileSpec = File(generatedPackageDir, "OpenProfileDeeplinkSpecs.kt")
        val seriesSpec = File(generatedPackageDir, "OpenSeriesDeeplinkSpecs.kt")

        assertThat(profileSpec.exists()).isTrue()
        assertThat(seriesSpec.exists()).isTrue()
    }

    @Test
    fun `additional specs override base specs by name`() {
        val project = ProjectBuilder.builder().build()
        val mainSpecsFile = temporaryFolder.newFile("override-main.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["main.example.com"]
                """.trimIndent()
            )
        }
        val overrideSpecsFile = temporaryFolder.newFile("override-variant.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open profile"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["variant.example.com"]
                """.trimIndent()
            )
        }
        val outputDir = temporaryFolder.newFolder("generated-override")
        val task = project.tasks.register("generateOverrideSpecs", GenerateDeeplinkSpecsTask::class.java).get()

        task.specsFileProperty.set(project.layout.file(project.provider { mainSpecsFile }))
        task.additionalSpecsFilesProperty.setFrom(
            project.layout.file(project.provider { overrideSpecsFile })
        )
        task.packageNameProperty.set("com.example.app")
        task.moduleNameProperty.set("app")
        task.compositeProcessorsProperty.set(emptyList())
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/OpenProfileDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecCode).contains("host = setOf(\"variant.example.com\")")
        assertThat(generatedSpecCode).doesNotContain("main.example.com")
    }
}
