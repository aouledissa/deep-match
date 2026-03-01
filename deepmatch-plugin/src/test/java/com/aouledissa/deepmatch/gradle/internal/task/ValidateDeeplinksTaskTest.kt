/*
 * Copyright 2026 DeepMatch Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aouledissa.deepmatch.gradle.internal.task

import com.google.common.truth.Truth.assertThat
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ValidateDeeplinksTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `validate throws when uri option is missing`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile(".deeplinks.yml").apply {
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
        val task = project.tasks.register("validate", ValidateDeeplinksTask::class.java).get()
        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))

        val error = assertThrows(GradleException::class.java) {
            task.validate()
        }

        assertThat(error).hasMessageThat().contains("--uri option is required")
    }

    @Test
    fun `validate handles malformed uri gracefully`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("invalid-uri.deeplinks.yml").apply {
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
        val task = project.tasks.register("validateMalformed", ValidateDeeplinksTask::class.java).get()
        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.uriProperty.set("://bad-uri")

        task.validate()
    }

    @Test
    fun `validate runs for matching uri`() {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("valid-uri.deeplinks.yml").apply {
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
                        type: numeric
                """.trimIndent()
            )
        }
        val task = project.tasks.register("validateMatch", ValidateDeeplinksTask::class.java).get()
        task.specsFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.uriProperty.set("app://example.com/profile/123")

        task.validate()
    }

    @Test
    fun `validate reads additional specs files`() {
        val project = ProjectBuilder.builder().build()
        val mainSpecsFile = temporaryFolder.newFile("validate-main.deeplinks.yml").apply {
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
        val extraSpecsFile = temporaryFolder.newFile("validate-extra.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open series"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["series.example.com"]
                    pathParams:
                      - name: series
                      - name: seriesId
                        type: numeric
                """.trimIndent()
            )
        }
        val task = project.tasks.register("validateAdditional", ValidateDeeplinksTask::class.java).get()
        task.specsFileProperty.set(project.layout.file(project.provider { mainSpecsFile }))
        task.additionalSpecsFilesProperty.setFrom(
            project.layout.file(project.provider { extraSpecsFile })
        )
        task.uriProperty.set("app://series.example.com/series/123")

        task.validate()
    }
}
