package com.aouledissa.deepmatch.processor.internal

import android.app.Activity
import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.processor.DeeplinkHandler
import com.aouledissa.deepmatch.processor.DeeplinkProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeeplinkProcessorImpl(
    private val registry: HashMap<DeeplinkSpec, DeeplinkHandler<out DeeplinkParams>>,
    private val coroutineScope: CoroutineScope,
) : DeeplinkProcessor {

    override fun match(deeplink: Uri, activity: Activity) {
        coroutineScope.launch {
            val spec = registry.keys.find { it.matcher.matches(deeplink.decoded()) }
            spec?.let {
                val handler = registry[spec] as? DeeplinkHandler<DeeplinkParams>
                val params = buildDeeplinkParams(spec, deeplink)
                withContext(Dispatchers.Main) {
                    handler?.handle(activity = activity, params = params)
                }
            }
        }
    }

    private fun Uri.decoded(): String {
        val decodedPathSegments = pathSegments.joinToString("/")
        val decodedQuery = query?.let { "?$it" }.orEmpty()
        val decodedFragment = fragment?.let { "#$it" }.orEmpty()
        return "$scheme://$host/$decodedPathSegments$decodedQuery$decodedFragment"
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
        spec.fragment?.let { params["fragment"] = it }

        return spec.parametersClass?.let {
            DeeplinkParamsAutoFactory.tryCreate(spec.parametersClass, params)
        }
    }
}