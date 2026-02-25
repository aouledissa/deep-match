package com.aouledissa.deepmatch.api

import kotlin.reflect.KClass

object DeeplinkSpecFixtures {

    fun create(
        scheme: Set<String> = setOf(),
        host: Set<String> = setOf(),
        pathParams: List<Param> = emptyList(),
        queryParams: Set<Param> = setOf(),
        fragment: String? = null,
        parametersClass: KClass<out DeeplinkParams>? = null
    ): DeeplinkSpec {
        return DeeplinkSpec(
            scheme = scheme,
            host = host,
            pathParams = pathParams,
            queryParams = queryParams,
            fragment = fragment,
            parametersClass = parametersClass
        )
    }
}