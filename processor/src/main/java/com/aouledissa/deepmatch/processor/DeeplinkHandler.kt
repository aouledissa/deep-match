package com.aouledissa.deepmatch.processor

import com.aouledissa.deepmatch.api.DeeplinkParams

interface DeeplinkHandler<T : DeeplinkParams> {
    fun handle(params: T?)
}