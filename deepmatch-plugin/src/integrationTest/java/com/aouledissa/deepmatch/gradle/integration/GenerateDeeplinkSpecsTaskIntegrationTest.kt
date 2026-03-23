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

class GenerateDeeplinkSpecsTaskIntegrationTest {

    @get:Rule
    val fixture = GradleProjectFixture()

    private val validSpecYaml = """
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

    @Test
    fun generateDebugDeeplinkSpecsSucceedsAndProducesFiles() {
        fixture.withWorkingAndroidProject()
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result = fixture.run("generateDebugDeeplinkSpecs")

        assertThat(result.task(":generateDebugDeeplinkSpecs")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val metadataFile = File(
            fixture.projectDir,
            "build/generated/deepmatch/specs/debug/spec-shapes.json"
        )
        assertThat(metadataFile.exists()).isTrue()

        val metadataContent = metadataFile.readText()
        assertThat(metadataContent).contains("open profile")
    }

    @Test
    fun generateDebugDeeplinkSpecsIsUpToDateOnSecondRun() {
        fixture.withWorkingAndroidProject()
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result1 = fixture.run("generateDebugDeeplinkSpecs")
        assertThat(result1.task(":generateDebugDeeplinkSpecs")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val result2 = fixture.run("generateDebugDeeplinkSpecs")
        assertThat(result2.task(":generateDebugDeeplinkSpecs")!!.outcome)
            .isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun generateDebugDeeplinkSpecsFailsWithInvalidSpec() {
        fixture.withWorkingAndroidProject()
        fixture.writeFile(
            ".deeplinks.yml",
            """
            deeplinkSpecs:
              - name: "invalid spec"
                activity: com.example.app.MainActivity
                scheme: []
                host: ["example.com"]
            """.trimIndent()
        )

        val result = fixture.run("generateDebugDeeplinkSpecs", expectFailure = true)

        assertThat(result.task(":generateDebugDeeplinkSpecs")!!.outcome)
            .isEqualTo(TaskOutcome.FAILED)
    }
}
