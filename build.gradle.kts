plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.12.0"
    id("maven-publish")
}

rootProject.group = "com.gradle.enterprise"
rootProject.version = "0.1"

repositories {
    jcenter()
    gradlePluginPortal()
}

dependencies {
    val gradleEnterprisePluginVersion = "3.3.4"
    val junit5Version = "5.6.2"
    val commonsIOVersion = "2.7"
    val jacksonVersion = "2.10.3"
    implementation("commons-io:commons-io:$commonsIOVersion")

    compileOnly("com.gradle:gradle-enterprise-gradle-plugin:${gradleEnterprisePluginVersion}")
    testImplementation("com.gradle:gradle-enterprise-gradle-plugin:${gradleEnterprisePluginVersion}")
    implementation(gradleApi())

    testImplementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")
}

gradlePlugin {
    plugins.create("conventionsPlugin") {
        id = "com.gradle.enterprise.gradle-enterprise-conventions-plugin"
        implementationClass = "com.gradle.enterprise.conventions.GradleEnterpriseConventionsPlugin"
        displayName = "Gradle Enterprise Conventions Plugin"
        description = "Gradle Enterprise Conventions Plugin"
    }
}

pluginBundle {
    website = "https://github.com/gradle/gradle-enterprise-conventions-plugin"
    vcsUrl = "https://github.com/gradle/gradle-enterprise-conventions-plugin.git"
    tags = listOf("gradle", "gradle enterprise")
}

tasks.named("test", Test::class.java) {
    useJUnitPlatform()
}
