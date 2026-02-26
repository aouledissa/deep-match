package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.gradle.internal.model.CompositeSpecShape
import com.aouledissa.deepmatch.gradle.internal.model.CompositeSpecsMetadata
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ValidateCompositeSpecsCollisionsTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val jsonSerializer = Json { prettyPrint = false }

    @Test
    fun `validate succeeds when no collision exists`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register(
            "validateComposite",
            ValidateCompositeSpecsCollisionsTask::class.java
        ).get()

        val appMetadata = writeMetadata(
            fileName = "app.json",
            modulePath = ":app",
            specName = "open profile",
            signature = "scheme=app|host=example.com|port=|path=static:profile/typed:alphanumeric|query=|fragment="
        )
        val featureMetadata = writeMetadata(
            fileName = "feature.json",
            modulePath = ":feature-series",
            specName = "open series",
            signature = "scheme=app|host=example.com|port=|path=static:series/typed:numeric|query=|fragment="
        )

        task.metadataFiles.from(appMetadata, featureMetadata)
        task.variantNameProperty.set("debug")

        task.validate()
    }

    @Test
    fun `validate fails when two modules share same signature`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register(
            "validateCompositeCollision",
            ValidateCompositeSpecsCollisionsTask::class.java
        ).get()

        val sharedSignature = "scheme=app|host=example.com|port=|path=static:profile/typed:alphanumeric|query=|fragment="
        val appMetadata = writeMetadata(
            fileName = "app-collision.json",
            modulePath = ":app",
            specName = "open profile",
            signature = sharedSignature
        )
        val featureMetadata = writeMetadata(
            fileName = "feature-collision.json",
            modulePath = ":feature-profile",
            specName = "open profile feature",
            signature = sharedSignature
        )

        task.metadataFiles.from(appMetadata, featureMetadata)
        task.variantNameProperty.set("debug")

        val error = assertThrows(GradleException::class.java) {
            task.validate()
        }

        assertThat(error).hasMessageThat().contains("Found deeplink URI-shape collisions")
        assertThat(error).hasMessageThat().contains("module=:app")
        assertThat(error).hasMessageThat().contains("module=:feature-profile")
    }

    private fun writeMetadata(
        fileName: String,
        modulePath: String,
        specName: String,
        signature: String
    ): File {
        val file = temporaryFolder.newFile(fileName)
        val metadata = CompositeSpecsMetadata(
            modulePath = modulePath,
            variant = "debug",
            specs = listOf(
                CompositeSpecShape(
                    name = specName,
                    signature = signature,
                    example = "app://example.com/profile/abc"
                )
            )
        )
        file.writeText(jsonSerializer.encodeToString(CompositeSpecsMetadata.serializer(), metadata))
        return file
    }
}
