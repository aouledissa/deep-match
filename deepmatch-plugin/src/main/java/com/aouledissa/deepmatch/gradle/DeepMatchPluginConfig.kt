package com.aouledissa.deepmatch.gradle

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration for the DeepMatch Gradle plugin.
 *
 * Exposes options for manifest generation and report generation.
 */
abstract class DeepMatchPluginConfig @Inject constructor(objects: ObjectFactory) {

    val generateManifestFiles: Property<Boolean> = objects.property(Boolean::class.java)
    val report: DeepMatchReportConfig = objects.newInstance(DeepMatchReportConfig::class.java)

    fun report(action: Action<in DeepMatchReportConfig>) {
        action.execute(report)
    }

    companion object {
        internal const val NAME: String = "deepMatch"
    }
}

abstract class DeepMatchReportConfig @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java)
    val output: RegularFileProperty = objects.fileProperty()

    init {
        enabled.convention(false)
    }
}
