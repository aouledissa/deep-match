package com.aouledissa.deepmatch.sample

import android.app.Activity
import android.widget.Toast
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.processor.DeeplinkHandler

object UserProfileDeeplinkHandler : DeeplinkHandler<DeeplinkParams> {
    override fun handle(activity: Activity, params: DeeplinkParams?) {
        Toast.makeText(activity, "user profile deeplink received!", Toast.LENGTH_SHORT).show()
    }
}