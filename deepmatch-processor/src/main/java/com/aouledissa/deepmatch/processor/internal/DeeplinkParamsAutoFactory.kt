package com.aouledissa.deepmatch.processor.internal

import com.aouledissa.deepmatch.api.DeeplinkParams
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

internal object DeeplinkParamsAutoFactory {

    fun tryCreate(
        parametersClass: KClass<out DeeplinkParams>?,
        params: Map<String, String?>
    ): DeeplinkParams? {
        val constructor = parametersClass?.primaryConstructor ?: return null
        val args = constructor.parameters.associateWith { kParameter ->
            val type = kParameter.type.classifier as KClass<*>
            val value = params[kParameter.name?.lowercase()]?.let { convertValue(type, it) }
            value
        }
        return constructor.callBy(args)
    }

    private fun convertValue(
        type: KClass<*>?,
        value: String
    ): Any {
        return when (type) {
            Int::class -> value.toInt()
            Double::class -> value.toDouble()
            Boolean::class -> value.toBoolean()
            String::class -> value
            else -> throw IllegalArgumentException("Unsupported parameter type $type")
        }
    }
}