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

package com.aouledissa.deepmatch.convention

import org.gradle.api.publish.maven.MavenPom

fun MavenPom.configureDeepMatchPom() {
    url.set("https://aouledissa.com/deep-match/")
    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
        }
    }
    developers {
        developer {
            name.set("aouledissa")
            email.set("dev@aouledissa.com")
            url.set("https://www.aouledissa.com")
        }
    }
    scm {
        connection.set("scm:git:git://github.com/aouledissa/deep-match.git")
        developerConnection.set("scm:git:ssh://github.com/aouledissa/deep-match.git")
        url.set("https://github.com/aouledissa/deep-match/tree/main")
    }
}
