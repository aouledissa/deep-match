package com.aouledissa.deepmatch.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class DeepMatchPluginConfig @Inject constructor(objects: ObjectFactory) {

    val specsFile: Property<String> = objects.property(String::class.java)

    companion object {
        internal const val NAME: String = "deepMatch"
    }
}
