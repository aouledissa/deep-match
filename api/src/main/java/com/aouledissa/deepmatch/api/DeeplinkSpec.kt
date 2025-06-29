package com.aouledissa.deepmatch.api

import kotlinx.serialization.Serializable

@Serializable
data class DeeplinkSpec(
    val scheme: String,
    val host: String,
//    val pathParams: Set<Param>,
//    val queryParams: Set<Param>,
//    val fragment: String?,
)

//@Serializable
//internal data class Param(
//    val name: String,
//    val type: ParamType,
//    val required: Boolean
//)
//
//@Serializable
//internal enum class ParamType {
//    @SerialName("alphanumeric")
//    ALPHANUMERIC,
//
//    @SerialName("numeric")
//    NUMERIC,
//
//    @SerialName("string")
//    STRING;
//
//    fun getType(): KClass<*> {
//        return when (this) {
//            NUMERIC -> Int::class
//            STRING, ALPHANUMERIC -> String::class
//        }
//    }
//}