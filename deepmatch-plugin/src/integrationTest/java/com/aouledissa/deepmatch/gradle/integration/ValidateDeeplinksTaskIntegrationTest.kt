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

class ValidateDeeplinksTaskIntegrationTest {

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
                type: numeric
    """.trimIndent()

    @Test
    fun validateDeeplinksFailsWhenUriArgumentIsMissing() {
        fixture.withAndroidProject()
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result = fixture.run("validateDeeplinks", expectFailure = true)

        assertThat(result.task(":validateDeeplinks")!!.outcome)
            .isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("uriProperty")
    }

    @Test
    fun validateDeeplinksSucceedsWithMatchingUri() {
        fixture.withAndroidProject()
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result = fixture.run("validateDeeplinks", "--uri=app://example.com/profile/123")

        assertThat(result.task(":validateDeeplinks")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("[MATCH] open profile")
    }
}
