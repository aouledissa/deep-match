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
    fun `fragment-only spec generates params class and wires parametersClass`() {
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
        assertThat(generatedSpecCode).contains("parametersClass = OpenProfileDeeplinkParams::class")
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
    fun `static-only spec still generates params class and parametersClass`() {
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
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/OpenHomeDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecFile.exists()).isTrue()
        assertThat(generatedSpecCode).contains("class OpenHomeDeeplinkParams()")
        assertThat(generatedSpecCode).contains("parametersClass = OpenHomeDeeplinkParams::class")
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
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/OpenProfileDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecCode).contains("host = setOf()")
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
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        task.generateDeeplinkSpecs()

        val generatedSpecFile = File(outputDir, "com/example/app/deeplinks/OpenProfileDeeplinkSpecs.kt")
        val generatedSpecCode = generatedSpecFile.readText()

        assertThat(generatedSpecCode).contains("host = setOf()")
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
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))

        val error = assertThrows(GradleException::class.java) {
            task.generateDeeplinkSpecs()
        }
        assertThat(error).hasMessageThat()
            .contains("Duplicate deeplink spec names found: open profile")
    }
}
