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
import org.junit.Rule
import org.junit.Test

class ConfigurationCacheIntegrationTest {

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
    fun checkTaskIsConfigurationCacheCompatible() {
        fixture.withWorkingAndroidProject(
            buildScriptExtra = """
                android {
                    lint {
                        abortOnError false
                    }
                }
                dependencies {
                    implementation 'com.aouledissa.deepmatch:deepmatch-processor:${fixture.deepMatchVersion}'
                }
                deepMatch {
                    generateManifestFiles = true
                    report {
                        enabled = true
                    }
                }
            """.trimIndent()
        )
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        // Warm-up: populates .gradle/ and build/ so the project root directory listing
        // is stable before the CC entry is stored. This mimics steady-state usage.
        // Without this, the first CC run stores the entry against an empty root,
        // then the second run sees .gradle/ and build/ as new entries and invalidates it.
        fixture.run("check")

        // First run: verifies no CC violations — entry must be stored cleanly.
        val firstResult = fixture.run("check", "--configuration-cache")
        assertThat(firstResult.output).contains("Configuration cache entry stored")

        // Second run: verifies the CC entry is reused with no re-configuration.
        val secondResult = fixture.run("check", "--configuration-cache")
        assertThat(secondResult.output).contains("Configuration cache entry reused")
    }
}
