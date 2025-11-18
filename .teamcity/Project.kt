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
        param("env.DEVELOCITY_ACCESS_KEY", "!awssm://tc/gradle-org-conventions-plugin/_all/DEVELOCITY_ACCESS_KEY")
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
