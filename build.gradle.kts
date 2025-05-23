plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
    id("maven-publish")
}

rootProject.group = "io.github.gradle"
rootProject.version = "0.12.0"

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
    testImplementation(libs.develocity.plugin)
    implementation(gradleApi())

    testImplementation(libs.bundles.jackson)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.mockitoJunitJupiter)
}

// Workaround for https://github.com/gradle/dev-infrastructure/issues/505#issuecomment-762060878
extensions.configure<ExtraPropertiesExtension>("ext") {
    set("gradle.publish.key", project.findProperty("gradlePublishKey"))
    set("gradle.publish.secret", project.findProperty("gradlePublishSecret"))
}

gradlePlugin {
    website = "https://github.com/gradle/gradle-enterprise-conventions-plugin"
    vcsUrl = "https://github.com/gradle/gradle-enterprise-conventions-plugin.git"

    plugins.create("conventionsPlugin") {
        id = "io.github.gradle.develocity-conventions-plugin"
        implementationClass = "com.gradle.enterprise.conventions.DevelocityConventionsPlugin"
        displayName = "Develocity Conventions Plugin"
        description = "Develocity Conventions Plugin for OSS Gradle projects"
        website = "https://github.com/gradle/gradle-org-conventions-plugin"
        vcsUrl = "https://github.com/gradle/gradle-org-conventions-plugin.git"
        tags = listOf("gradle", "gradle enterprise", "develocity")
    }
}

tasks.named("publishPlugins", Task::class.java) {
    dependsOn("check")
}

tasks.named("test", Test::class.java) {
    useJUnitPlatform()
}
