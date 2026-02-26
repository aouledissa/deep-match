package com.aouledissa.deepmatch.gradle.internal.task

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
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
}
