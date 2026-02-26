package com.aouledissa.deepmatch.gradle.internal.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeeplinkConfigTest {

    @Test
    fun `containsTemplateParams returns true when fragment is present`() {
        val config = DeeplinkConfig(
            name = "open profile",
            activity = "com.example.app.MainActivity",
            scheme = listOf("app"),
            host = listOf("example.com"),
            fragment = "details"
        )

        assertThat(config.containsTemplateParams()).isTrue()
    }

    @Test
    fun `containsTemplateParams returns false when no typed inputs are defined`() {
        val config = DeeplinkConfig(
            name = "open profile",
            activity = "com.example.app.MainActivity",
            scheme = listOf("app"),
            host = listOf("example.com"),
            fragment = null
        )

        assertThat(config.containsTemplateParams()).isFalse()
    }
}
