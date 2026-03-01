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

package com.aouledissa.deepmatch.api

object DeeplinkSpecFixtures {

    fun create(
        name: String = "",
        scheme: Set<String> = setOf(),
        host: Set<String> = setOf(),
        port: Int? = null,
        pathParams: List<Param> = emptyList(),
        queryParams: Set<Param> = setOf(),
        fragment: String? = null,
        paramsFactory: ((Map<String, String?>) -> DeeplinkParams?)? = null
    ): DeeplinkSpec {
        return DeeplinkSpec(
            name = name,
            scheme = scheme,
            host = host,
            port = port,
            pathParams = pathParams,
            queryParams = queryParams,
            fragment = fragment,
            paramsFactory = paramsFactory
        )
    }
}
