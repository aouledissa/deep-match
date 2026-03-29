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
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GenerateDeeplinkManifestFileTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `static path generates exact path entries with trailing slash variant`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "about page"
                activity: com.example.app.MainActivity
                scheme: [https]
                host: ["example.com"]
                pathParams:
                  - name: about
            """.trimIndent()
        )

        assertThat(xml).contains("android:path=\"/about\"")
        assertThat(xml).contains("android:path=\"/about/\"")
        assertThat(xml).doesNotContain("android:pathPrefix=")
        assertThat(xml).doesNotContain("android:pathPattern=")
        assertThat(xml).doesNotContain("android:pathAdvancedPattern=")
    }

    @Test
    fun `typed segment at end generates pathPrefix`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "open profile"
                activity: com.example.app.MainActivity
                scheme: [app]
                host: ["example.com"]
                pathParams:
                  - name: users
                  - name: userId
                    type: alphanumeric
            """.trimIndent()
        )

        assertThat(xml).contains("android:pathPrefix=\"/users/\"")
        assertThat(xml).doesNotContain("android:pathPattern=")
    }

    @Test
    fun `typed segment in middle generates pathPattern and pathAdvancedPattern when compileSdk is 31+`() {
        val xml = generateManifest(
            yaml = """
            deeplinkSpecs:
              - name: "user posts"
                activity: com.example.app.MainActivity
                scheme: [app]
                host: ["example.com"]
                pathParams:
                  - name: users
                  - name: userId
                    type: numeric
                  - name: posts
            """.trimIndent(),
            compileSdk = 34
        )

        assertThat(xml).contains("android:pathPattern=\"/users/.*/posts\"")
        assertThat(xml).contains("android:pathAdvancedPattern=\"/users/[^/]+/posts\"")
        assertThat(xml).contains("tools:targetApi=\"31\"")
    }

    @Test
    fun `typed segment in middle generates only pathPattern when compileSdk is below 31`() {
        val xml = generateManifest(
            yaml = """
            deeplinkSpecs:
              - name: "user posts"
                activity: com.example.app.MainActivity
                scheme: [app]
                host: ["example.com"]
                pathParams:
                  - name: users
                  - name: userId
                    type: numeric
                  - name: posts
            """.trimIndent(),
            compileSdk = 30
        )

        assertThat(xml).contains("android:pathPattern=\"/users/.*/posts\"")
        assertThat(xml).doesNotContain("android:pathAdvancedPattern=")
    }

    @Test
    fun `all typed path generates only advanced pattern on compileSdk 31+`() {
        val xml = generateManifest(
            yaml = """
            deeplinkSpecs:
              - name: "dynamic route"
                activity: com.example.app.MainActivity
                scheme: [app]
                host: ["example.com"]
                pathParams:
                  - name: section
                    type: string
                  - name: itemId
                    type: numeric
            """.trimIndent(),
            compileSdk = 34
        )

        assertThat(xml).contains("android:pathAdvancedPattern=\"/[^/]+/[^/]+\"")
        assertThat(xml).doesNotContain("android:pathPattern=")
        assertThat(xml).doesNotContain("android:pathPrefix=")
        assertThat(xml).doesNotContain("android:path=\"")
    }

    @Test
    fun `all typed path omits path attributes when compileSdk is below 31`() {
        val xml = generateManifest(
            yaml = """
            deeplinkSpecs:
              - name: "dynamic route"
                activity: com.example.app.MainActivity
                scheme: [app]
                host: ["example.com"]
                pathParams:
                  - name: section
                    type: string
                  - name: itemId
                    type: numeric
            """.trimIndent(),
            compileSdk = 30
        )

        assertThat(xml).doesNotContain("android:path=\"")
        assertThat(xml).doesNotContain("android:pathPrefix=")
        assertThat(xml).doesNotContain("android:pathPattern=")
        assertThat(xml).doesNotContain("android:pathAdvancedPattern=")
    }

    @Test
    fun `hostless spec does not generate android host attribute`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "open profile"
                activity: com.example.app.MainActivity
                scheme: [app]
                pathParams:
                  - name: profile
                  - name: profileId
                    type: numeric
            """.trimIndent()
        )

        assertThat(xml).contains("android:scheme=\"app\"")
        assertThat(xml).doesNotContain("android:host=")
        assertThat(xml).contains("tools:ignore=\"AppLinkUrlError\"")
    }

    @Test
    fun `fragment and query params are not written to manifest`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "open profile"
                activity: com.example.app.MainActivity
                scheme: [https]
                host: ["example.com"]
                pathParams:
                  - name: users
                  - name: userId
                    type: alphanumeric
                queryParams:
                  - name: ref
                    type: string
                fragment: "details"
            """.trimIndent()
        )

        assertThat(xml).doesNotContain("android:fragment=")
        assertThat(xml).doesNotContain("android:query")
    }

    @Test
    fun `autoVerify true includes default and browsable categories`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "verify profile"
                activity: com.example.app.MainActivity
                scheme: [https]
                host: ["example.com"]
                autoVerify: true
                categories: [DEFAULT]
            """.trimIndent()
        )

        assertThat(xml).contains("android:autoVerify=\"true\"")
        assertThat(xml).contains("android:name=\"android.intent.category.DEFAULT\"")
        assertThat(xml).contains("android:name=\"android.intent.category.BROWSABLE\"")
    }

    @Test
    fun `autoVerify true splits mixed web and custom schemes into separate intent filters`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "profile spotlight"
                activity: com.example.app.MainActivity
                scheme: [https, app]
                host: ["example.com"]
                autoVerify: true
            """.trimIndent()
        )

        val autoVerifyFilter = Regex(
            "<intent-filter[^>]*android:autoVerify=\"true\"[\\s\\S]*?</intent-filter>"
        ).find(xml)?.value
        val customSchemeFilter = Regex(
            "<intent-filter(?![^>]*android:autoVerify=\"true\")[\\s\\S]*?android:scheme=\"app\"[\\s\\S]*?</intent-filter>"
        ).find(xml)?.value

        assertThat(Regex("<intent-filter").findAll(xml).count()).isEqualTo(2)
        assertThat(Regex("android:autoVerify=\"true\"").findAll(xml).count()).isEqualTo(1)
        assertThat(autoVerifyFilter).isNotNull()
        assertThat(autoVerifyFilter).contains("android:scheme=\"https\"")
        assertThat(autoVerifyFilter).doesNotContain("android:scheme=\"app\"")
        assertThat(customSchemeFilter).isNotNull()
    }

    @Test
    fun `multiple hosts generate separate intent filters for each host`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "multi-host deeplink"
                activity: com.example.app.MainActivity
                scheme: [https, app]
                host: ["example.com", "test.com"]
            """.trimIndent()
        )

        // With autoVerify=false (default), schemes are not split, so we get 1 filter per host = 2 filters
        val intentFilterCount = Regex("<intent-filter").findAll(xml).count()
        assertThat(intentFilterCount).isEqualTo(2)
        assertThat(xml).contains("android:host=\"example.com\"")
        assertThat(xml).contains("android:host=\"test.com\"")
        // Check that https appears 2 times (once for each host)
        assertThat(Regex("android:scheme=\"https\"").findAll(xml).count()).isEqualTo(2)
        // Check that app appears 2 times (once for each host)
        assertThat(Regex("android:scheme=\"app\"").findAll(xml).count()).isEqualTo(2)
    }

    @Test
    fun `multiple hosts with autoVerify generate separate intent filters for each host and scheme combination`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "multi-host deeplink with verify"
                activity: com.example.app.MainActivity
                scheme: [https, app]
                host: ["example.com", "test.com"]
                autoVerify: true
            """.trimIndent()
        )

        // With autoVerify=true, schemes are split (https vs app), so we get (2 scheme groups) * (2 hosts) = 4 filters
        val intentFilterCount = Regex("<intent-filter").findAll(xml).count()
        assertThat(intentFilterCount).isEqualTo(4)
        assertThat(xml).contains("android:host=\"example.com\"")
        assertThat(xml).contains("android:host=\"test.com\"")
        // https should appear in 2 filters (one autoVerify=true with example.com, one with test.com)
        assertThat(Regex("android:scheme=\"https\"").findAll(xml).count()).isEqualTo(2)
        // app should appear in 2 filters (one with example.com, one with test.com)
        assertThat(Regex("android:scheme=\"app\"").findAll(xml).count()).isEqualTo(2)
        // autoVerify should appear twice (once for each https host: example.com and test.com)
        assertThat(Regex("android:autoVerify=\"true\"").findAll(xml).count()).isEqualTo(2)
    }

    @Test
    fun `activity includes exported true and merge strategy`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "open profile"
                activity: com.example.app.MainActivity
                scheme: [app]
                host: ["example.com"]
            """.trimIndent()
        )

        assertThat(xml).contains("xmlns:tools=\"http://schemas.android.com/tools\"")
        assertThat(xml).contains("android:exported=\"true\"")
        assertThat(xml).contains("tools:node=\"merge\"")
    }

    @Test
    fun `generated manifest includes xml declaration`() {
        val xml = generateManifest(
            """
            deeplinkSpecs:
              - name: "open profile"
                activity: com.example.app.MainActivity
                scheme: [app]
                host: ["example.com"]
            """.trimIndent()
        )

        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
    }

    @Test
    fun `port is generated when provided and omitted otherwise`() {
        val withPort = generateManifest(
            """
            deeplinkSpecs:
              - name: "staging profile"
                activity: com.example.app.MainActivity
                scheme: [https]
                host: ["staging.example.com"]
                port: 8080
            """.trimIndent()
        )
        val withoutPort = generateManifest(
            """
            deeplinkSpecs:
              - name: "prod profile"
                activity: com.example.app.MainActivity
                scheme: [https]
                host: ["example.com"]
            """.trimIndent()
        )

        assertThat(withPort).contains("android:port=\"8080\"")
        assertThat(withoutPort).doesNotContain("android:port=")
    }

    @Test
    fun `additional specs files are merged into generated manifest`() {
        val project = ProjectBuilder.builder().build()
        val mainSpecsFile = temporaryFolder.newFile("main-${System.nanoTime()}.deeplinks.yml").apply {
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
        val extraSpecsFile = temporaryFolder.newFile("extra-${System.nanoTime()}.deeplinks.yml").apply {
            writeText(
                """
                deeplinkSpecs:
                  - name: "open series"
                    activity: com.example.app.MainActivity
                    scheme: [app]
                    host: ["series.example.com"]
                """.trimIndent()
            )
        }
        val outputManifest = temporaryFolder.newFile("AndroidManifest-${System.nanoTime()}.xml")
        val outputDir = outputManifest.parentFile
        val task = project.tasks.register(
            "generateManifestMultiFile${System.nanoTime()}",
            GenerateDeeplinkManifestFile::class.java
        ).get()

        task.specFileProperty.set(project.layout.file(project.provider { mainSpecsFile }))
        task.additionalSpecsFilesProperty.setFrom(
            project.layout.file(project.provider { extraSpecsFile })
        )
        task.outputFile.set(project.layout.file(project.provider { outputManifest }))
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))
        task.compileSdkProperty.set(34)
        task.verboseProperty.set(false)

        task.generateDeeplinkManifest()
        val xml = outputManifest.readText()

        assertThat(xml).contains("android:host=\"example.com\"")
        assertThat(xml).contains("android:host=\"series.example.com\"")
    }

    private fun generateManifest(yaml: String, compileSdk: Int = 34): String {
        val project = ProjectBuilder.builder().build()
        val specsFile = temporaryFolder.newFile("manifest-${System.nanoTime()}.deeplinks.yml").apply {
            writeText(yaml)
        }
        val outputManifest = temporaryFolder.newFile("AndroidManifest-${System.nanoTime()}.xml")
        val outputDir = outputManifest.parentFile
        val task = project.tasks.register(
            "generateManifest${System.nanoTime()}",
            GenerateDeeplinkManifestFile::class.java
        ).get()

        task.specFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.outputFile.set(project.layout.file(project.provider { outputManifest }))
        task.outputDir.set(project.layout.dir(project.provider { outputDir }))
        task.compileSdkProperty.set(compileSdk)
        task.verboseProperty.set(false)

        task.generateDeeplinkManifest()
        return outputManifest.readText()
    }
}
