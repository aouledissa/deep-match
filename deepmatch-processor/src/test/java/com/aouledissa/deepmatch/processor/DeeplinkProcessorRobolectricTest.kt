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
}
