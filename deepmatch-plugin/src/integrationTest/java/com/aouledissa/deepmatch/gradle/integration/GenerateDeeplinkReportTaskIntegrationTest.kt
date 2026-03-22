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

package com.aouledissa.deepmatch.gradle.integration

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import java.io.File

class GenerateDeeplinkReportTaskIntegrationTest {

    @get:Rule
    val fixture = GradleProjectFixture()

    private val validSpecYaml = """
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

    @Test
    fun generateDeeplinkReportSucceedsAndProducesHtmlReport() {
        fixture.withAndroidProject(
            buildScriptExtra = """
                deepMatch {
                    report {
                        enabled = true
                    }
                }
            """.trimIndent()
        )
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result = fixture.run("generateDeeplinkReport")

        assertThat(result.task(":generateDeeplinkReport")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val reportFile = File(
            fixture.projectDir,
            "build/reports/deepmatch/deeplinks-catalogue.html"
        )
        assertThat(reportFile.exists()).isTrue()

        val reportContent = reportFile.readText()
        assertThat(reportContent).contains("<html")
        assertThat(reportContent).contains("open series")
    }
}
