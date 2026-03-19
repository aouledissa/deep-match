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
    val manifestSyncViolation: Property<ManifestSyncViolation> = objects.property(ManifestSyncViolation::class.java)
    val report: DeepMatchReportConfig = objects.newInstance(DeepMatchReportConfig::class.java)

    init {
        manifestSyncViolation.convention(ManifestSyncViolation.WARN)
    }

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

enum class ManifestSyncViolation {
    /** Log a warning when deeplink intent filters are missing from the manifest. */
    WARN,

    /** Fail the build when deeplink intent filters are missing from the manifest. */
    FAIL
}
