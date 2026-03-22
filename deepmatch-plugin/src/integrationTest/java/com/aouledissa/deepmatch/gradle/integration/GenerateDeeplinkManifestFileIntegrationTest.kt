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

class GenerateDeeplinkManifestFileIntegrationTest {

    @get:Rule
    val fixture = GradleProjectFixture()

    private val validSpecYaml = """
        deeplinkSpecs:
          - name: "open home"
            activity: com.example.app.MainActivity
            scheme: [app]
            host: ["example.com"]
    """.trimIndent()

    @Test
    fun generateDebugDeeplinksManifestSucceedsAndProducesXml() {
        fixture.withAndroidProject(
            buildScriptExtra = """
                deepMatch {
                    generateManifestFiles = true
                }
            """.trimIndent()
        )
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result = fixture.run("generateDebugDeeplinksManifest")

        assertThat(result.task(":generateDebugDeeplinksManifest")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val manifestFile = File(
            fixture.projectDir,
            "build/generated/manifests/generateDebugDeeplinksManifest/AndroidManifest.xml"
        )
        assertThat(manifestFile.exists()).isTrue()

        val manifestContent = manifestFile.readText()
        assertThat(manifestContent).contains("android.intent.action.VIEW")
        assertThat(manifestContent).contains("app")
        assertThat(manifestContent).contains("example.com")
    }

    @Test
    fun generateDebugDeeplinksManifestIsUpToDateOnSecondRun() {
        fixture.withAndroidProject(
            buildScriptExtra = """
                deepMatch {
                    generateManifestFiles = true
                }
            """.trimIndent()
        )
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result1 = fixture.run("generateDebugDeeplinksManifest")
        assertThat(result1.task(":generateDebugDeeplinksManifest")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val result2 = fixture.run("generateDebugDeeplinksManifest")
        assertThat(result2.task(":generateDebugDeeplinksManifest")!!.outcome)
            .isEqualTo(TaskOutcome.UP_TO_DATE)
    }
}
