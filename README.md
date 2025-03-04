# Gradle Enterprise Conventions Plugin

Inspired by https://github.com/spring-gradle-plugins/gradle-enterprise-conventions-plugin, this plugin configures public [Gradle projects](https://github.com/gradle)
to use the public Gradle Enterprise instance at [ge.gradle.org](https://ge.gradle.org).

Requires Gradle 7.6+. The plugin is configuration-cache compatible when used the Gradle Enterprise plugin 3.11.3+.

## What it does

When applied as a settings plugin alongside the [Develocity Plugin](https://plugins.gradle.org/plugin/com.gradle.develocity), this plugin does the following:

- If the build cache is enabled (via `--build-cache` or `org.gradle.caching=true`, see [the doc](https://guides.gradle.org/using-build-cache/)):
  - Enable the local cache.
  - Enable [ge.gradle.org](https://ge.gradle.org) as remote cache and anonymous read access, enjoy faster build!
    - There're three build cache node available on the earth: `eu`(the default)/`us`/`au`, you can use `-DcacheNode=us`/`-DcacheNode=au` to use other ones.
  - Enable pushing to remote cache on CI if required credentials are provided.
- By default, build scans are published to `ge.gradle.org`. If you would like to publish to your own GE server, add `-Dgradle.enterprise.url=https://ge.mycompany.com/`.
  If you would like to publish to public build scan server (`scan.gradle.com`), add `-DagreePublicBuildScanTermOfService=yes` to your build.
  - For CI build (`CI` environment variable exists):
    - Add `CI` build scan tag.
    - Add build scan link and build scan custom value `gitCommitId` to the build (by auto detecting environment variables):
      - Travis: `TRAVIS_BUILD_ID`/`TRAVIS_BUILD_WEB_URL`
      - Jenkins: `BUILD_ID`/`BUILD_URL`
      - GitHub Actions: `${System.getenv("GITHUB_RUN_ID")} ${System.getenv("GITHUB_RUN_NUMBER")}`/`https://github.com/gradle/gradle/runs/${System.getenv("GITHUB_RUN_ID")}`
      - TeamCity: `BUILD_ID`/`BUILD_URL`
    - Upload build scans in the foreground.
  - For local build:
    - Add `LOCAL` build scan tag.
    - Add build scan custom value `gitCommitId` by running `git rev-parse --verify HEAD`.
    - If running in IDEA:
      - Add `IDEA` build scan tag.
      - Add build scan custom value `ideaVersion` to IDEA version.
    - Upload build scans in the background.
  - For CI and local builds:
    - Add build scan custom value `gitBranchName` by running `git rev-parse --abbrev-ref HEAD`.
    - If the build directory is dirty:
      - Add build scan tag `dirty`
      - Add build scan custom value `gitStatus` with the output of `git status --porcelain`

## Use the plugin

The plugin is published to gradle plugin portal.

This is done by configuring a plugin management repository in `settings.gradle`, as shown in the following example:

```
plugins {
    // …
    id "com.gradle.develocity" version "<<version>>"
    id "io.github.gradle.gradle-enterprise-conventions-plugin" version "<<version>>"
    // …
}
```

## Credentials

To enable build scan publishing, authenticate with [Develocity doc](https://docs.gradle.com/develocity/gradle-plugin/current/#authenticating_with_gradle_enterprise), then add a `gradle.enterprise.url` system property to your build.

```
./gradlew myBuildTask -Dgradle.enterprise.url=https://ge.mycompany.com/
```

To enable build cache pushing, the access key associated with the build needs to have build cache write permission.

```
export GRADLE_CACHE_REMOTE_URL=https://ge.mycompany.com/
./gradlew myBuildTask 
```

```
./gradlew myBuildTask -Dgradle.cache.remote.server=https://ge.mycompany.com/
```

To enable build scan publishing, you need to correctly authenticate as documented [here](https://docs.gradle.com/develocity/gradle-plugin/current/#authenticating).

## Development

Feel free to fork this repository, customize the plugin, and make a contribution!

You can install the plugin to local maven repository via:

```
./gradlew publishPluginMavenPublicationToMavenLocal
```

Then use the plugin under development via:

```
buildscript {
    repositories { 
        mavenLocal() 
    }
    dependencies {
        classpath("com.gradle.enterprise:gradle-enterprise-conventions-plugin:${thePluginVersion}")
    }
}

plugins {
    id("com.gradle.develocity").version("3.19.2")
}

apply(plugin="io.github.gradle.gradle-enterprise-conventions-plugin")

```


