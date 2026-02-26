package com.aouledissa.deepmatch.gradle.internal.task

import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GenerateDeeplinkManifestFileTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `hostless spec does not generate android host attribute`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("hostless-manifest.deeplinks.yml").apply {
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
        val outputManifest = temporaryFolder.newFile("AndroidManifest.xml")
        val task = project.tasks.register("generateManifest", GenerateDeeplinkManifestFile::class.java).get()

        task.specFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.outputFile.set(project.layout.file(project.provider { outputManifest }))

        task.generateDeeplinkManifest()

        val xml = outputManifest.readText()
        assertThat(xml).contains("android:scheme=\"app\"")
        assertThat(xml).doesNotContain("android:host=")
    }
}
