package com.aouledissa.deepmatch.processor

import android.app.Activity
import android.net.Uri
import android.os.Looper
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.aouledissa.deepmatch.processor.internal.DeeplinkProcessorImpl
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class DeeplinkProcessorRobolectricTest {

    private lateinit var dispatcher: TestDispatcher
    private lateinit var scope: TestScope

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        scope = TestScope(dispatcher)
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun deeplinkWithTypedParameters_invokesHandlerWithParsedValues() {
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
        val handler = FakeDeeplinkHandler<SeriesParams>()

        val processor = DeeplinkProcessorImpl(
            registry = hashMapOf(spec to handler as DeeplinkHandler<out DeeplinkParams>),
            coroutineScope = scope
        )

        withActivity { activity ->
            val uri = Uri.parse("app://example.com/series/42?ref=promo#details")
            processor.match(uri, activity)
        }

        scope.advanceUntilIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val params = handler.lastParams()
        assertThat(handler.isInvoked(1)).isTrue()
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.ref).isEqualTo("promo")
        assertThat(params?.fragment).isEqualTo("details")
    }

    @Test
    fun nonMatchingDeeplink_doesNotInvokeHandler() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val handler = FakeDeeplinkHandler<DeeplinkParams>()

        val processor = DeeplinkProcessorImpl(
            registry = hashMapOf(spec to handler as DeeplinkHandler<out DeeplinkParams>),
            coroutineScope = scope
        )

        withActivity { activity ->
            processor.match(Uri.parse("app://other.com"), activity)
        }

        scope.advanceUntilIdle()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertThat(handler.isInvoked()).isFalse()
    }

    private inline fun withActivity(block: (Activity) -> Unit) {
        val controller = Robolectric.buildActivity(TestActivity::class.java).setup()
        try {
            block(controller.get())
        } finally {
            controller.pause().destroy()
        }
    }

    data class SeriesParams(
        val seriesId: Int,
        val ref: String?,
        val fragment: String?
    ) : DeeplinkParams
}

private class TestActivity : Activity()
