// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.gradleup.nmcp) apply false
    alias(libs.plugins.gradleup.nmcp.aggregation)
    alias(libs.plugins.binary.compatibility.validator)
}

group = "com.aouledissa.deepmatch"
version = (project.findProperty("deep.match.version") as? String)
    ?: System.getenv("DEEP_MATCH_VERSION")

allprojects {
    group = rootProject.group
    version = rootProject.version
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
