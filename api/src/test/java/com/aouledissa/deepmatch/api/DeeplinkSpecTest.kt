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
        val expectedPattern = "$scheme://${Regex.escape(host)}".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = scheme,
            host = setOf(host),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).isEqualTo(expectedPattern.pattern)
    }

    @Test
    fun `deeplink pattern includes multiple hosts`() {
        // given
        val scheme = "https"
        val host1 = "test.com"
        val host2 = "prod.com"
        val hosts = setOf(host1, host2)
        val expectedPattern = "$scheme://(${Regex.escape(host1)}|${Regex.escape(host2)})".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = scheme,
            host = hosts,
            pathParams = emptySet(),
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
            scheme = "https",
            host = setOf("test.com"),
            pathParams = setOf(pathParam),
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
        val params = setOf(pathParam1, pathParam2)
        val expectedPattern = params.joinToString(separator = "/") { Regex.escape(it.name) }

        // when
        sut = DeeplinkSpec(
            scheme = "https",
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
            scheme = "https",
            host = setOf("test.com"),
            pathParams = setOf(pathParam),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(pathParam.type!!.regex.pattern)
    }

    @Test
    fun `deeplink pattern includes single query parameter`() {
        // given
        val queryParam = Param(name = "userId", type = ParamType.ALPHANUMERIC)
        val expectedPattern =
            "${Regex.escape("?")}${Regex.escape(queryParam.name)}=${queryParam.type!!.regex.pattern}".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = "https",
            host = setOf("test.com"),
            pathParams = setOf(),
            queryParams = setOf(queryParam),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(expectedPattern.pattern)
    }

    @Test
    fun `deeplink pattern includes multiple query parameters`() {
        // given
        val queryParam1 = Param(name = "q1", type = ParamType.STRING)
        val queryParam2 = Param(name = "q2", type = ParamType.NUMERIC)
        val queries = setOf(queryParam1, queryParam2)
        val expectedPattern =
            "${Regex.escape("?")}${Regex.escape(queryParam1.name)}=${queryParam1.type!!.regex.pattern}&${
                Regex.escape(
                    queryParam2.name
                )
            }=${queryParam2.type!!.regex.pattern}".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = "https",
            host = setOf("test.com"),
            pathParams = setOf(),
            queryParams = queries,
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(expectedPattern.pattern)
    }

    @Test
    fun `deeplink pattern includes only typed query parameters`() {
        // given
        val queryParam1 = Param(name = "q1")
        val queryParam2 = Param(name = "q2", type = ParamType.NUMERIC)
        val queries = setOf(queryParam1, queryParam2)
        val expectedPattern =
            "${Regex.escape("?")}${Regex.escape(queryParam2.name)}=${queryParam2.type!!.regex.pattern}".toRegex()

        // when
        sut = DeeplinkSpec(
            scheme = "https",
            host = setOf("test.com"),
            pathParams = setOf(),
            queryParams = queries,
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).contains(expectedPattern.pattern)
    }

    @Test
    fun `deeplink pattern includes fragment`() {
        // given
        val fragment = "dummyFragment"
        val expectedPattern = "#dummyFragment"

        // when
        sut = DeeplinkSpec(
            scheme = "https",
            host = setOf("test.com"),
            pathParams = emptySet(),
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
            scheme = "https",
            host = setOf("test.com"),
            pathParams = emptySet(),
            queryParams = emptySet(),
            fragment = null,
            parametersClass = null
        )
        val deeplinkMatcher = sut.matcher

        // then
        assertThat(deeplinkMatcher.pattern).doesNotContain("#")
    }
}