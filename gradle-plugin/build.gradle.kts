import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.maven.publish)
}

group = findProperty("groupName") as String
version = findProperty("commonVersion") as String

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
    implementation(project(":api"))
    implementation(libs.android.gradle.api)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.squareup.kotlinpoet)
    implementation(libs.kaml)
    implementation(libs.xmlUtil.serialization)
}

gradlePlugin {
    plugins {
        create("DeepMatchPlugin") {
            id = "com.aouledissa.deepmatch.plugin.android"
            implementationClass = "com.aouledissa.deepmatch.gradle.DeepMatchPlugin"
        }
    }
}
