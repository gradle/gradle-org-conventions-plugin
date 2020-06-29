import jetbrains.buildServer.configs.kotlin.v2018_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs


object Project : Project({
    buildType(Verify)
    buildType(ReleasePlugin)
})

object Verify : BuildType({
    id = AbsoluteId("VerifyGradleEnterpriseConventionsPlugin")
    uuid = "VerifyGradleEnterpriseConventionsPlugin"
    name = "Verify Gradle Enterprise Conventions Plugin"
    description = "Verify Gradle Enterprise Conventions Plugin"

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
                    +:<default>
                    +:refs/heads/*
                """
        }
    }

    steps {
        gradle {
            useGradleWrapper = true
            tasks = "check"
            buildFile = "build.gradle.kts"
        }
    }
})

object ReleasePlugin : BuildType({
    id = AbsoluteId("ReleaseGradleEnterpriseConventionsPlugin")
    uuid = "ReleaseGradleEnterpriseConventionsPlugin"
    name = "Release Gradle Enterprise Conventions Plugin"
    description = "Release Gradle Enterprise Conventions Plugin"

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
            gradleParams = "-Dgradle.publish.skip.namespace.check=true -Pgradle.publish.key=%GRADLE_PUBLISH_KEY% -Pgradle.publish.secret=%GRADLE_PUBLISH_SECRET%"
            tasks = "publishPlugins"
            buildFile = "build.gradle.kts"
        }
    }
    params {
        param("env.JAVA_HOME", "%linux.java8.oracle.64bit%")
        password("GRADLE_PUBLISH_KEY", "credentialsJSON:8b35ff9d-0de0-4266-8850-9162aa31b4bf", display = jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay.HIDDEN)
        password("GRADLE_PUBLISH_SECRET", "credentialsJSON:6934a5f0-e002-4067-884e-5090de3c9ec3", display = jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay.HIDDEN)
    }
})

