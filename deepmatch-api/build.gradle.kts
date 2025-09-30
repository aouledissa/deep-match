import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.java.testFixtures)
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
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
}

publishing {
    publications {
        register<MavenPublication>("DeepMatchApi") {
            from(components["java"])
            groupId = findProperty("groupName") as String
            version = findProperty("commonVersion") as String
            artifactId = "api"
        }
    }
}
