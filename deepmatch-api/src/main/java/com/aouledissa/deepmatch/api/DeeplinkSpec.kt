package com.aouledissa.deepmatch.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Immutable description of a deeplink shape generated from the YAML spec.
 *
 * Each instance captures the allowed URI components alongside the optional
 * params factory that should be invoked when a match occurs.
 */
data class DeeplinkSpec(
    val name: String = "",
    val scheme: Set<String>,
    val host: Set<String>,
    val port: Int? = null,
    val pathParams: List<Param>,
    val queryParams: Set<Param>,
    val fragment: String?,
    val paramsFactory: ((Map<String, String?>) -> DeeplinkParams?)?
) {
    val matcher: Regex by lazy { buildMatcher() }

    fun matchesQueryParams(queryParamResolver: (String) -> String?): Boolean {
        if (queryParams.isEmpty()) return true
        return queryParams.filter { it.type != null }.all { param ->
            val value = queryParamResolver(param.name)
            when {
                value != null && param.type != null -> param.type.regex.matches(value)
                param.required -> false
                else -> true
            }
        }
    }

    private fun buildMatcher(): Regex {
        val schemePattern = buildSchemePattern()
        val hostPattern = buildHostPattern()
        val portPattern = buildPortPattern()
        val pathPattern = buildPathPattern()
        val fragmentPattern = buildFragmentPattern()

        return "$schemePattern://$hostPattern$portPattern$pathPattern$fragmentPattern"
            .toRegex(RegexOption.IGNORE_CASE)
    }

    private fun buildSchemePattern(): String {
        return scheme.joinToString(separator = "|") { Regex.escape(it) }
            .let { if (scheme.size > 1) "($it)" else it }
    }

    private fun buildHostPattern(): String {
        if (host.isEmpty()) return ""
        return host.joinToString(separator = "|") { Regex.escape(it) }
            .let { if (host.size > 1) "($it)" else it }
    }

    private fun buildPortPattern(): String {
        return port?.let { ":${Regex.escape(it.toString())}" }.orEmpty()
    }

    private fun buildPathPattern(): String {
        return pathParams.joinToString(separator = "/") { param ->
            when (param.type) {
                null -> Regex.escape(param.name)
                else -> param.type.regex.pattern
            }
        }.let {
            when (pathParams.size) {
                0 -> ""
                else -> "/$it"
            }
        }
    }

    private fun buildFragmentPattern(): String {
        return fragment?.let { Regex.escape("#$it") }.orEmpty()
    }
}

private val alphaNumericRegex = "[a-zA-Z0-9._~-]+".toRegex()
private val numericRegex = "[0-9]+".toRegex()
private val stringRegex = "[a-zA-Z._~-]+".toRegex()

/**
 * Definition of a single path or query parameter declared in a deeplink
 * configuration.
 */
@Serializable
data class Param(
    val name: String,
    val type: ParamType? = null,
    val required: Boolean = false
) {
    override fun toString(): String {
        return "Param(name = \"$name\", type = ${
            when (this.type) {
                null -> null
                else -> "${ParamType::class.simpleName}.${type.name}"
            }
        }, required = $required)"
    }
}

/**
 * Supported parameter types. The associated regular expression is embedded into
 * generated URI matchers as well as typed conversions during runtime.
 */
@Serializable
enum class ParamType(val regex: Regex) {
    @SerialName("alphanumeric")
    ALPHANUMERIC(regex = alphaNumericRegex),

    @SerialName("numeric")
    NUMERIC(regex = numericRegex),

    @SerialName("string")
    STRING(regex = stringRegex);

    fun getType(): KClass<*> {
        return when (this) {
            NUMERIC -> Int::class
            STRING, ALPHANUMERIC -> String::class
        }
    }
}
