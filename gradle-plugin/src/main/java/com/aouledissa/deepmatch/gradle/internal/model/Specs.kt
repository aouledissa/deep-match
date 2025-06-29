package com.aouledissa.deepmatch.gradle.internal.model

import kotlinx.serialization.Serializable

@Serializable
internal data class Specs(
    val deeplinkSpecs: List<DeeplinkConfig>
)

@Serializable
internal data class DeeplinkConfig(
    val name: String,
    val scheme: String,
    val host: String,
)