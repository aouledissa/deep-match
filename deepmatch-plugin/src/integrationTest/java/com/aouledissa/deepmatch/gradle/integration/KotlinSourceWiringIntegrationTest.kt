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
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the source-set wiring logic in [addGeneratedSourceDirectory]:
 *
 * - AGP < 9 + `org.jetbrains.kotlin.android`: generated sources are added to the **java** source
 *   set so that KGP can promote them to the Kotlin compilation classpath.
 * - AGP >= 9 (auto built-in Kotlin): [Sources.kotlin] is non-null so generated sources are added
 *   directly to the **kotlin** source set.
 * - No Kotlin support at all (AGP < 9, no KGP): a [GradleException] is thrown with an actionable
 *   message.
 */
class KotlinSourceWiringIntegrationTest {

    @get:Rule
    val fixture = GradleProjectFixture()


    private val validSpecYaml = """
        deeplinkSpecs:
          - name: "open profile"
            activity: com.example.app.MainActivity
            scheme: [app]
            host: ["example.com"]
    """.trimIndent()

    /**
     * Registers a `dumpSourceSets` task in the build script that prints all kotlin and java
     * source directories for the debug variant to stdout, one per line, prefixed with
     * "KOTLIN_SRC:" or "JAVA_SRC:". Used to assert which source set the generated directory
     * ends up in.
     */
    private val dumpSourceSetsBuildScriptExtra = """
        def components = extensions.getByType(com.android.build.api.variant.AndroidComponentsExtension)
        components.onVariants(components.selector().withName('debug')) { variant ->
            tasks.register('dumpSourceSets') {
                dependsOn 'generateDebugDeeplinkSpecs'
                def kotlinSrcs = variant.sources.kotlin?.all
                def javaSrcs = variant.sources.java?.all
                doLast {
                    kotlinSrcs?.get()?.each { println('KOTLIN_SRC:' + it.asFile.canonicalPath) }
                    javaSrcs?.get()?.each { println('JAVA_SRC:' + it.asFile.canonicalPath) }
                }
            }
        }
    """.trimIndent()

    @Test
    fun `standalone KGP wires generated sources to java source set and includes them on the classpath`() {
        assumeTrue("Standalone KGP conflicts with AGP >= 9 built-in Kotlin — only testable on AGP < 9", !fixture.agpVersion.startsWith("9"))
        fixture.withAndroidStandaloneKotlinProject(buildScriptExtra = dumpSourceSetsBuildScriptExtra)
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val genResult = fixture.run("generateDebugDeeplinkSpecs")
        assertThat(genResult.task(":generateDebugDeeplinkSpecs")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val generatedFiles = fixture.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        assertThat(generatedFiles).isNotEmpty()

        val dumpResult = fixture.run("dumpSourceSets")
        val javaSrcRoots = dumpResult.output.lines()
            .filter { it.startsWith("JAVA_SRC:") }
            .map { it.removePrefix("JAVA_SRC:").trim() }
        val generatedFilePath = generatedFiles.first().canonicalPath
        assertThat(javaSrcRoots.any { generatedFilePath.startsWith(it) }).isTrue()

        // compileDebugKotlin must declare generateDebugDeeplinkSpecs as a dependency,
        // proving the generated sources are on the compilation classpath
        val dryRunResult = fixture.run("compileDebugKotlin", "--dry-run")
        assertThat(dryRunResult.output).contains(":generateDebugDeeplinkSpecs")
    }

    @Test
    fun `AGP built-in Kotlin wires generated sources to kotlin source set and includes them on the classpath`() {
        assumeTrue("AGP built-in Kotlin is only available on AGP >= 9", fixture.agpVersion.startsWith("9"))
        fixture.withAndroidProject(buildScriptExtra = dumpSourceSetsBuildScriptExtra)
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val genResult = fixture.run("generateDebugDeeplinkSpecs")
        assertThat(genResult.task(":generateDebugDeeplinkSpecs")!!.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val generatedFiles = fixture.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        assertThat(generatedFiles).isNotEmpty()

        val dumpResult = fixture.run("dumpSourceSets")
        val kotlinSrcRoots = dumpResult.output.lines()
            .filter { it.startsWith("KOTLIN_SRC:") }
            .map { it.removePrefix("KOTLIN_SRC:").trim() }
        val generatedFilePath = generatedFiles.first().canonicalPath
        assertThat(kotlinSrcRoots.any { generatedFilePath.startsWith(it) }).isTrue()

        // compileDebugKotlin must declare generateDebugDeeplinkSpecs as a dependency,
        // proving the generated sources are on the compilation classpath
        val dryRunResult = fixture.run("compileDebugKotlin", "--dry-run")
        assertThat(dryRunResult.output).contains(":generateDebugDeeplinkSpecs")
    }

    @Test
    fun `fails with actionable error when no Kotlin support is available`() {
        assumeTrue("Sources.kotlin is always non-null on AGP >= 9, so this error path is only reachable on AGP < 9", !fixture.agpVersion.startsWith("9"))
        // This branch is only reachable on AGP < 9 without org.jetbrains.kotlin.android,
        // where Sources.kotlin is null.
        val sdkDir = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: error("ANDROID_HOME or ANDROID_SDK_ROOT must be set")

        fixture.writeFile("local.properties", "sdk.dir=$sdkDir")
        fixture.writeFile("gradle.properties", "android.useAndroidX=true")
        fixture.writeFile(
            "src/main/AndroidManifest.xml",
            """<?xml version="1.0" encoding="utf-8"?>
            |<manifest />
            """.trimMargin()
        )
        fixture.withBuildScript(
            """
            plugins {
                id 'com.android.application'
                id 'com.aouledissa.deepmatch.gradle'
            }
            android {
                namespace 'com.example.app'
                compileSdk 33
                defaultConfig { minSdk 21 }
            }
            """.trimIndent()
        )
        fixture.writeFile(".deeplinks.yml", validSpecYaml)

        val result = fixture.run("generateDebugDeeplinkSpecs", expectFailure = true)

        assertThat(result.output).contains(
            "Apply the 'org.jetbrains.kotlin.android' plugin if you are on AGP < 9, " +
                "or upgrade to AGP >= 9."
        )
    }
}