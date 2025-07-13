package com.aouledissa.deepmatch.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration for the DeepMatch Gradle plugin.
 *
 * This class allows clients to configure the behavior of the DeepMatch plugin,
 * primarily by specifying whether to generate AndroidManifest.xml files.
 *
 * @property generateManifestFiles A boolean flag indicating whether the plugin should
 *                                 generate `AndroidManifest.xml` files based on the
 *                                 deeplink specifications. Defaults to `true`.
 *                                 Set to `false` if you prefer to manage your manifest
 *                                 entries manually or use another mechanism.
 *                                 Example: `generateManifestFiles = false`
 */
abstract class DeepMatchPluginConfig @Inject constructor(objects: ObjectFactory) {

    val generateManifestFiles: Property<Boolean> = objects.property(Boolean::class.java)

    companion object {
        internal const val NAME: String = "deepMatch"
    }
}
