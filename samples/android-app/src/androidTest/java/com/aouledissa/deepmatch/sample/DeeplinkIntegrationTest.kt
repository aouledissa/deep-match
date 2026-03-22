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

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests that verify the generated manifest intent filters are correct.
 *
 * Uses PackageManager.resolveActivity() to check whether the OS would route a given
 * deeplink URI to MainActivity — testing the generated manifest, not the runtime processor.
 */
@RunWith(AndroidJUnit4::class)
class DeeplinkIntegrationTest {

    private val packageManager
        get() = InstrumentationRegistry.getInstrumentation().targetContext.packageManager

    private fun resolve(uri: String) = packageManager.resolveActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)),
        0
    )

    // region: valid deeplinks should route to MainActivity

    @Test
    fun profileDeeplinkResolvesToMainActivity() {
        val resolved = resolve("app://sample.deepmatch.dev/profile/abc123?ref=home#details")
        assertNotNull(resolved)
        assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
    }

    @Test
    fun profileDeeplinkWithHttpsResolvesToMainActivity() {
        val resolved = resolve("https://sample.deepmatch.dev/profile/abc123?ref=home#details")
        assertNotNull(resolved)
        assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
    }

    @Test
    fun seriesDeeplinkResolvesToMainActivity() {
        val resolved = resolve("app://sample.deepmatch.dev/series/42")
        assertNotNull(resolved)
        assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
    }

    @Test
    fun aboutDeeplinkResolvesToMainActivity() {
        val resolved = resolve("app://sample.deepmatch.dev/about")
        assertNotNull(resolved)
        assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
    }

    @Test
    fun userPostsDeeplinkResolvesToMainActivity() {
        val resolved = resolve("app://sample.deepmatch.dev/users/42/posts")
        assertNotNull(resolved)
        assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
    }

    @Test
    fun hostlessProfileDeeplinkResolvesToMainActivity() {
        val resolved = resolve("app:///profile/123")
        assertNotNull(resolved)
        assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
    }

    // endregion

    // region: invalid deeplinks should NOT route to MainActivity

    @Test
    fun unknownSchemeDoesNotResolve() {
        assertNull(resolve("unknown://sample.deepmatch.dev/profile/abc123?ref=home"))
    }

    @Test
    fun httpsOnSeriesDoesNotResolve() {
        // Series spec only declares app:// scheme, not https://
        assertNull(resolve("https://sample.deepmatch.dev/series/42"))
    }

    // endregion
}
