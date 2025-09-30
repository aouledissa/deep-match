package com.aouledissa.deepmatch.processor.internal

import android.app.Activity
import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.aouledissa.deepmatch.processor.DeeplinkHandler
import com.aouledissa.deepmatch.processor.FakeDeeplinkHandler
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeeplinkProcessorImplTest {

    private val activity = mockk<Activity>(relaxed = true)

    @Test
    fun `match invokes only the handler of the matching spec`() = runProcessorTest {
        val matchingSpec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val otherSpec = matchingSpec.copy(host = setOf("other.com"))

        val matchingHandler = FakeDeeplinkHandler<DeeplinkParams>()
        val otherHandler = FakeDeeplinkHandler<DeeplinkParams>()

        val matchingUri = mockk<Uri>(relaxed = true)
        every { matchingUri.scheme } returns "app"
        every { matchingUri.host } returns "example.com"
        every { matchingUri.pathSegments } returns emptyList()
        every { matchingUri.query } returns null
        every { matchingUri.fragment } returns null
        every { matchingUri.getQueryParameter(any()) } returns null

        @Suppress("UNCHECKED_CAST")
        val processor = DeeplinkProcessorImpl(
            registry = hashMapOf(
                matchingSpec to matchingHandler as DeeplinkHandler<out DeeplinkParams>,
                otherSpec to otherHandler as DeeplinkHandler<out DeeplinkParams>
            ),
            coroutineScope = this
        )

        processor.match(matchingUri, activity)
        advanceUntilIdle()

        assertThat(matchingHandler.isInvoked(1)).isTrue()
        assertThat(otherHandler.isInvoked()).isFalse()
    }

    @Test
    fun `match builds DeeplinkParams with typed path query and fragment values`() =
        runProcessorTest {
            val spec = DeeplinkSpec(
                scheme = setOf("app"),
                host = setOf("example.com"),
                pathParams = linkedSetOf(
                    Param(name = "series"),
                    Param(name = "seriesId", type = ParamType.NUMERIC)
                ),
                queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
                fragment = "trailer-tab",
                parametersClass = SeriesParams::class
            )
            val handler = FakeDeeplinkHandler<SeriesParams>()

            @Suppress("UNCHECKED_CAST")
            val deeplink = mockk<Uri>(relaxed = true)
            every { deeplink.scheme } returns "app"
            every { deeplink.host } returns "example.com"
            every { deeplink.pathSegments } returns listOf("series", "42")
            every { deeplink.query } returns "ref=promo"
            every { deeplink.fragment } returns "trailer-tab"
            every { deeplink.getQueryParameter("ref") } returns "promo"
            every { deeplink.getQueryParameter(any()) } answers {
                if (firstArg<String>() == "ref") "promo" else null
            }

            @Suppress("UNCHECKED_CAST")
            val processor = DeeplinkProcessorImpl(
                registry = hashMapOf(spec to handler as DeeplinkHandler<out DeeplinkParams>),
                coroutineScope = this
            )

            processor.match(deeplink, activity)
            advanceUntilIdle()

            val params = handler.lastParams()
            assertThat(handler.isInvoked(1)).isTrue()
            assertThat(params?.seriesId).isEqualTo(42)
            assertThat(params?.ref).isEqualTo("promo")
            assertThat(params?.fragment).isEqualTo("trailer-tab")
        }

    @Test
    fun `match does nothing when no spec matches`() = runProcessorTest {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val handler = FakeDeeplinkHandler<DeeplinkParams>()

        @Suppress("UNCHECKED_CAST")
        val deeplink = mockk<Uri>(relaxed = true)
        every { deeplink.scheme } returns "app"
        every { deeplink.host } returns "other.com"
        every { deeplink.pathSegments } returns emptyList()
        every { deeplink.query } returns null
        every { deeplink.fragment } returns null
        every { deeplink.getQueryParameter(any()) } returns null

        @Suppress("UNCHECKED_CAST")
        val processor = DeeplinkProcessorImpl(
            registry = hashMapOf(spec to handler as DeeplinkHandler<out DeeplinkParams>),
            coroutineScope = this
        )

        processor.match(deeplink, activity)
        advanceUntilIdle()

        assertThat(handler.isInvoked()).isFalse()
    }

    private inline fun runProcessorTest(crossinline block: suspend TestScope.() -> Unit) =
        runTest {
            val mainDispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(mainDispatcher)
            try {
                block()
            } finally {
                Dispatchers.resetMain()
            }
        }

    data class SeriesParams(
        val seriesId: Int,
        val ref: String?,
        val fragment: String?
    ) : DeeplinkParams
}
