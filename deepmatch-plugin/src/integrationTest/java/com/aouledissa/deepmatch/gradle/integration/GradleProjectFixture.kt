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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * JUnit @Rule that provides a temporary Gradle project directory and
 * simplifies running tests with [GradleRunner].
 *
 * The AGP version, DeepMatch plugin version, and local maven repository path are all driven
 * by system properties injected by the integration test Gradle tasks, enabling multi-version
 * testing against different AGP generations from the same test source set.
 *
 * | System property                   | Set by                        | Default              |
 * |-----------------------------------|-------------------------------|----------------------|
 * | `agp.version`                     | `integrationTestAgp8/9` tasks | `9.0.1`              |
 * | `deepmatch.plugin.version`        | `integrationTestAgp8/9` tasks | `0.0.0-SNAPSHOT`     |
 * | `integration.test.local.repo`     | `integrationTestAgp8/9` tasks | `~/.m2/repository`   |
 * | `gradle.version.for.test`         | `integrationTestAgp8` task    | *(none — inherits)*  |
 */
class GradleProjectFixture : ExternalResource() {

    private val temporaryFolder = TemporaryFolder()

    val projectDir: File
        get() = temporaryFolder.root

    val agpVersion: String = System.getProperty("agp.version", "9.0.1")
    private val deepMatchVersion: String = System.getProperty("deepmatch.plugin.version", "0.0.0-SNAPSHOT")
    private val localMavenRepo: String = System.getProperty(
        "integration.test.local.repo",
        "${System.getProperty("user.home")}/.m2/repository"
    )
    private val gradleVersionForTest: String? = System.getProperty("gradle.version.for.test")

    override fun before() {
        temporaryFolder.create()
        writeFile(
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    maven { url = uri("file://$localMavenRepo") }
                    google()
                    gradlePluginPortal()
                    mavenCentral()
                }
                plugins {
                    id("com.android.application") version "$agpVersion"
                    id("com.android.library") version "$agpVersion"
                    id("com.aouledissa.deepmatch.gradle") version "$deepMatchVersion"
                    id("org.jetbrains.kotlin.android") version "2.3.10"
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                    maven { url = uri("file://$localMavenRepo") }
                }
            }
            rootProject.name = "test-project"
            """.trimIndent()
        )
    }

    override fun after() {
        temporaryFolder.delete()
    }

    /**
     * Write a build script file. Defaults to build.gradle (Groovy).
     */
    fun withBuildScript(content: String, filename: String = "build.gradle") {
        writeFile(filename, content)
    }

    /**
     * Scaffold a minimal Android application project with the DeepMatch plugin applied.
     * Uses AGP's auto built-in Kotlin support (AGP >= 9 path).
     */
    fun withAndroidProject(buildScriptExtra: String = "") {
        val sdkDir = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: error("ANDROID_HOME or ANDROID_SDK_ROOT must be set")

        writeFile("local.properties", "sdk.dir=$sdkDir")
        writeFile("gradle.properties", "android.useAndroidX=true")
        writeFile(
            "src/main/AndroidManifest.xml",
            """<?xml version="1.0" encoding="utf-8"?>
            |<manifest />
            """.trimMargin()
        )
        withBuildScript(
            """
            plugins {
                id 'com.android.application'
                id 'com.aouledissa.deepmatch.gradle'
            }

            android {
                namespace 'com.example.app'
                compileSdk 33
                defaultConfig {
                    minSdk 21
                }
            }
            $buildScriptExtra
            """.trimIndent()
        )
    }

    /**
     * Scaffold a minimal Android application project that works for the current AGP version:
     * - AGP >= 9: uses [withAndroidProject] (built-in Kotlin, no standalone KGP needed)
     * - AGP < 9: uses [withAndroidStandaloneKotlinProject] (standalone KGP required for compilation)
     *
     * Use this in tests that just need "a working Android project with Kotlin" and do not care
     * about which source-wiring path is exercised. Use [withAndroidProject] or
     * [withAndroidStandaloneKotlinProject] directly only when testing version-specific behaviour.
     */
    fun withWorkingAndroidProject(buildScriptExtra: String = "") {
        if (agpVersion.startsWith("9")) {
            withAndroidProject(buildScriptExtra)
        } else {
            withAndroidStandaloneKotlinProject(buildScriptExtra)
        }
    }

    /**
     * Scaffold a minimal Android application project using the standalone Kotlin Gradle Plugin
     * (`org.jetbrains.kotlin.android`). Use this to exercise the AGP < 9 source-wiring path
     * where AGP does not automatically add the Kotlin source set to the classpath.
     */
    fun withAndroidStandaloneKotlinProject(buildScriptExtra: String = "") {
        val sdkDir = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
            ?: error("ANDROID_HOME or ANDROID_SDK_ROOT must be set")

        writeFile("local.properties", "sdk.dir=$sdkDir")
        writeFile("gradle.properties", "android.useAndroidX=true")
        writeFile(
            "src/main/AndroidManifest.xml",
            """<?xml version="1.0" encoding="utf-8"?>
            |<manifest />
            """.trimMargin()
        )
        withBuildScript(
            """
            plugins {
                id 'com.android.application'
                id 'org.jetbrains.kotlin.android'
                id 'com.aouledissa.deepmatch.gradle'
            }

            android {
                namespace 'com.example.app'
                compileSdk 33
                defaultConfig {
                    minSdk 21
                }
            }
            $buildScriptExtra
            """.trimIndent()
        )
    }

    /**
     * Write an arbitrary file relative to project root.
     */
    fun writeFile(relativePath: String, content: String): File {
        val file = File(projectDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }

    /**
     * Run a Gradle build in this project directory.
     *
     * @param args Gradle command-line arguments (e.g., "build", "help", "--uri=...")
     * @param expectFailure If true, call buildAndFail() instead of build()
     * @return The [BuildResult]
     */
    fun run(
        vararg args: String,
        expectFailure: Boolean = false
    ): BuildResult {
        var runner = GradleRunner.create()
            .withProjectDir(projectDir)
//            .withPluginClasspath()
            .withArguments(*args, "--stacktrace")
        if (gradleVersionForTest != null) {
            runner = runner.withGradleVersion(gradleVersionForTest)
        }

        return if (expectFailure) {
            runner.buildAndFail()
        } else {
            runner.build()
        }
    }
}