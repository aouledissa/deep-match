package com.aouledissa.deepmatch.sample

import android.app.Activity
import android.widget.Toast
import com.aouledissa.deepmatch.processor.DeeplinkHandler
import com.aouledissa.deepmatch.sample.deeplinks.OpenSeriesDeeplinkParams

object OpenSeriesDeeplinkHandler : DeeplinkHandler<OpenSeriesDeeplinkParams> {
    override fun handle(
        activity: Activity,
        params: OpenSeriesDeeplinkParams?
    ) {
        Toast.makeText(
            activity,
            "requesting series with id: ${params?.seriesId}",
            Toast.LENGTH_SHORT
        ).show()
    }
}