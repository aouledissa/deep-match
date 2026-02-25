package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.processor.internal.DeeplinkProcessorImpl

/**
 * Coordinates deeplink resolution by matching incoming URIs against registered
 * specs and returning decoded parameters, if any.
 */
interface DeeplinkProcessor {
    /**
     * Attempts to match [deeplink] and returns the decoded parameters for the
     * first matching spec.
     */
    fun match(deeplink: Uri): DeeplinkParams?

    /** Builder used to compose a processor instance at runtime. */
    class Builder {
        private val registry: MutableSet<DeeplinkSpec> = mutableSetOf()

        /**
         * Registers a [deeplinkSpec]. Duplicate specs are ignored so the first
         * registration wins.
         */
        fun register(
            deeplinkSpec: DeeplinkSpec,
        ): Builder {
            registry.add(deeplinkSpec)
            return this
        }

        /** Builds the processor using the registered specs. */
        fun build(): DeeplinkProcessor {
            return DeeplinkProcessorImpl(registry = registry)
        }
    }
}
