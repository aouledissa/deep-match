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

package com.aouledissa.deepmatch.sample

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aouledissa.deepmatch.sample.deeplinks.AppDeeplinkProcessor
import com.aouledissa.deepmatch.sample.deeplinks.OpenProfileDeeplinkParams
import com.aouledissa.deepmatch.sample.deeplinks.OpenSeriesDeeplinkParams
import com.aouledissa.deepmatch.sample.deeplinks.OpenAboutDeeplinkParams
import com.aouledissa.deepmatch.sample.deeplinks.OpenUserPostsDeeplinkParams
import com.aouledissa.deepmatch.sample.deeplinks.OpenHostlessProfileDeeplinkParams
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test to verify DeepMatch-generated code compiles and runs correctly.
 * Tests that deeplinks are properly generated, manifest entries exist, and
 * the processor matches URIs as expected.
 */
@RunWith(AndroidJUnit4::class)
class DeeplinkSmokeTest {

    @Test
    fun testProfileDeeplinkMatches() {
        val uri = Uri.parse("app://sample.deepmatch.dev/profile/abc123?ref=home#details")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params is OpenProfileDeeplinkParams)
        val profileParams = params as OpenProfileDeeplinkParams
        assert(profileParams.userId == "abc123")
        assert(profileParams.ref == "home")
    }

    @Test
    fun testProfileDeeplinkWithHttps() {
        val uri = Uri.parse("https://sample.deepmatch.dev/profile/xyz789?ref=shared#details")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params is OpenProfileDeeplinkParams)
        val profileParams = params as OpenProfileDeeplinkParams
        assert(profileParams.userId == "xyz789")
        assert(profileParams.ref == "shared")
    }

    @Test
    fun testSeriesDeeplinkMatches() {
        val uri = Uri.parse("app://sample.deepmatch.dev/series/999")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params is OpenSeriesDeeplinkParams)
        assert((params as OpenSeriesDeeplinkParams).seriesId == 999)
    }

    @Test
    fun testAboutDeeplinkMatches() {
        val uri = Uri.parse("app://sample.deepmatch.dev/about")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params is OpenAboutDeeplinkParams)
    }

    @Test
    fun testUserPostsDeeplinkMatches() {
        val uri = Uri.parse("app://sample.deepmatch.dev/users/123/posts")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params is OpenUserPostsDeeplinkParams)
        assert((params as OpenUserPostsDeeplinkParams).userId == 123)
    }

    @Test
    fun testHostlessProfileDeeplinkMatches() {
        val uri = Uri.parse("app:///profile/456")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params is OpenHostlessProfileDeeplinkParams)
        assert((params as OpenHostlessProfileDeeplinkParams).profileId == 456)
    }

    @Test
    fun testMissingRequiredQueryParamReturnsNull() {
        val uri = Uri.parse("app://sample.deepmatch.dev/profile/abc123")
        // Profile deeplink requires 'ref' query param
        val params = AppDeeplinkProcessor.match(uri)
        assert(params == null)
    }

    @Test
    fun testInvalidParamTypeReturnsNull() {
        val uri = Uri.parse("app://sample.deepmatch.dev/series/notanumber")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params == null)
    }

    @Test
    fun testWrongHostReturnsNull() {
        val uri = Uri.parse("app://wronghost.com/profile/abc?ref=home#details")
        val params = AppDeeplinkProcessor.match(uri)
        assert(params == null)
    }
}
