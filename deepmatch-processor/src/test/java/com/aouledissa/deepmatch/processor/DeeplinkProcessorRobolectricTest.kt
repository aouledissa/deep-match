package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeeplinkProcessorRobolectricTest {

    @Test
    fun deeplinkWithTypedParameters_returnsParsedValues() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
            fragment = "details",
            parametersClass = SeriesParams::class
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val uri = Uri.parse("app://example.com/series/42?ref=promo#details")
        val params = processor.match(uri) as SeriesParams?
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.ref).isEqualTo("promo")
        assertThat(params?.fragment).isEqualTo("details")
    }

    @Test
    fun nonMatchingDeeplink_returnsNull() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(Uri.parse("app://other.com"))
        assertThat(params).isNull()
    }

    @Test
    fun deeplinkWithOutOfOrderQueryParams_matchesAndReturnsParsedValues() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(
                Param(name = "ref", type = ParamType.STRING),
                Param(name = "page", type = ParamType.NUMERIC)
            ),
            fragment = null,
            parametersClass = PagedSeriesParams::class
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val uri = Uri.parse("app://example.com/series/42?page=1&ref=promo")
        val params = processor.match(uri) as PagedSeriesParams?
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.page).isEqualTo(1)
        assertThat(params?.ref).isEqualTo("promo")
    }

    @Test
    fun deeplinkMatchesWhenOptionalQueryParamIsAbsent() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
            fragment = null,
            parametersClass = OptionalRefParams::class
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val uri = Uri.parse("app://example.com/series/42")
        val params = processor.match(uri) as OptionalRefParams?
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.ref).isNull()
    }

    @Test
    fun deeplinkDoesNotMatchWhenRequiredQueryParamIsAbsent() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING, required = true)),
            fragment = null,
            parametersClass = RequiredRefParams::class
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val uri = Uri.parse("app://example.com/series/42")
        val params = processor.match(uri)
        assertThat(params).isNull()
    }

    @Test
    fun deeplinkWithDifferentPathOrder_doesNotMatch() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = SeriesOnlyParams::class
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val uri = Uri.parse("app://example.com/42/series")
        val params = processor.match(uri)
        assertThat(params).isNull()
    }

    @Test
    fun staticDeeplinkMatch_returnsEmptyParamsInstance() {
        val spec = DeeplinkSpec(
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(Param(name = "home")),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = HomeParams::class
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(Uri.parse("app://example.com/home"))
        assertThat(params).isInstanceOf(HomeParams::class.java)
    }

    data class SeriesParams(
        val seriesId: Int,
        val ref: String?,
        val fragment: String?
    ) : DeeplinkParams

    data class PagedSeriesParams(
        val seriesId: Int,
        val ref: String?,
        val page: Int?
    ) : DeeplinkParams

    data class SeriesOnlyParams(
        val seriesId: Int
    ) : DeeplinkParams

    data class OptionalRefParams(
        val seriesId: Int,
        val ref: String?
    ) : DeeplinkParams

    data class RequiredRefParams(
        val seriesId: Int,
        val ref: String
    ) : DeeplinkParams

    class HomeParams : DeeplinkParams
}
