package com.aouledissa.deepmatch.processor

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.aouledissa.deepmatch.processor.DeeplinkProcessor.Builder
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class DeeplinkProcessorInstrumentationTest {

    @Test
    fun deeplinkWithTypedParameters_invokesHandlerWithParsedValues() {
        val latch = CountDownLatch(1)
        val capturedParams = AtomicReference<SeriesParams?>()

        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = linkedSetOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
            fragment = "details",
            parametersClass = SeriesParams::class
        )

        val handler = object : DeeplinkHandler<SeriesParams> {
            override fun handle(activity: android.app.Activity, params: SeriesParams?) {
                capturedParams.set(params)
                latch.countDown()
            }
        }

        val processor = Builder()
            .register(spec, handler)
            .build()

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val uri = Uri.parse("app://example.com/series/42?ref=promo#details")
                processor.match(uri, activity)
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        val params = capturedParams.get()
        assertThat(params).isNotNull()
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.ref).isEqualTo("promo")
        assertThat(params?.fragment).isEqualTo("details")
    }

    @Test
    fun nonMatchingDeeplink_doesNotInvokeHandler() {
        val latch = CountDownLatch(1)
        val invocationCount = AtomicInteger(0)

        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )

        val handler = object : DeeplinkHandler<DeeplinkParams> {
            override fun handle(activity: android.app.Activity, params: DeeplinkParams?) {
                invocationCount.incrementAndGet()
                latch.countDown()
            }
        }

        val processor = Builder()
            .register(spec, handler)
            .build()

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                processor.match(Uri.parse("app://other.com"), activity)
            }
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isFalse()
        assertThat(invocationCount.get()).isEqualTo(0)
    }

    data class SeriesParams(
        val seriesId: Int,
        val ref: String?,
        val fragment: String?
    ) : DeeplinkParams
}
