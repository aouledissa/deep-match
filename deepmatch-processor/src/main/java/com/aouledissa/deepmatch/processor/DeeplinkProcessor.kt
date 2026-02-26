package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.processor.internal.DeeplinkParamsAutoFactory

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
        val spec = specs.find {
            it.matcher.matches(deeplink.decoded()) && it.matchesQueryParams(deeplink::getQueryParameter)
        }
            ?: return null
        return buildDeeplinkParams(spec, deeplink)
    }

    private fun Uri.decoded(): String {
        val decodedPathSegments = pathSegments.joinToString("/")
            .trimEnd('/')
            .let { if (pathSegments.isNullOrEmpty().not()) "/$it" else it }
        val decodedFragment = fragment?.let { "#$it" }.orEmpty()
        return "$scheme://${host.orEmpty()}$decodedPathSegments$decodedFragment"
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

        return spec.parametersClass?.let {
            DeeplinkParamsAutoFactory.tryCreate(spec.parametersClass, params)
        }
    }
}
