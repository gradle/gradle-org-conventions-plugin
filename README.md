# Develocity Conventions Plugin

Inspired by https://github.com/spring-io/develocity-conventions, this plugin configures public [Gradle projects](https://github.com/gradle)
to use the public Develocity instance at [ge.gradle.org](https://ge.gradle.org).

Requires Java 17+ and Gradle 8.14+ and Develocity plugin 4.2+.

## What it does

When applied as a settings plugin alongside the [Develocity Plugin](https://plugins.gradle.org/plugin/com.gradle.develocity), this plugin does the following:

- If the build cache is enabled (via `--build-cache` or `org.gradle.caching=true`, see [the doc](https://guides.gradle.org/using-build-cache/)):
  - Enable the local cache.
  - Enable [ge.gradle.org](https://ge.gradle.org) as remote cache with anonymous read access, enjoy faster build!
  - Enable edge discovery by default so the nearest edge node is selected based on your preferred [location](https://ge.gradle.org/settings/location).
  - Enable pushing to remote cache on CI if required credentials are provided.
- By default, build scans are published to `ge.gradle.org`. If you would like to publish to your own Develocity server, set `-Ddevelocity.server.url=https://ge.example.org/` or the `DEVELOCITY_SERVER_URL` environment variable. The URL can point directly at an edge node (for example `https://edge.example.org`) to use a specific build cache location when edge discovery is disabled.
  If you would like to publish to public build scan server (`scans.gradle.com`), add `-DagreePublicBuildScanTermOfService=yes` to your build.
  - For CI build (`CI` environment variable exists):
    - Add `CI` build scan tag.
    - Add build scan link and build scan custom value `gitCommitId` to the build (by auto-detecting environment variables):
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
  - When an AI coding agent is detected:
    - Add `AGENT` build scan tag.
    - Add build scan custom value `ai.agent` with the name of the detected agent.
    - Detection checks the [`AGENT` environment variable](https://github.com/agentsmd/agents.md/issues/136), then falls back to tool-specific variables: `CLAUDECODE`, `CURSOR_AGENT`, `GEMINI_CLI`, `CODEX_SANDBOX`, `OPENCODE_CLIENT`.
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
    id("com.gradle.develocity").version("<version>")
    id("io.github.gradle.develocity-conventions-plugin").version("<version>")
    // …
}
```

## Credentials

To enable build scan publishing, authenticate with [Develocity](https://docs.gradle.com/develocity/gradle-plugin/current/#authenticating).
Then set the Develocity server URL via the `develocity.server.url` system property or the `DEVELOCITY_SERVER_URL` environment variable if you publish to a different server than the default one.

Edge discovery is enabled by default for the remote build cache. To disable it, set the `develocity.edge.discovery` system property or the `DEVELOCITY_EDGE_DISCOVERY` environment variable to `false`. The system property takes precedence over the environment variable.

The legacy `-DcacheNode` system property is no longer supported. See [Change edge node location](#change-edge-node-location) for how to target a specific build cache location.

```
./gradlew myBuildTask -Ddevelocity.server.url=https://ge.example.org/
```

```
export DEVELOCITY_SERVER_URL=https://edge.example.org
./gradlew myBuildTask
```

```
./gradlew myBuildTask --build-cache -Ddevelocity.edge.discovery=false
```

```
export DEVELOCITY_EDGE_DISCOVERY=false
./gradlew myBuildTask --build-cache
```

To enable build cache pushing, the access key associated with the build needs to have build cache write permission.

To enable build scan publishing, you need to correctly authenticate as documented [here](https://docs.gradle.com/develocity/gradle-plugin/current/#authenticating).

## Change edge node location

The remote build cache is served by geographically distributed edge nodes. With edge discovery enabled (the default), the nearest edge is picked automatically based on your preferred [location](https://ge.gradle.org/settings/location) at `ge.gradle.org`.

If you have a registered `ge.gradle.org` account, set your preferred location at [ge.gradle.org/settings/location](https://ge.gradle.org/settings/location) and edge discovery will route your builds accordingly.

If you do not have an account (and therefore cannot set a preferred location), point `develocity.server.url` / `DEVELOCITY_SERVER_URL` directly at an edge node:

| Region | URL                            |
|--------|--------------------------------|
| EU     | `https://eun-edge.gradle.org`  |
| US     | `https://usw-edge.gradle.org`  |
| AU     | `https://au-edge.gradle.org`   |

```
./gradlew myBuildTask -Ddevelocity.server.url=https://eun-edge.gradle.org
```

```
export DEVELOCITY_SERVER_URL=https://usw-edge.gradle.org
./gradlew myBuildTask
```

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
        classpath("io.github.gradle:develocity-conventions-plugin:${thePluginVersion}")
    }
}

plugins {
    id("com.gradle.develocity").version("4.0.1")
}

apply(plugin= "io.github.gradle.develocity-conventions-plugin")

```


