package com.aouledissa.deepmatch.processor

import android.app.Activity
import android.net.Uri

class FakeDeeplinkProcessor : DeeplinkProcessor {
    override fun match(deeplink: Uri, activity: Activity) {
    }
}