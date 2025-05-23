import jetbrains.buildServer.configs.kotlin.AbsoluteId
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CheckoutMode
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs


object Project : Project({
    buildType(Verify)
    buildType(ReleasePlugin)
    params {
        param("env.DEVELOCITY_ACCESS_KEY", "%ge.gradle.org.access.key%")
    }
})

object Verify : BuildType({
    id = AbsoluteId("VerifyGradleEnterpriseConventionsPlugin")
    uuid = "VerifyGradleEnterpriseConventionsPlugin"
    name = "Verify Develocity Conventions Plugin"
    description = "Verify Develocity Conventions Plugin"

    vcs {
        root(AbsoluteId("GradlePlugins_GradleEnterpriseConventionsPlugin_Master"))

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    triggers {
        vcs {
            branchFilter = """
    +:refs/heads/*
""".trimIndent()
        }
    }

    params {
        param("env.GRADLE_CACHE_REMOTE_USERNAME", "%gradle.cache.remote.username%")
        param("env.GRADLE_CACHE_REMOTE_PASSWORD", "%gradle.cache.remote.password%")
    }

    steps {
        gradle {
            useGradleWrapper = true
            tasks = "check"
            gradleParams = "--build-cache"
            buildFile = "build.gradle.kts"
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "GradlePlugins_GradleEnterpriseConventionsPlugin_Master"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%github.bot-teamcity.token%"
                }
            }
        }
    }
})

object ReleasePlugin : BuildType({
    id = AbsoluteId("ReleaseGradleEnterpriseConventionsPlugin")
    uuid = "ReleaseGradleEnterpriseConventionsPlugin"
    name = "Release Develocity Conventions Plugin"
    description = "Release Develocity Conventions Plugin"

    vcs {
        root(AbsoluteId("GradlePlugins_GradleEnterpriseConventionsPlugin_Master"))

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    steps {
        gradle {
            useGradleWrapper = true
            gradleParams = "--build-cache -Dgradle.publish.skip.namespace.check=true"
            tasks = "publishPlugins"
            buildFile = "build.gradle.kts"
        }
    }

    params {
        param("env.GRADLE_CACHE_REMOTE_USERNAME", "%gradle.cache.remote.username%")
        param("env.GRADLE_CACHE_REMOTE_PASSWORD", "%gradle.cache.remote.password%")
        param("env.ORG_GRADLE_PROJECT_gradlePublishKey", "%plugin.portal.publish.key%")
        param("env.ORG_GRADLE_PROJECT_gradlePublishSecret", "%plugin.portal.publish.secret%")
    }
})
