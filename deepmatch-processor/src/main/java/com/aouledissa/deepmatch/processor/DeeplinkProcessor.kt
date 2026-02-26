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
            val decoded = deeplink.decoded() ?: return null

            for (spec in specs) {
                val regexMatch = spec.matcher.matches(decoded)
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
        } catch (error: Exception) {
            null
        }
    }

    private fun Uri.decoded(): String? {
        val decodedScheme = scheme ?: return null
        val decodedPathSegments = pathSegments.joinToString("/")
            .trimEnd('/')
            .let { if (pathSegments.isNullOrEmpty().not()) "/$it" else it }
        val decodedFragment = fragment?.let { "#$it" }.orEmpty()
        val decodedPort = if (port >= 0) ":$port" else ""
        return "$decodedScheme://${host.orEmpty()}$decodedPort$decodedPathSegments$decodedFragment"
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
