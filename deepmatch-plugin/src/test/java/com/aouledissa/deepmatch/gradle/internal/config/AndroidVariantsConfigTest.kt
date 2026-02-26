package com.aouledissa.deepmatch.gradle.internal.config

import com.aouledissa.deepmatch.gradle.DeepMatchPluginConfig
import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AndroidVariantsConfigTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `resolve composite processors auto-discovers deepmatch dependency modules`() {
        val root = rootProject("auto-discovery")
        val app = childProject(root, "app")
        val featureProfile = childProject(root, "feature-profile")
        val nonDeepMatch = childProject(root, "non-deepmatch")

        featureProfile.extensions.add(DeepMatchPluginConfig.NAME, Any())
        featureProfile.extensions.add("android", FakeAndroidExtension("com.example.profile"))
        nonDeepMatch.extensions.add("android", FakeAndroidExtension("com.example.non"))

        addProjectDependency(app, "implementation", featureProfile)
        addProjectDependency(app, "implementation", nonDeepMatch)

        val composite = resolveCompositeProcessorFqcns(
            project = app,
            variantName = "debug"
        )

        assertThat(composite).containsExactly(
            "com.example.profile.deeplinks.FeatureProfileDeeplinkProcessor"
        )
    }

    @Test
    fun `resolve composite processors are sorted and stable`() {
        val root = rootProject("merge")
        val app = childProject(root, "app")
        val featureProfile = childProject(root, "feature-profile")
        val featureSeries = childProject(root, "feature-series")

        featureProfile.extensions.add(DeepMatchPluginConfig.NAME, Any())
        featureProfile.extensions.add("android", FakeAndroidExtension("com.example.profile"))
        featureSeries.extensions.add(DeepMatchPluginConfig.NAME, Any())
        featureSeries.extensions.add("android", FakeAndroidExtension("com.example.series"))
        addProjectDependency(app, "implementation", featureProfile)
        addProjectDependency(app, "implementation", featureSeries)

        val composite = resolveCompositeProcessorFqcns(
            project = app,
            variantName = "debug"
        )

        assertThat(composite).containsExactly(
            "com.example.profile.deeplinks.FeatureProfileDeeplinkProcessor",
            "com.example.series.deeplinks.FeatureSeriesDeeplinkProcessor"
        ).inOrder()
    }

    @Test
    fun `resolve composite processors considers variant specific dependencies`() {
        val root = rootProject("variant")
        val app = childProject(root, "app")
        val featureProfile = childProject(root, "feature-profile")

        featureProfile.extensions.add(DeepMatchPluginConfig.NAME, Any())
        featureProfile.extensions.add("android", FakeAndroidExtension("com.example.profile"))
        addProjectDependency(app, "debugImplementation", featureProfile)

        val debugComposite = resolveCompositeProcessorFqcns(
            project = app,
            variantName = "debug"
        )
        val releaseComposite = resolveCompositeProcessorFqcns(
            project = app,
            variantName = "release"
        )

        assertThat(debugComposite).containsExactly(
            "com.example.profile.deeplinks.FeatureProfileDeeplinkProcessor"
        )
        assertThat(releaseComposite).isEmpty()
    }

    private fun rootProject(folderName: String): Project {
        val dir = temporaryFolder.newFolder(folderName)
        return ProjectBuilder.builder()
            .withName("root")
            .withProjectDir(dir)
            .build()
    }

    private fun childProject(root: Project, name: String): Project {
        val childDir = File(root.projectDir, name).apply { mkdirs() }
        return ProjectBuilder.builder()
            .withParent(root)
            .withName(name)
            .withProjectDir(childDir)
            .build()
    }

    private fun addProjectDependency(
        project: Project,
        configurationName: String,
        dependencyProject: Project
    ) {
        project.configurations.maybeCreate(configurationName)
        project.dependencies.add(
            configurationName,
            project.dependencies.project(mapOf("path" to dependencyProject.path))
        )
    }

    private class FakeAndroidExtension(private val namespace: String) {
        fun getNamespace(): String = namespace
    }
}
