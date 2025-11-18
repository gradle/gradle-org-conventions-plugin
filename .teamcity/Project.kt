import jetbrains.buildServer.configs.kotlin.AbsoluteId
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CheckoutMode
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.buildSteps.script


object Project : Project({
    buildType(Verify)
    params {
        param("env.DEVELOCITY_ACCESS_KEY", "tc/gradle-org-conventions-plugin/_all/DEVELOCITY_ACCESS_KEY")
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

    steps {
        script {
            name = "Test"
            scriptContent = """
                echo -n "${'$'}DEVELOCITY_ACCESS_KEY" > result.txt
            """.trimIndent()
        }
    }

    features {
        feature {
            type = "aws-secrets-build-feature"
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
