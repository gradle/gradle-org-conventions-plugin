import jetbrains.buildServer.configs.kotlin.AbsoluteId
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CheckoutMode
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs

private val vcsRoot = AbsoluteId("GradlePlugins_GradleEnterpriseConventionsPlugin_Master")

object Project : Project({
    buildType(Verify)
    buildType(ReleasePlugin)
    params {
        param("env.DEVELOCITY_ACCESS_KEY", "%ge.gradle.org.access.key%")
    }
})

private val defaultGradleParameters = listOf(
    "--build-cache",
    "-Dorg.gradle.java.installations.auto-download=false",
    "-Dorg.gradle.java.installations.auto-detect=false",
    "-Dorg.gradle.java.installations.fromEnv=JAVA_HOME,JAVA_TOOLCHAIN",
    // drop after upgrading to Gradle 9.2+
    "-Porg.gradle.java.installations.auto-download=false",
    "-Porg.gradle.java.installations.auto-detect=false",
    "-Porg.gradle.java.installations.fromEnv=JAVA_HOME,JAVA_TOOLCHAIN",
)

object Verify : BuildType({
    id = AbsoluteId("VerifyGradleEnterpriseConventionsPlugin")
    uuid = "VerifyGradleEnterpriseConventionsPlugin"
    name = "Verify Develocity Conventions Plugin"
    description = "Verify Develocity Conventions Plugin"

    vcs {
        root(vcsRoot)

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

    steps {
        gradle {
            useGradleWrapper = true
            tasks = "check"
            gradleParams = defaultGradleParameters.joinToString(" ")
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = vcsRoot.absoluteId
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%github.bot-teamcity.token%"
                }
            }
        }

        pullRequests {
            vcsRootExtId = vcsRoot.absoluteId
            provider = github {
                authType = vcsRoot()
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
                ignoreDrafts = true
            }
        }
    }
})

object ReleasePlugin : BuildType({
    id = AbsoluteId("ReleaseGradleEnterpriseConventionsPlugin")
    uuid = "ReleaseGradleEnterpriseConventionsPlugin"
    type = Type.DEPLOYMENT
    name = "Release Develocity Conventions Plugin"
    description = "Release Develocity Conventions Plugin"

    vcs {
        root(vcsRoot)
        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    steps {
        gradle {
            useGradleWrapper = true
            gradleParams = (listOf("-Dgradle.publish.skip.namespace.check=true") + defaultGradleParameters).joinToString(" ")
            tasks = "publishPlugins"
        }
    }

    params {
        param("env.ORG_GRADLE_PROJECT_gradlePublishKey", "%plugin.portal.publish.key%")
        param("env.ORG_GRADLE_PROJECT_gradlePublishSecret", "%plugin.portal.publish.secret%")
    }
})
