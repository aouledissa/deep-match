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
