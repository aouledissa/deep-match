package com.aouledissa.deepmatch.sample

import android.app.Activity
import android.widget.Toast
import com.aouledissa.deepmatch.processor.DeeplinkHandler
import com.aouledissa.deepmatch.sample.deeplinks.OpenMoviesDeeplinkParams

object OpenMoviesDeeplinkHandler: DeeplinkHandler<OpenMoviesDeeplinkParams> {

    override fun handle(
        activity: Activity,
        params: OpenMoviesDeeplinkParams?
    ) {
        Toast.makeText(activity, "user profile deeplink received!", Toast.LENGTH_SHORT).show()
    }
}