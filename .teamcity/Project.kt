import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.versionedSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs


object Project : Project({
    features {
        versionedSettings {
            id = "GradlePlugins_GradleEnterpriseConventionsPlugin_master"
            mode = VersionedSettings.Mode.ENABLED
            buildSettingsMode = VersionedSettings.BuildSettingsMode.PREFER_SETTINGS_FROM_VCS
            rootExtId = "GradlePlugins_GradleEnterpriseConventionsPlugin_master"
            showChanges = true
            settingsFormat = VersionedSettings.Format.KOTLIN
            storeSecureParamsOutsideOfVcs = true
        }
    }

    buildType(Verify)
    buildType(ReleasePlugin)
})

object Verify : BuildType({
    id = AbsoluteId("VerifyGradleEnterpriseConventionsPlugin")
    uuid = "VerifyGradleEnterpriseConventionsPlugin"
    name = "VerifyGradleEnterpriseConventionsPlugin"
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
            buildFile = "build.gradle.kts"
            tasks = "check"
        }
    }
})

object ReleasePlugin : BuildType({
    id = AbsoluteId("ReleaseGradleEnterpriseConventionsPlugin")
    uuid = "ReleaseGradleEnterpriseConventionsPlugin"
    name = "ReleaseGradleEnterpriseConventionsPlugin"
    description = "Release Gradle Enterprise Conventions Plugin"

    vcs {
        root(AbsoluteId("GradlePlugins_GradleEnterpriseConventionsPlugin_Master"))

        checkoutMode = CheckoutMode.ON_AGENT
        cleanCheckout = true
    }

    requirements {
        contains("teamcity.agent.jvm.os.name", "Linux")
    }
})

