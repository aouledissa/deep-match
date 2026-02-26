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
