package com.aouledissa.deepmatch.processor

import android.app.Activity
import com.aouledissa.deepmatch.api.DeeplinkParams

/**
 * Callback invoked when a deeplink matches. Implementations should perform the
 * UI navigation or business logic associated with the deeplink.
 *
 * @param T type of parameters emitted by the processor. Generated specs that
 * declare typed values expose a concrete [DeeplinkParams] implementation.
 */
interface DeeplinkHandler<T : DeeplinkParams> {
    /**
     * Called on the main thread once the processor finishes matching and
     * building parameters for a deeplink.
     */
    fun handle(activity: Activity, params: T?)
}
