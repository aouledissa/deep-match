package com.aouledissa.deepmatch.processor

import android.app.Activity
import com.aouledissa.deepmatch.api.DeeplinkParams

class FakeDeeplinkHandler<T : DeeplinkParams> : DeeplinkHandler<T> {
    private var invocationCount = 0
    private var lastParams: T? = null

    override fun handle(activity: Activity, params: T?) {
        invocationCount++
        @Suppress("UNCHECKED_CAST")
        lastParams = params as T?
    }

    fun isInvoked() = invocationCount > 0

    fun isInvoked(times: Int) = invocationCount == times

    fun lastParams(): T? = lastParams
}
