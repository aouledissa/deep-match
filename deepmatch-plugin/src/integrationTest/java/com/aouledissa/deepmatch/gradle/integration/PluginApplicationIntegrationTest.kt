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

class PluginApplicationIntegrationTest {

    @get:Rule
    val fixture = GradleProjectFixture()

    @Test
    fun deepMatchPluginAppliesWithoutAndroidPlugin() {
        // The plugin registers the deepMatch extension unconditionally. When no
        // Android plugin (AppPlugin / LibraryPlugin) is applied, it silently
        // skips variant configuration — but must not throw.
        fixture.withBuildScript(
            """
            plugins {
                id("java-library")
                id("com.aouledissa.deepmatch.gradle")
            }
            """.trimIndent(),
            filename = "build.gradle.kts"
        )

        val result = fixture.run("help")

        assertThat(result.task(":help")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun deepMatchExtensionIsAccessibleAfterPluginApplied() {
        // Verify the deepMatch extension is registered so that consumers can
        // configure it without a "Extension of type ... does not exist" error.
        fixture.withBuildScript(
            """
            plugins {
                id("java-library")
                id("com.aouledissa.deepmatch.gradle")
            }
            deepMatch {
                generateManifestFiles.set(true)
            }
            """.trimIndent(),
            filename = "build.gradle.kts"
        )

        val result = fixture.run("help")

        assertThat(result.task(":help")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

}
