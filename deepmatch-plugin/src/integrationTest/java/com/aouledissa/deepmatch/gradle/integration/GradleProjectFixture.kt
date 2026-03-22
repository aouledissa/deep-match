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
 */
class GradleProjectFixture : ExternalResource() {

    private val temporaryFolder = TemporaryFolder()

    val projectDir: File
        get() = temporaryFolder.root

    override fun before() {
        temporaryFolder.create()
        writeFile(
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
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
     * Scaffold a minimal Android application project with the DeepMatch plugin
     * applied. The [buildScriptExtra] block is appended after the android and
     * deepMatch configuration blocks so callers can customise the extension.
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
        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args, "--stacktrace")

        return if (expectFailure) {
            runner.buildAndFail()
        } else {
            runner.build()
        }
    }
}
