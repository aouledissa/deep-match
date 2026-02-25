package com.aouledissa.deepmatch.processor.internal

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.processor.DeeplinkProcessor

/**
 * Default implementation backing [DeeplinkProcessor]. It performs regex-based
 * matching and returns decoded parameters for the first matching spec.
 */
internal class DeeplinkProcessorImpl(
    private val registry: Set<DeeplinkSpec>,
) : DeeplinkProcessor {

    override fun match(deeplink: Uri): DeeplinkParams? {
        val spec = registry.find { it.matcher.matches(deeplink.decoded()) }
            ?: return null
        return buildDeeplinkParams(spec, deeplink)
    }

    private fun Uri.decoded(): String {
        val decodedPathSegments = pathSegments.joinToString("/")
            .let { if (pathSegments.isNullOrEmpty().not()) "/$it" else it }
        val decodedQuery = query?.let { "?$it" }.orEmpty()
        val decodedFragment = fragment?.let { "#$it" }.orEmpty()
        return "$scheme://$host$decodedPathSegments$decodedQuery$decodedFragment"
    }

    private fun buildDeeplinkParams(
        spec: DeeplinkSpec,
        deeplink: Uri
    ): DeeplinkParams? {
        val params = mutableMapOf<String, String?>()
        val pathSegments = deeplink.pathSegments
        val fragment = deeplink.fragment

        // extract path params
        spec.pathParams.forEachIndexed { index, param ->
            val value = pathSegments[index]
            if (param.type != null) {
                params[param.name.lowercase()] = value
            }
        }

        // extract query params
        spec.queryParams.forEach { param ->
            val value = deeplink.getQueryParameter(param.name)
            params[param.name.lowercase()] = value
        }

        // extract fragment
        spec.fragment?.let { params["fragment"] = fragment }

        return spec.parametersClass?.let {
            DeeplinkParamsAutoFactory.tryCreate(spec.parametersClass, params)
        }
    }
}
