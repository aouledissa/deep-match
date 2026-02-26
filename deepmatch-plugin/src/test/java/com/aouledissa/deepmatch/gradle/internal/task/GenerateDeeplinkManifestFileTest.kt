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
        val task = project.tasks.register(
            "generateManifestMultiFile${System.nanoTime()}",
            GenerateDeeplinkManifestFile::class.java
        ).get()

        task.specFileProperty.set(project.layout.file(project.provider { mainSpecsFile }))
        task.additionalSpecsFilesProperty.setFrom(
            project.layout.file(project.provider { extraSpecsFile })
        )
        task.outputFile.set(project.layout.file(project.provider { outputManifest }))
        task.compileSdkProperty.set(34)

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
        val task = project.tasks.register(
            "generateManifest${System.nanoTime()}",
            GenerateDeeplinkManifestFile::class.java
        ).get()

        task.specFileProperty.set(project.layout.file(project.provider { specsFile }))
        task.outputFile.set(project.layout.file(project.provider { outputManifest }))
        task.compileSdkProperty.set(compileSdk)

        task.generateDeeplinkManifest()
        return outputManifest.readText()
    }
}
