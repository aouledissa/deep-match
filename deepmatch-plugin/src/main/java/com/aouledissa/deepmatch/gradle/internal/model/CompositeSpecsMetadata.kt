package com.aouledissa.deepmatch.gradle.internal.model

import com.aouledissa.deepmatch.api.ParamType
import kotlinx.serialization.Serializable

@Serializable
internal data class CompositeSpecsMetadata(
    val modulePath: String,
    val variant: String,
    val specs: List<CompositeSpecShape>
)

@Serializable
internal data class CompositeSpecShape(
    val name: String,
    val signature: String,
    val example: String
)

internal fun DeeplinkConfig.toCollisionSignature(): String {
    val schemes = scheme.map { it.lowercase() }.sorted().joinToString(",")
    val hosts = host.map { it.lowercase() }.sorted().joinToString(",")
    val portValue = port?.toString().orEmpty()
    val pathShape = pathParams.orEmpty()
        .joinToString("/") { param ->
            when (param.type) {
                null -> "static:${param.name.lowercase()}"
                ParamType.NUMERIC -> "typed:numeric"
                ParamType.ALPHANUMERIC -> "typed:alphanumeric"
                ParamType.STRING -> "typed:string"
            }
        }
    val queryShape = queryParams.orEmpty()
        .map { param ->
            val normalizedType = when (param.type) {
                ParamType.NUMERIC -> "numeric"
                ParamType.ALPHANUMERIC -> "alphanumeric"
                ParamType.STRING -> "string"
                null -> "none"
            }
            "query:${param.name.lowercase()}:$normalizedType:required=${param.required}"
        }
        .sorted()
        .joinToString("&")
    val fragmentValue = fragment?.lowercase().orEmpty()

    return buildString {
        append("scheme=")
        append(schemes)
        append("|host=")
        append(hosts)
        append("|port=")
        append(portValue)
        append("|path=")
        append(pathShape)
        append("|query=")
        append(queryShape)
        append("|fragment=")
        append(fragmentValue)
    }
}
