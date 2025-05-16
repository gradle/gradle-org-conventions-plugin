plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
    id("maven-publish")
}

group = "io.github.gradle"
version = "0.11.0"

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
        id = "io.github.gradle.gradle-enterprise-conventions-plugin"
        implementationClass = "com.gradle.enterprise.conventions.DevelocityConventionsPlugin"
        displayName = "Develocity Conventions Plugin"
        description = "Develocity Conventions Plugin"
        website = "https://github.com/gradle/gradle-enterprise-conventions-plugin"
        vcsUrl = "https://github.com/gradle/gradle-enterprise-conventions-plugin.git"
        tags = listOf("gradle", "gradle enterprise")
    }
}

tasks.named("publishPlugins", Task::class.java) {
    dependsOn("check")
}

tasks.named("test", Test::class.java) {
    useJUnitPlatform()
}
