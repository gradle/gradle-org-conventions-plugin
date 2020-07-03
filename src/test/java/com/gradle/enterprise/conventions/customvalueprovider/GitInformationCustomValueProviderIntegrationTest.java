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
        Utils.execAndGetStdout(projectDir, "git", "init");
        Utils.execAndGetStdout(projectDir, "git", "add", ".");

        succeeds("help");

        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundValue("Git Status"));
        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundValue("Git Branch Name"));
    }

    @Test
    public void addGitBranchNameIfAvailable() {
        String headCommit = prepareGitCommit();

        succeeds("help");

        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundValue("Git Status"));
        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundValue("Git Branch Name", "new-branch"));
        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundLink("Source", "https://github.com/gradle/gradle/commit/" + headCommit));
    }

    @Test
    public void onlyAddGitCommitLinkLocally() {
        withEnvironmentVariable("CI", "1");
        prepareGitCommit();

        succeeds("help");

        Assertions.assertFalse(getConfiguredBuildScan().containsBackgroundLink("Source"));
    }

    private String prepareGitCommit() {
        write(".gitignore", "*", "!fileToCommit.txt");
        write("fileToCommit.txt", "hello");
        Utils.execAndGetStdout(projectDir, "git", "init");
        Utils.execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");
        Utils.execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        Utils.execAndGetStdout(projectDir, "git", "add", ".");
        Utils.execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");

        return Utils.execAndGetStdout(projectDir, "git", "log", "-1", "--format=%H").get();
    }
}
