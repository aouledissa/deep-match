import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.gradle.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":deepmatch-api"))
    compileOnly(libs.android.gradle.api)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.squareup.kotlinpoet)
    implementation(libs.kaml)
    implementation(libs.xmlUtil.serialization)
    testImplementation(gradleTestKit())
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
}

gradlePlugin {
    website = "https://aouledissa.com/deep-match/"
    vcsUrl = "https://github.com/aouledissa/deep-match"
    plugins {
        create("DeepMatchPlugin") {
            id = "com.aouledissa.deepmatch.gradle"
            implementationClass = "com.aouledissa.deepmatch.gradle.DeepMatchPlugin"
            displayName = "DeepMatch Gradle Plugin"
            description = "Codegen and specs parser for Deeplink auto handling Library: DeepMatch"
            tags = listOf("android", "deeplink", "codegen", "deepmatch")
        }
    }
}
