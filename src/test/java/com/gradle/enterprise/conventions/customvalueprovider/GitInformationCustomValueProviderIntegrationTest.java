package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GitInformationCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @Test
    public void doNothingIfNotAGitRepo() {
        succeeds("help");

        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundValue("Git Status"));
        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundValue("Git Branch Name"));
    }

    @Test
    public void tagDirtyIfGitRepoIsDirty() {
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "init");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "add", ".");

        succeeds("help");

        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundValue("Git Status"));
        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundValue("Git Branch Name"));
    }

    @Test
    public void addGitBranchNameIfAvailable() {
        write(".gitignore", "*", "!fileToCommit.txt");
        write("fileToCommit.txt", "hello");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "init");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "add", ".");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");

        succeeds("help");

        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundValue("Git Status"));
        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundValue("Git Branch Name", "new-branch"));
    }
}
