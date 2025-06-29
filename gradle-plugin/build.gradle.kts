plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

group = findProperty("group_name") as String
version = findProperty("deepmatch_version") as String

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.android.gradle.api)
}

gradlePlugin {
    plugins {
        create("DeepMatchPlugin") {
            id = "com.aouledissa.deepmatch.gradle"
            implementationClass = "com.aouledissa.deepmatch.gradle.DeepMatchPlugin"
        }
    }
}
