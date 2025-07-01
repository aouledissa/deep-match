package com.aouledissa.deepmatch.gradle.internal.model

import com.aouledissa.deepmatch.api.Param
import kotlinx.serialization.Serializable

@Serializable
internal data class Specs(
    val deeplinkSpecs: List<DeeplinkConfig>
)

@Serializable
internal data class DeeplinkConfig(
    val name: String,
    val activity: String,
    val categories: List<IntentFilterCategory> = listOf(IntentFilterCategory.DEFAULT),
    val autoVerify: Boolean? = false,
    val scheme: String,
    val host: String,
    val pathParams: List<Param>? = null,
    val queryParams: List<Param>? = null,
    val fragment: String? = null
) {

    fun containsTemplateParams(): Boolean {
        return pathParams?.any { it.type != null } == true || queryParams.isNullOrEmpty().not()
    }
}