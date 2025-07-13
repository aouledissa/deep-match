package com.aouledissa.deepmatch.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class DeeplinkSpec(
    val scheme: String,
    val host: String,
    val pathParams: Set<Param>,
    val queryParams: Set<Param>,
    val fragment: String?,
    val parametersClass: KClass<out DeeplinkParams>?
) {
    val matcher: Regex by lazy { buildMatcher() }

    private fun buildMatcher(): Regex {
        val pathSegments = pathParams.joinToString("/") { param ->
            when (param.type) {
                null -> param.name
                else -> param.type.regex.pattern
            }
        }.apply { Regex.escape(this) }
        val query = when {
            queryParams.isEmpty() -> ""
            else -> {
                queryParams.filter { it.type != null }
                    .joinToString(prefix = Regex.escape("?"), separator = "&") {
                        "${it.name}=${it.type!!.regex.pattern}"
                    }
            }
        }.apply { Regex.escape(this) }
        val fragment = fragment?.let { Regex.escape("#$it") }.orEmpty()
        return "$scheme://${Regex.escape(host)}/$pathSegments$query$fragment".toRegex()
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