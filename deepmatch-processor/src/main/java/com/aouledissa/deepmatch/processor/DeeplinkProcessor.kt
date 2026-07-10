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

package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec

/**
 * Matches incoming URIs against registered deeplink specs and returns decoded
 * params for the first match.
 */
open class DeeplinkProcessor(
    private val specs: Set<DeeplinkSpec>
) {
    /**
     * Attempts to match [deeplink] against the configured specs and returns the
     * decoded params for the first matching spec, or `null` when no match is
     * found.
     */
    open fun match(deeplink: Uri): DeeplinkParams? {
        return try {
            val matchTarget = deeplink.matchTarget() ?: return null

            for (spec in specs) {
                val regexMatch = spec.matcher.matches(matchTarget)
                if (!regexMatch) {
                    continue
                }

                val queryMatch = spec.matchesQueryParams(deeplink::getQueryParameter)
                if (!queryMatch) {
                    continue
                }

                val params = buildDeeplinkParams(spec, deeplink)
                if (params == null) {
                    continue
                }

                return params
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds the string used for structural matching from the URI's **encoded**
     * components. Matching on the encoded form keeps percent-encoded delimiters
     * (e.g. `%2F`, `%23`) distinct from real delimiters, so a single encoded
     * segment cannot masquerade as multiple path segments. Individual param
     * values are decoded later, only when they are extracted.
     */
    private fun Uri.matchTarget(): String? {
        val scheme = scheme ?: return null
        val path = encodedPath.orEmpty().trimEnd('/')
        val fragment = encodedFragment?.let { "#$it" }.orEmpty()
        val port = if (port >= 0) ":$port" else ""
        return "$scheme://${host.orEmpty()}$port$path$fragment"
    }

    private fun buildDeeplinkParams(
        spec: DeeplinkSpec,
        deeplink: Uri
    ): DeeplinkParams? {
        val params = mutableMapOf<String, String?>()
        val pathSegments = deeplink.pathSegments
        val fragment = deeplink.fragment

        // Extract typed path params.
        spec.pathParams.forEachIndexed { index, param ->
            if (index >= pathSegments.size) return@forEachIndexed
            val value = pathSegments[index]
            if (param.type != null) {
                params[param.name.lowercase()] = value
            }
        }

        // Extract query params.
        spec.queryParams.forEach { param ->
            val value = deeplink.getQueryParameter(param.name)
            params[param.name.lowercase()] = value
        }

        // Extract fragment when declared in spec.
        spec.fragment?.let { params["fragment"] = fragment }

        return spec.paramsFactory?.invoke(params)
    }
}
