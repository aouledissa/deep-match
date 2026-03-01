/*
 * Copyright 2026 DeepMatch Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aouledissa.deepmatch.processor

import android.net.Uri
import com.aouledissa.deepmatch.api.DeeplinkParams
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class CompositeDeeplinkProcessorTest {

    @Test
    fun `match returns first non-null result in registration order`() {
        val uri = mockk<Uri>(relaxed = true)
        val first = StubProcessor(result = null)
        val second = StubProcessor(result = ProfileParams("123"))
        val third = StubProcessor(result = ProfileParams("999"))

        val processor = CompositeDeeplinkProcessor(first, second, third)

        val result = processor.match(uri) as ProfileParams?

        assertThat(result?.userId).isEqualTo("123")
        assertThat(first.invocationCount).isEqualTo(1)
        assertThat(second.invocationCount).isEqualTo(1)
        assertThat(third.invocationCount).isEqualTo(0)
    }

    @Test
    fun `match returns null when none of the processors match`() {
        val uri = mockk<Uri>(relaxed = true)
        val processor = CompositeDeeplinkProcessor(
            StubProcessor(result = null),
            StubProcessor(result = null)
        )

        assertThat(processor.match(uri)).isNull()
    }

    data class ProfileParams(val userId: String) : DeeplinkParams

    private class StubProcessor(private val result: DeeplinkParams?) :
        DeeplinkProcessor(specs = emptySet()) {
        var invocationCount: Int = 0
            private set

        override fun match(deeplink: Uri): DeeplinkParams? {
            invocationCount += 1
            return result
        }
    }
}
