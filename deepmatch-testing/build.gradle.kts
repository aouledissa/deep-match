import com.aouledissa.deepmatch.convention.configureDeepMatchPom
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gradleup.nmcp)
    signing
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.aouledissa.deepmatch.testing"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":deepmatch-processor"))
}

publishing {
    publications {
        register<MavenPublication>("deepMatchTesting") {
            artifactId = "deepmatch-testing"
            project.afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("DeepMatch Testing")
                description.set(" Shared test fixtures (fake handlers/processors and spec builders) used by the runtime and plugin tests.")
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
    sign(publishing.publications["deepMatchTesting"])
}
