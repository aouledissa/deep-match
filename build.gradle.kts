import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.gradleup.nmcp) apply false
    alias(libs.plugins.gradleup.nmcp.aggregation)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.detekt)
}

group = "com.aouledissa.deepmatch"
version = (project.findProperty("deep.match.version") as? String)
    ?: System.getenv("DEEP_MATCH_VERSION")
    ?: "0.0.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
    }
}

nmcpAggregation {
    centralPortal {
        username.set((findProperty("sonatype.user") as? String) ?: System.getenv("MAVEN_USERNAME"))
        password.set((findProperty("sonatype.token") as? String) ?: System.getenv("MAVEN_TOKEN"))
        publishingType = "USER_MANAGED"
        publicationName = "DeepMatch-$version"
    }
}

dependencies {
    nmcpAggregation(project(":deepmatch-api"))
    nmcpAggregation(project(":deepmatch-processor"))
    nmcpAggregation(project(":deepmatch-testing"))
}
