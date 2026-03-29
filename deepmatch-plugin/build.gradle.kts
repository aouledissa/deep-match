import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.java.library)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.gradle.publish)
    signing
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
    // AGP is intentionally NOT on the integration test runtime classpath.
    // Integration test projects resolve AGP directly from remote repositories at the version
    // specified by the agp.version system property, allowing multi-version testing.
}

val deepMatchVersion: String? = providers.environmentVariable("DEEP_MATCH_VERSION")
    .orElse(providers.gradleProperty("deep.match.version"))
    .getOrElse("0.0.0-SNAPSHOT")

val localMavenRepo = "${System.getProperty("user.home")}/.m2/repository"

fun registerIntegrationTestTask(taskName: String, agpVersion: String, gradleVersion: String? = null) =
    tasks.register<Test>(taskName) {
        description = "Runs GradleRunner integration tests against AGP $agpVersion."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.runtimeClasspath
        systemProperty("agp.version", agpVersion)
        systemProperty("deepmatch.plugin.version", deepMatchVersion)
        systemProperty("integration.test.local.repo", localMavenRepo)
        // AGP 8.x is not compatible with Gradle 9.x. Pin the GradleRunner to a Gradle version
        // that is compatible with the AGP series under test.
        if (gradleVersion != null) {
            systemProperty("gradle.version.for.test", gradleVersion)
        }
        // Locally: auto-publish so `./gradlew integrationTestAgp9` works without a separate step.
        // In CI: artifacts are pre-built by the build-artifacts job and downloaded to ~/.m2,
        // so re-publishing would wastefully recompile the entire project.
        if (System.getenv("CI") == null) {
            dependsOn(rootProject.getTasksByName("publishToMavenLocal", true))
        }
    }

registerIntegrationTestTask("integrationTestAgp9", libs.versions.agp.get())
registerIntegrationTestTask("integrationTestAgp8", libs.versions.agp8.get(), libs.versions.gradle8.get())

tasks.register("integrationTest") {
    description = "Runs GradleRunner integration tests against all supported AGP versions."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    dependsOn("integrationTestAgp9", "integrationTestAgp8")
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

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    isRequired = signingKey != null // disable signing for local builds and test
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}