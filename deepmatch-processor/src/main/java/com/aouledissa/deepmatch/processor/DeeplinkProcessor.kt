package com.aouledissa.deepmatch.processor

import android.app.Activity
import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.processor.internal.DeeplinkProcessorImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Coordinates deeplink resolution by matching incoming URIs against registered
 * specs and delegating to their handlers.
 */
interface DeeplinkProcessor {
    /**
     * Attempts to match [deeplink] and, when successful, dispatches the
     * associated handler on the main thread.
     */
    fun match(deeplink: Uri, activity: Activity)

    /** Builder used to compose a processor instance at runtime. */
    class Builder {
        private val registry: HashMap<DeeplinkSpec, DeeplinkHandler<out DeeplinkParams>> =
            hashMapOf()

        /**
         * Registers a [deeplinkSpec] with its [handler]. Duplicate specs are
         * ignored so the first registration wins.
         */
        fun <T : DeeplinkParams> register(
            deeplinkSpec: DeeplinkSpec,
            handler: DeeplinkHandler<out T>
        ): Builder {
            if (registry.contains(deeplinkSpec).not())
                registry[deeplinkSpec] = handler
            return this
        }

        /** Builds the processor using an isolated IO coroutine scope. */
        fun build(): DeeplinkProcessor {
            val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            return DeeplinkProcessorImpl(
                registry = registry,
                coroutineScope = coroutineScope,
            )
        }
    }
}
