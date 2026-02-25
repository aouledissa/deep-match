package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams

class FakeDeeplinkProcessor : DeeplinkProcessor(specs = emptySet()) {
    private var resultProvider: (Uri) -> DeeplinkParams? = { null }
    private var lastDeeplink: Uri? = null

    fun configure(resultProvider: (Uri) -> DeeplinkParams?) {
        this.resultProvider = resultProvider
    }

    override fun match(deeplink: Uri): DeeplinkParams? {
        lastDeeplink = deeplink
        return resultProvider(deeplink)
    }

    fun isInvoked() = lastDeeplink != null
}
