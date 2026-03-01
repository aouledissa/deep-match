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
    val autoVerify: Boolean = false,
    val scheme: List<String>,
    val host: List<String> = emptyList(),
    val port: Int? = null,
    val pathParams: List<Param>? = null,
    val queryParams: List<Param>? = null,
    val fragment: String? = null
) {

    fun hasTypedParams(): Boolean =
        pathParams?.any { it.type != null } == true ||
                queryParams?.any { it.type != null } == true
}
