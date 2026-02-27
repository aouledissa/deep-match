pluginManagement {
    includeBuild("../..")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("com.aouledissa.deepmatch:deepmatch-api")).using(project(":deepmatch-api"))
        substitute(module("com.aouledissa.deepmatch:deepmatch-processor")).using(project(":deepmatch-processor"))
        substitute(module("com.aouledissa.deepmatch:deepmatch-testing")).using(project(":deepmatch-testing"))
    }
}

rootProject.name = "app"
