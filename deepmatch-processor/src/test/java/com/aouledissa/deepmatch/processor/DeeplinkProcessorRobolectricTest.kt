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
            name = "open series",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
            fragment = "details",
            paramsFactory = ::seriesFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(Uri.parse("app://example.com/series/42?ref=promo#details")) as SeriesParams?
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.ref).isEqualTo("promo")
        assertThat(params?.fragment).isEqualTo("details")
    }

    @Test
    fun nonMatchingDeeplink_returnsNull() {
        val spec = DeeplinkSpec(
            name = "open home",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = null
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(Uri.parse("app://other.com"))).isNull()
    }

    @Test
    fun deeplinkWithOutOfOrderQueryParams_matchesAndReturnsParsedValues() {
        val spec = DeeplinkSpec(
            name = "open paged series",
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
            paramsFactory = ::pagedSeriesFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(Uri.parse("app://example.com/series/42?page=1&ref=promo")) as PagedSeriesParams?
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.page).isEqualTo(1)
        assertThat(params?.ref).isEqualTo("promo")
    }

    @Test
    fun deeplinkMatchesWhenOptionalQueryParamIsAbsent() {
        val spec = DeeplinkSpec(
            name = "open optional ref",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING)),
            fragment = null,
            paramsFactory = ::optionalRefFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(Uri.parse("app://example.com/series/42")) as OptionalRefParams?
        assertThat(params?.seriesId).isEqualTo(42)
        assertThat(params?.ref).isNull()
    }

    @Test
    fun deeplinkDoesNotMatchWhenRequiredQueryParamIsAbsent() {
        val spec = DeeplinkSpec(
            name = "open required ref",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = setOf(Param(name = "ref", type = ParamType.STRING, required = true)),
            fragment = null,
            paramsFactory = ::requiredRefFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(Uri.parse("app://example.com/series/42"))).isNull()
    }

    @Test
    fun deeplinkWithDifferentPathOrder_doesNotMatch() {
        val spec = DeeplinkSpec(
            name = "series order",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = ::seriesOnlyFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(Uri.parse("app://example.com/42/series"))).isNull()
    }

    @Test
    fun staticDeeplinkMatch_returnsEmptyParamsInstance() {
        val spec = DeeplinkSpec(
            name = "open home",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(Param(name = "home")),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = { HomeParams() }
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(Uri.parse("app://example.com/home"))).isInstanceOf(HomeParams::class.java)
    }

    @Test
    fun uppercaseHttpsAndHost_matchesLowercaseSpec() {
        val spec = DeeplinkSpec(
            name = "open path",
            scheme = setOf("https"),
            host = setOf("example.com"),
            pathParams = listOf(Param(name = "path")),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = { HomeParams() }
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(Uri.parse("HTTPS://Example.COM/path"))).isInstanceOf(HomeParams::class.java)
    }

    @Test
    fun uppercaseAppAndHost_matchesTypedSpec() {
        val spec = DeeplinkSpec(
            name = "open profile",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "profile"),
                Param(name = "userId", type = ParamType.ALPHANUMERIC)
            ),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = ::profileFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(Uri.parse("App://EXAMPLE.com/profile/abc")) as ProfileParams?
        assertThat(params?.userId).isEqualTo("abc")
    }

    @Test
    fun trailingSlashAndNonTrailingSlash_matchSameSpec() {
        val spec = DeeplinkSpec(
            name = "open numeric profile",
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
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val withoutTrailingSlash = processor.match(Uri.parse("app://example.com/profile/123"))
        val withTrailingSlash = processor.match(Uri.parse("app://example.com/profile/123/"))

        assertThat(withoutTrailingSlash).isInstanceOf(NumericProfileParams::class.java)
        assertThat(withTrailingSlash).isInstanceOf(NumericProfileParams::class.java)
    }

    @Test
    fun pathParamsAreExtractedForTrailingSlashAndNonTrailingSlashForms() {
        val spec = DeeplinkSpec(
            name = "open numeric profile",
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
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val withoutTrailingSlash = processor.match(Uri.parse("app://example.com/profile/123")) as NumericProfileParams?
        val withTrailingSlash = processor.match(Uri.parse("app://example.com/profile/123/")) as NumericProfileParams?

        assertThat(withoutTrailingSlash?.userId).isEqualTo(123)
        assertThat(withTrailingSlash?.userId).isEqualTo(123)
    }

    @Test
    fun hostlessDeeplink_matchesAndExtractsPathParams() {
        val spec = DeeplinkSpec(
            name = "hostless profile",
            scheme = setOf("app"),
            host = emptySet(),
            pathParams = listOf(
                Param(name = "profile"),
                Param(name = "profileId", type = ParamType.NUMERIC)
            ),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = ::hostlessProfileFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val params = processor.match(Uri.parse("app:///profile/123")) as HostlessProfileParams?
        assertThat(params?.profileId).isEqualTo(123)
    }

    @Test
    fun deeplinkWithPort_matchesOnlyWhenPortMatchesSpec() {
        val spec = DeeplinkSpec(
            name = "port profile",
            scheme = setOf("https"),
            host = setOf("example.com"),
            port = 8080,
            pathParams = listOf(Param(name = "profile")),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = { HomeParams() }
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        val matching = processor.match(Uri.parse("https://example.com:8080/profile"))
        val nonMatching = processor.match(Uri.parse("https://example.com/profile"))

        assertThat(matching).isInstanceOf(HomeParams::class.java)
        assertThat(nonMatching).isNull()
    }

    @Test
    fun emptyUri_returnsNullWithoutCrash() {
        val processor = DeeplinkProcessor(specs = emptySet())
        assertThat(processor.match(Uri.EMPTY)).isNull()
    }

    @Test
    fun blankUri_returnsNullWithoutCrash() {
        val processor = DeeplinkProcessor(specs = emptySet())
        assertThat(processor.match(Uri.parse(""))).isNull()
    }

    @Test
    fun uriWithoutScheme_returnsNullWithoutCrash() {
        val spec = DeeplinkSpec(
            name = "open profile",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "profile"),
                Param(name = "userId", type = ParamType.ALPHANUMERIC)
            ),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = ::profileFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(Uri.parse("//example.com/profile/abc"))).isNull()
    }

    @Test
    fun malformedNumericValue_returnsNullWithoutCrash() {
        val spec = DeeplinkSpec(
            name = "open series",
            scheme = setOf("app"),
            host = setOf("example.com"),
            pathParams = listOf(
                Param(name = "series"),
                Param(name = "seriesId", type = ParamType.NUMERIC)
            ),
            queryParams = emptySet(),
            fragment = null,
            paramsFactory = ::seriesOnlyFactory
        )
        val processor = DeeplinkProcessor(specs = setOf(spec))

        assertThat(processor.match(Uri.parse("app://example.com/series/abc"))).isNull()
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

    data class ProfileParams(
        val userId: String
    ) : DeeplinkParams

    data class NumericProfileParams(
        val userId: Int
    ) : DeeplinkParams

    data class HostlessProfileParams(
        val profileId: Int
    ) : DeeplinkParams

    private fun seriesFactory(params: Map<String, String?>): DeeplinkParams? = try {
        SeriesParams(
            seriesId = params["seriesid"]!!.toInt(),
            ref = params["ref"],
            fragment = params["fragment"]
        )
    } catch (e: Exception) {
        null
    }

    private fun pagedSeriesFactory(params: Map<String, String?>): DeeplinkParams? = try {
        PagedSeriesParams(
            seriesId = params["seriesid"]!!.toInt(),
            ref = params["ref"],
            page = params["page"]?.toInt()
        )
    } catch (e: Exception) {
        null
    }

    private fun seriesOnlyFactory(params: Map<String, String?>): DeeplinkParams? = try {
        SeriesOnlyParams(seriesId = params["seriesid"]!!.toInt())
    } catch (e: Exception) {
        null
    }

    private fun optionalRefFactory(params: Map<String, String?>): DeeplinkParams? = try {
        OptionalRefParams(
            seriesId = params["seriesid"]!!.toInt(),
            ref = params["ref"]
        )
    } catch (e: Exception) {
        null
    }

    private fun requiredRefFactory(params: Map<String, String?>): DeeplinkParams? = try {
        RequiredRefParams(
            seriesId = params["seriesid"]!!.toInt(),
            ref = params["ref"]!!
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

    private fun hostlessProfileFactory(params: Map<String, String?>): DeeplinkParams? = try {
        HostlessProfileParams(profileId = params["profileid"]!!.toInt())
    } catch (e: Exception) {
        null
    }
}
