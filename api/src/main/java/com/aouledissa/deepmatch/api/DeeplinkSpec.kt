package com.aouledissa.deepmatch.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class DeeplinkSpec(
    val scheme: String,
    val host: String,
    val pathParams: Set<Param>,
    val queryParams: Set<Param.TemplateParam>,
    val fragment: String?,
)

@Serializable
sealed class Param {
    @SerialName("definedParam")
    @Serializable
    data class DefinedParam(val key: String) : Param() {
        override fun toString(): String {
            return "DefinedParam(key = \"$key\")"
        }
    }

    @SerialName("templateParam")
    @Serializable
    data class TemplateParam(val key: String, val type: ParamType) : Param() {
        override fun toString(): String {
            return "TemplateParam(key = \"$key\", type = ${ParamType::class.simpleName}.${type.name})"
        }
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