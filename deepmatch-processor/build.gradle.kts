import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
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
        singleVariant("release")
    }

    @Suppress("UnstableApiUsage")
    testFixtures {
        enable = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {

    api(project(":deepmatch-api"))
    implementation(kotlin("reflect"))
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk.core)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(project(":deepmatch-testing"))
}

publishing {
    publications {
        afterEvaluate {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = findProperty("groupName") as String
                version = findProperty("commonVersion") as String
                artifactId = "deepmatch-processor"
            }
        }
    }
}
