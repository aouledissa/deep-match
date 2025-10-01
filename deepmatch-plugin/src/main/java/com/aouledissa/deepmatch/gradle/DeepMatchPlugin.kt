package com.aouledissa.deepmatch.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.aouledissa.deepmatch.gradle.internal.config.configureAndroidVariants
import org.gradle.api.Plugin
import org.gradle.api.Project

internal const val LOG_TAG = "> DeepMatch:"

/**
 * Gradle entry point that wires DeepMatch into Android application and library
 * modules. The plugin configures per-variant tasks that parse YAML specs,
 * generate Kotlin sources, and optionally produce manifest snippets.
 */
class DeepMatchPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val config =
            target.extensions.create(DeepMatchPluginConfig.NAME, DeepMatchPluginConfig::class.java)

        when {
            target.plugins.hasPlugin(AppPlugin::class.java)
                    || target.plugins.hasPlugin(LibraryPlugin::class.java) -> {
                configureAndroidVariants(target, config)
            }
        }
    }
}
