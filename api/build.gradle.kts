import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
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
    implementation(libs.kotlin.serialization.json)
}

publishing {
    publications {
        register<MavenPublication>("DeepMatchApi") {
            from(components["java"])
            groupId = findProperty("group_name") as String
            artifactId = "api"
            version = findProperty("deepmatch_version") as String
        }
    }
}
