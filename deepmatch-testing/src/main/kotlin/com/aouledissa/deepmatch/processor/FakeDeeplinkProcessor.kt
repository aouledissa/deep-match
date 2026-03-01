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

class FakeDeeplinkProcessor : DeeplinkProcessor(specs = emptySet()) {
    private var resultProvider: (Uri) -> DeeplinkParams? = { null }
    private var lastDeeplink: Uri? = null

    fun configure(resultProvider: (Uri) -> DeeplinkParams?) {
        this.resultProvider = resultProvider
    }

    override fun match(deeplink: Uri): DeeplinkParams? {
        lastDeeplink = deeplink
        return resultProvider(deeplink)
    }

    fun isInvoked() = lastDeeplink != null
}
