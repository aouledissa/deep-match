package com.aouledissa.deepmatch.processor

data class DeeplinkSpec(
    val name: String,
    val description: String,
    val scheme: String,
    val host: String,
    val pathParams: Set<Param>,
    val queryParams: Set<Param>,
    val fragment: String?
)

data class Param(
    val name: String,
    val type: ParamType,
    val required: Boolean
)

enum class ParamType {
    ALPHANUMERIC,
    NUMERIC,
    STRING,
}