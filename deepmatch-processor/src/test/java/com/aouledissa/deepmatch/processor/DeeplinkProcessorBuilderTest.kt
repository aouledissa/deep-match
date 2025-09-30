package com.aouledissa.deepmatch.processor

import com.aouledissa.deepmatch.api.DeeplinkParams
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
        val handler = FakeDeeplinkHandler<DeeplinkParams>()

        val chainedBuilder = builder.register(deeplinkSpec, handler)

        assertThat(chainedBuilder).isSameInstanceAs(builder)
    }

    @Test
    fun `build creates DeeplinkProcessorImpl with registered handler`() {
        val handler = FakeDeeplinkHandler<DeeplinkParams>()
        val processor = DeeplinkProcessor.Builder()
            .register(deeplinkSpec, handler)
            .build()

        assertThat(processor).isInstanceOf(DeeplinkProcessorImpl::class.java)

        val registry = processor.registry()
        assertThat(registry).containsKey(deeplinkSpec)
        assertThat(registry[deeplinkSpec]).isSameInstanceAs(handler)
    }

    @Test
    fun `register does not override handler for same spec`() {
        val firstHandler = FakeDeeplinkHandler<DeeplinkParams>()
        val secondHandler = FakeDeeplinkHandler<DeeplinkParams>()

        val processor = DeeplinkProcessor.Builder()
            .register(deeplinkSpec, firstHandler)
            .register(deeplinkSpec, secondHandler)
            .build()

        val registry = processor.registry()
        assertThat(registry).hasSize(1)
        assertThat(registry[deeplinkSpec]).isSameInstanceAs(firstHandler)
    }
}

private fun DeeplinkProcessor.registry(): Map<DeeplinkSpec, DeeplinkHandler<out DeeplinkParams>> {
    val impl = this as DeeplinkProcessorImpl
    val field = DeeplinkProcessorImpl::class.java.getDeclaredField("registry")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(impl) as Map<DeeplinkSpec, DeeplinkHandler<out DeeplinkParams>>
}
