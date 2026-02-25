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
    namespace = "com.aouledissa.deepmatch.processor"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {

    api(project(":deepmatch-api"))
    implementation(kotlin("reflect"))
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk.core)
    testImplementation(libs.robolectric)
    testImplementation(project(":deepmatch-testing"))
}

publishing {
    publications {
        register<MavenPublication>("deepMatchProcessor") {
            artifactId = "deepmatch-processor"
            project.afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("DeepMatch Processor")
                description.set("Runtime processor that matches URIs against DeepMatch specifications")
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
    sign(publishing.publications["deepMatchProcessor"])
}
