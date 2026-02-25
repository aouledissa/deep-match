package com.aouledissa.deepmatch.processor.internal

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class DeeplinkProcessorImplTest {

    @Test
    fun `match returns params for the matching spec`() {
        val matchingSpec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val otherSpec = matchingSpec.copy(host = setOf("other.com"))

        val matchingUri = mockk<Uri>(relaxed = true)
        every { matchingUri.scheme } returns "app"
        every { matchingUri.host } returns "example.com"
        every { matchingUri.pathSegments } returns emptyList()
        every { matchingUri.query } returns null
        every { matchingUri.fragment } returns null
        every { matchingUri.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessorImpl(
            registry = setOf(matchingSpec, otherSpec)
        )

        val params = processor.match(matchingUri)
        assertThat(params).isNull()
    }

    @Test
    fun `match builds DeeplinkParams with typed path query and fragment values`() {
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

            val processor = DeeplinkProcessorImpl(
                registry = setOf(spec)
            )

            val params = processor.match(deeplink) as SeriesParams?
            assertThat(params?.seriesId).isEqualTo(42)
            assertThat(params?.ref).isEqualTo("promo")
            assertThat(params?.fragment).isEqualTo("trailer-tab")
    }

    @Test
    fun `match returns null when no spec matches`() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )

        @Suppress("UNCHECKED_CAST")
        val deeplink = mockk<Uri>(relaxed = true)
        every { deeplink.scheme } returns "app"
        every { deeplink.host } returns "other.com"
        every { deeplink.pathSegments } returns emptyList()
        every { deeplink.query } returns null
        every { deeplink.fragment } returns null
        every { deeplink.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessorImpl(
            registry = setOf(spec)
        )

        val params = processor.match(deeplink)
        assertThat(params).isNull()
    }

    data class SeriesParams(
        val seriesId: Int,
        val ref: String?,
        val fragment: String?
    ) : DeeplinkParams
}
