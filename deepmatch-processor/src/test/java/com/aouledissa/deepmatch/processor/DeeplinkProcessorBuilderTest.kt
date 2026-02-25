package com.aouledissa.deepmatch.processor

import com.aouledissa.deepmatch.api.DeeplinkSpec
import com.aouledissa.deepmatch.processor.internal.DeeplinkProcessorImpl
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeeplinkProcessorBuilderTest {

    private val deeplinkSpec = DeeplinkSpec(
        scheme = setOf("app"),
        host = setOf("example.com"),
        pathParams = emptySet(),
        queryParams = emptySet(),
        fragment = null,
        parametersClass = null
    )

    @Test
    fun `register returns the same Builder instance`() {
        val builder = DeeplinkProcessor.Builder()

        val chainedBuilder = builder.register(deeplinkSpec)

        assertThat(chainedBuilder).isSameInstanceAs(builder)
    }

    @Test
    fun `build creates DeeplinkProcessorImpl with registered spec`() {
        val processor = DeeplinkProcessor.Builder()
            .register(deeplinkSpec)
            .build()

        assertThat(processor).isInstanceOf(DeeplinkProcessorImpl::class.java)

        val registry = processor.registry()
        assertThat(registry).contains(deeplinkSpec)
    }

    @Test
    fun `register ignores duplicate specs`() {
        val processor = DeeplinkProcessor.Builder()
            .register(deeplinkSpec)
            .register(deeplinkSpec)
            .build()

        val registry = processor.registry()
        assertThat(registry).hasSize(1)
        assertThat(registry).contains(deeplinkSpec)
    }
}

private fun DeeplinkProcessor.registry(): Set<DeeplinkSpec> {
    val impl = this as DeeplinkProcessorImpl
    val field = DeeplinkProcessorImpl::class.java.getDeclaredField("registry")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(impl) as Set<DeeplinkSpec>
}
