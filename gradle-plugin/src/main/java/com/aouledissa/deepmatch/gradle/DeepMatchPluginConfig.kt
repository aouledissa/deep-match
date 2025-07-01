package com.aouledissa.deepmatch.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration for the DeepMatch Gradle plugin.
 *
 * This class allows users to configure the behavior of the DeepMatch plugin,
 * primarily by specifying the location of the deeplinks specifications file
 * and whether to generate AndroidManifest.xml files.
 *
 * @property specsFile The path to the deeplinks specifications file used by DeepMatch.
 *                     This file defines the matching rules and configurations.
 *                     Example: `specsFile = "path/to/your/specs.yaml"`
 *
 * @property generateManifestFiles A boolean flag indicating whether the plugin should
 *                                 generate `AndroidManifest.xml` files based on the
 *                                 deeplink specifications. Defaults to `true`.
 *                                 Set to `false` if you prefer to manage your manifest
 *                                 entries manually or use another mechanism.
 *                                 Example: `generateManifestFiles = false`
 */
abstract class DeepMatchPluginConfig @Inject constructor(objects: ObjectFactory) {

    val specsFile: Property<String> = objects.property(String::class.java)

    val generateManifestFiles: Property<Boolean> = objects.property(Boolean::class.java)

    companion object {
        internal const val NAME: String = "deepMatch"
    }
}
