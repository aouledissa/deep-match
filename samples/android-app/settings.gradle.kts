pluginManagement {
    includeBuild("../..")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
