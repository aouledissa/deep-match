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
