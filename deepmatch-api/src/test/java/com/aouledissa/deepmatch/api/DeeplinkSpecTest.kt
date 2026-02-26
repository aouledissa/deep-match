package com.aouledissa.deepmatch.api

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeeplinkSpecTest {

    lateinit var sut: DeeplinkSpec

    @Test
    fun `deeplink pattern includes scheme and host`() {
        // given
        val scheme = "https"
        val host = "test.com"
        val expectedPattern = "${Regex.escape(scheme)}://${Regex.escape(host)}".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = setOf(scheme),
            host = setOf(host),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).isEqualTo(expectedPattern.pattern)
    }

    @Test
    fun `deeplink pattern includes multiple schemes`() {
        // given
        val scheme1 = "https"
        val scheme2 = "app"
        val expectedPattern = "(${Regex.escape(scheme1)}|${Regex.escape(scheme2)})".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = setOf(scheme1, scheme2),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(expectedPattern.pattern)
    }

    @Test
    fun `deeplink pattern includes multiple hosts`() {
        // given
        val scheme = "https"
        val host1 = "test.com"
        val host2 = "prod.com"
        val hosts = setOf(host1, host2)
        val expectedPattern =
            "${Regex.escape(scheme)}://(${Regex.escape(host1)}|${Regex.escape(host2)})".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = setOf(scheme),
            host = hosts,
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).isEqualTo(expectedPattern.pattern)
    }

    @Test
    fun `deeplink pattern includes single path parameter`() {
        // given
        val pathParam = Param(name = "userId")

        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = listOf(pathParam),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains("/${Regex.escape(pathParam.name)}")
    }

    @Test
    fun `deeplink pattern includes multiple path parameter`() {
        // given
        val pathParam1 = Param(name = "profile")
        val pathParam2 = Param(name = "bio")
        val params = listOf(pathParam1, pathParam2)
        val expectedPattern = params.joinToString(separator = "/") { Regex.escape(it.name) }

        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = params,
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(expectedPattern)
    }

    @Test
    fun `deeplink pattern includes path param template`() {
        // given
        val pathParam = Param(name = "userId", type = ParamType.ALPHANUMERIC)

        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = listOf(pathParam),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(pathParam.type!!.regex.pattern)
    }

    @Test
    fun `matchesQueryParams returns true regardless of query params order`() {
        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = setOf(
                Param(name = "ref", type = ParamType.STRING),
                Param(name = "page", type = ParamType.NUMERIC)
            ),
            fragment = null,
            parametersClass = null
        )

        // then
        assertThat(sut.matchesQueryParams(queryParamResolver("page=1&ref=promo"))).isTrue()
        assertThat(sut.matchesQueryParams(queryParamResolver("ref=promo&page=1"))).isTrue()
    }

    @Test
    fun `matchesQueryParams returns true when optional typed query param is missing`() {
        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = setOf(Param(name = "page", type = ParamType.NUMERIC)),
            fragment = null,
            parametersClass = null
        )

        // then
        assertThat(sut.matchesQueryParams(queryParamResolver(""))).isTrue()
    }

    @Test
    fun `matchesQueryParams returns false when required typed query param is missing`() {
        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = setOf(Param(name = "page", type = ParamType.NUMERIC, required = true)),
            fragment = null,
            parametersClass = null
        )

        // then
        assertThat(sut.matchesQueryParams(queryParamResolver(""))).isFalse()
    }

    @Test
    fun `matchesQueryParams returns false when typed query param does not satisfy declared regex`() {
        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = setOf(Param(name = "page", type = ParamType.NUMERIC)),
            fragment = null,
            parametersClass = null
        )

        // then
        assertThat(sut.matchesQueryParams(queryParamResolver("page=abc"))).isFalse()
    }

    @Test
    fun `matchesQueryParams ignores untyped query params`() {
        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = setOf(Param(name = "ref")),
            fragment = null,
            parametersClass = null
        )

        // then
        assertThat(sut.matchesQueryParams(queryParamResolver(""))).isTrue()
    }

    @Test
    fun `deeplink pattern includes fragment`() {
        // given
        val fragment = "dummyFragment"
        val expectedPattern = "#dummyFragment"

        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = fragment,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(expectedPattern)
    }

    @Test
    fun `deeplink does not contain hash tag when fragment is absent`() {
        // given

        // when
        sut = DeeplinkSpec(
            scheme = setOf("https"),
            host = setOf("test.com"),
            pathParams = emptyList(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).doesNotContain("#")
    }

    private fun queryParamResolver(query: String): (String) -> String? {
        if (query.isBlank()) return { null }
        val queryMap = query.split("&")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
        return { key -> queryMap[key] }
    }
}
