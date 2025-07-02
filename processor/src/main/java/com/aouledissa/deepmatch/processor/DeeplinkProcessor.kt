package com.aouledissa.deepmatch.processor

import android.app.Activity
import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.processor.internal.DeeplinkProcessorImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface DeeplinkProcessor {
    fun match(deeplink: Uri, activity: Activity)

    class Builder {
        private val registry: HashMap<DeeplinkSpec, DeeplinkHandler<out DeeplinkParams>> =
            hashMapOf()

        fun <T : DeeplinkParams> register(
            deeplinkSpec: DeeplinkSpec,
            handler: DeeplinkHandler<out T>
        ): Builder {
            if (registry.contains(deeplinkSpec).not())
                registry[deeplinkSpec] = handler
            return this
        }

        fun build(): DeeplinkProcessor {
            val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            return DeeplinkProcessorImpl(
                registry = registry,
                coroutineScope = coroutineScope,
            )
        }
    }
}