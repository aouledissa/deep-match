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

package com.aouledissa.deepmatch.gradle.internal.config

import org.gradle.api.GradleException

internal class MissingSpecsFileException(
    private val path: String,
    private val variantName: String
) : GradleException() {
    override val message: String
        get() = """
                DeepMatch Configuration Error: Failed to find deeplink specification files.
                To resolve this, add one or more '*.deeplinks.yml' files in ONE of the following locations (checked in this order):
                  - For variant-specific configuration: $path/src/$variantName/
                  - For a global fallback: $path/

                No '*.deeplinks.yml' files were found for the '$variantName' build variant in its source directory or at the project root.
            """.trimIndent()
}
