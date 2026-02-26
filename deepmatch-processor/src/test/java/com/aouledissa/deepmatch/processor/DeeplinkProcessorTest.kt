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
            name = "open series",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
            fragment = "trailer-tab",
            paramsFactory = ::seriesFactory
        )
        val otherSpec = matchingSpec.copy(name = "other", host = setOf("other.com"))

        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "example.com"
        every { uri.port } returns -1
        every { uri.pathSegments } returns listOf("series", "42")
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
            name = "open series",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = null
        )

        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "other.com"
        every { uri.port } returns -1
        every { uri.pathSegments } returns emptyList()
        every { uri.fragment } returns null
        every { uri.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(uri)).isNull()
    }

    @Test
    fun `match returns null when URI path has fewer segments than spec`() {
        val spec = DeeplinkSpec(
            name = "open profile",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "profile"),
                Param(name = "userId", type = ParamType.NUMERIC)
            ),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = ::numericProfileFactory
        )

        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "example.com"
        every { uri.port } returns -1
        every { uri.pathSegments } returns listOf("profile")
        every { uri.fragment } returns null
        every { uri.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(uri)).isNull()
    }

    @Test
    fun `match returns null when runtime exception occurs`() {
        val spec = DeeplinkSpec(
            name = "broken",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(Param(name = "home")),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = { HomeParams() }
        )
        val uri = mockk<Uri>(relaxed = true)
        every { uri.scheme } returns "app"
        every { uri.host } returns "example.com"
        every { uri.port } returns -1
        every { uri.pathSegments } throws IllegalStateException("boom")
        every { uri.fragment } returns null
        every { uri.getQueryParameter(any()) } returns null

        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(uri)).isNull()
    }

    data class SeriesParams(
        val seriesId: Int,
        val ref: String?,
        val fragment: String?
    ) : DeeplinkParams

    data class ProfileParams(
        val userId: String
    ) : DeeplinkParams

    data class NumericProfileParams(
        val userId: Int
    ) : DeeplinkParams

    class HomeParams : DeeplinkParams

    private fun seriesFactory(params: Map<String, String?>): DeeplinkParams? = try {
        SeriesParams(
            seriesId = params["seriesid"]!!.toInt(),
            ref = params["ref"],
            fragment = params["fragment"]
        )
    } catch (e: Exception) {
        null
    }

    private fun profileFactory(params: Map<String, String?>): DeeplinkParams? = try {
        ProfileParams(userId = params["userid"]!!)
    } catch (e: Exception) {
        null
    }

    private fun numericProfileFactory(params: Map<String, String?>): DeeplinkParams? = try {
        NumericProfileParams(userId = params["userid"]!!.toInt())
    } catch (e: Exception) {
        null
    }
}
