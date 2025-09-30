package com.aouledissa.deepmatch.processor

import android.app.Activity
import com.aouledissa.deepmatch.api.DeeplinkParams

interface DeeplinkHandler<T : DeeplinkParams> {
    fun handle(activity: Activity, params: T?)
}