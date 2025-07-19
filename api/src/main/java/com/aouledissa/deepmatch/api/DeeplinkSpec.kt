package com.aouledissa.deepmatch.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class DeeplinkSpec(
    val scheme: String,
    val host: Set<String>,
    val pathParams: Set<Param>,
    val queryParams: Set<Param>,
    val fragment: String?,
    val parametersClass: KClass<out DeeplinkParams>?
) {
    val matcher: Regex by lazy { buildMatcher() }

    private fun buildMatcher(): Regex {
        val hostPattern = buildHostPattern()
        val pathPattern = buildPathPattern()
        val queryPattern = buildQueryPattern()
        val fragmentPattern = buildFragmentPattern()

        return "$scheme://$hostPattern$pathPattern$queryPattern$fragmentPattern".toRegex()
    }

    private fun buildHostPattern(): String {
        return host.joinToString(separator = "|") { Regex.escape(it) }
            .let { if (host.size > 1) "($it)" else it }
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

    private fun buildQueryPattern(): String {
        return when {
            queryParams.isEmpty() -> ""
            else -> {
                queryParams.filter { it.type != null }
                    .joinToString(prefix = Regex.escape("?"), separator = "&") {
                        "${Regex.escape(it.name)}=${it.type!!.regex.pattern}"
                    }
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

@Serializable
data class Param(
    val name: String,
    val type: ParamType? = null
) {
    override fun toString(): String {
        return "Param(name = \"$name\", type = ${
            when (this.type) {
                null -> null
                else -> "${ParamType::class.simpleName}.${type.name}"
            }
        })"
    }
}

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