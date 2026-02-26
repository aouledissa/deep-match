package com.aouledissa.deepmatch.gradle.internal.model

import com.aouledissa.deepmatch.api.Param
import com.aouledissa.deepmatch.api.ParamType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeeplinkConfigTest {

    @Test
    fun `hasTypedParams returns true when typed path param is present`() {
        val config = DeeplinkConfig(
            name = "open profile",
            activity = "com.example.app.MainActivity",
            scheme = listOf("app"),
            host = listOf("example.com"),
            pathParams = listOf(Param(name = "id", type = ParamType.NUMERIC))
        )

        assertThat(config.hasTypedParams()).isTrue()
    }

    @Test
    fun `hasTypedParams returns false when no typed inputs are defined`() {
        val config = DeeplinkConfig(
            name = "open profile",
            activity = "com.example.app.MainActivity",
            scheme = listOf("app"),
            host = listOf("example.com"),
            fragment = null
        )

        assertThat(config.hasTypedParams()).isFalse()
    }
}
