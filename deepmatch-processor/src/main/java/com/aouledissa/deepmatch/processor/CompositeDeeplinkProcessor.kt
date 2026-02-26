package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams

/**
 * Composes multiple processors and returns the first successful match.
 */
open class CompositeDeeplinkProcessor(
    private val processors: List<DeeplinkProcessor>
) : DeeplinkProcessor(specs = emptySet()) {

    constructor(vararg processors: DeeplinkProcessor) : this(processors.toList())

    override fun match(deeplink: Uri): DeeplinkParams? {
        return processors.firstNotNullOfOrNull { it.match(deeplink) }
    }
}
