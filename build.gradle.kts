plugins {
    `java-gradle-plugin`
    alias(libs.plugins.plugin.publish)
    `maven-publish`
    signing
}

rootProject.group = "io.github.gradle"
rootProject.version = "0.14.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.develocity.plugin)

    implementation(gradleApi())

    testImplementation(platform(libs.jackson.bom))
    testImplementation("com.fasterxml.jackson.core:jackson-annotations")
    testImplementation("tools.jackson.core:jackson-core")
    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation(libs.develocity.plugin)
    testImplementation(libs.mockitoJunitJupiter)
}

gradlePlugin {
    website = "https://github.com/gradle/gradle-org-conventions-plugin"
    vcsUrl = "https://github.com/gradle/gradle-org-conventions-plugin.git"

    plugins.create("conventionsPlugin") {
        id = "io.github.gradle.develocity-conventions-plugin"
        implementationClass = "io.github.gradle.conventions.DevelocityConventionsPlugin"
        displayName = "Develocity Conventions Plugin"
        description = "Develocity Conventions Plugin for OSS Gradle projects"
        website = "https://github.com/gradle/gradle-org-conventions-plugin"
        vcsUrl = "https://github.com/gradle/gradle-org-conventions-plugin.git"
        tags = listOf("gradle", "develocity")
    }
}

signing {
    useInMemoryPgpKeys(
        project.providers.environmentVariable("PGP_SIGNING_KEY").orNull,
        project.providers.environmentVariable("PGP_SIGNING_KEY_PASSPHRASE").orNull
    )

    setRequired({
        providers.environmentVariable("CI").isPresent
    })
}

tasks.named("publishPlugins", Task::class.java) {
    dependsOn("check")
}

testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.get())
        }
    }
}

tasks.updateDaemonJvm.configure {
    toolchainDownloadUrls.empty()
}
