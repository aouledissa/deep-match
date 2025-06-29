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
)

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
enum class ParamType {
    @SerialName("alphanumeric")
    ALPHANUMERIC,

    @SerialName("numeric")
    NUMERIC,

    @SerialName("string")
    STRING;

    fun getType(): KClass<*> {
        return when (this) {
            NUMERIC -> Int::class
            STRING, ALPHANUMERIC -> String::class
        }
    }
}