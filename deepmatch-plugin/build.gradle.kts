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

val integrationTest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val integrationTestRuntimeOnly: Configuration by configurations.getting

// Dedicated resolvable configuration for AGP so pluginUnderTestMetadata can pick it up
// without a circular dependency on integrationTestRuntimeClasspath (which itself depends
// on pluginUnderTestMetadata output once testSourceSets.add(integrationTest) is applied).
val agpPluginMetadata: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(project(":deepmatch-api"))
    compileOnly(libs.android.gradle.api)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.squareup.kotlinpoet)
    implementation(libs.kaml)
    implementation(libs.xmlUtil.serialization)
    testImplementation(gradleTestKit())
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    // AGP on the integrationTest runtime classpath so GradleRunner can load the plugin
    integrationTestRuntimeOnly(libs.android.gradle.api)
    // Same AGP jar fed into pluginUnderTestMetadata via a cycle-free resolvable config
    agpPluginMetadata(libs.android.gradle.api)
}

tasks.named<org.gradle.plugin.devel.tasks.PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(agpPluginMetadata)
}

tasks.register<Test>("integrationTest") {
    description = "Runs GradleRunner integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
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
    // Registers integrationTest as a test source set for GradleTestKit:
    // - automatically wires plugin-under-test metadata onto its runtime classpath
    // - makes GradleRunner.withPluginClasspath() work in integration tests
    testSourceSets.add(integrationTest)
}
