package com.aouledissa.deepmatch.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.aouledissa.deepmatch.gradle.internal.config.configureAndroidVariants
import org.gradle.api.Plugin
import org.gradle.api.Project

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