package com.aouledissa.deepmatch.gradle.internal.model

import kotlinx.serialization.Serializable

@Serializable
internal enum class IntentFilterCategory(val manifestName: String) {
    BROWSABLE(manifestName = "android.intent.category.BROWSABLE"),
    DEFAULT(manifestName = "android.intent.category.DEFAULT"),
}