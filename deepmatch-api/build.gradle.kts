import com.aouledissa.deepmatch.convention.configureDeepMatchPom
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gradleup.nmcp)
    signing
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

extensions.configure<JavaPluginExtension> {
    withSourcesJar()
    withJavadocJar()
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
        register<MavenPublication>("deepMatchApi") {
            from(components["java"])
            artifactId = "deepmatch-api"
            pom {
                name.set("DeepMatch Api")
                description.set("Shared model classes for DeepMatch.")
                configureDeepMatchPom()
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications["deepMatchApi"])
}
