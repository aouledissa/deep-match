package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class DeeplinkProcessorTest {

    @Test
    fun `match returns params for the matching spec`() {
        val matchingSpec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
            fragment = "trailer-tab",
            parametersClass = SeriesParams::class
        )
        val otherSpec = matchingSpec.copy(host = setOf("other.com"))

        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "example.com"
        every { uri.pathSegments } returns listOf("series", "42")
        every { uri.query } returns "ref=promo"
        every { uri.fragment } returns "trailer-tab"
        every { uri.getQueryParameter("ref") } returns "promo"
        every { uri.getQueryParameter(any()) } answers {
            if (firstArg<String>() == "ref") "promo" else null
        }

        val processor = DeeplinkProcessor(specs = setOf(matchingSpec, otherSpec))

        val params = processor.match(uri) as? SeriesParams
        assertThat(params).isNotNull()
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.ref).isEqualTo("promo")
        assertThat(params?.fragment).isEqualTo("trailer-tab")
    }

    @Test
    fun `match returns null when no spec matches`() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )

        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "other.com"
        every { uri.pathSegments } returns emptyList()
        every { uri.query } returns null
        every { uri.fragment } returns null
        every { uri.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(uri)
        assertThat(params).isNull()
    }

    @Test
    fun `match returns null when spec has no parameters class`() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )

        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "example.com"
        every { uri.pathSegments } returns emptyList()
        every { uri.query } returns null
        every { uri.fragment } returns null
        every { uri.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(uri)
        assertThat(params).isNull()
    }

    @Test
    fun `match returns params instance for static spec with empty params class`() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(Param(name = "home")),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = HomeParams::class
        )

        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "example.com"
        every { uri.pathSegments } returns listOf("home")
        every { uri.query } returns null
        every { uri.fragment } returns null
        every { uri.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(uri)
        assertThat(params).isInstanceOf(HomeParams::class.java)
    }

    data class SeriesParams(
        val seriesId: Int,
        val ref: String?,
        val fragment: String?
    ) : DeeplinkParams

    class HomeParams : DeeplinkParams
}
