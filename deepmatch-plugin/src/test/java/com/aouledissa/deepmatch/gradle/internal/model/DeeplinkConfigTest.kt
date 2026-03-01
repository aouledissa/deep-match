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
