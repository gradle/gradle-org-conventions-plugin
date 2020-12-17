package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions.execAndGetStdout;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitInformationCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @BeforeEach
    public void setUp() {
        super.setUp();
        withPublicGradleEnterpriseUrl();
    }

    @Test
    public void doNothingIfNotAGitRepo() {
        succeeds("help");

        assertFalse(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        assertFalse(getConfiguredBuildScan().containsBackgroundValue("gitStatus"));
        assertFalse(getConfiguredBuildScan().containsBackgroundValue("gitBranchName"));
    }

    @Test
    public void tagDirtyIfGitRepoIsDirty() {
        execAndGetStdout(projectDir, "git", "init");
        execAndGetStdout(projectDir, "git", "add", ".");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        assertTrue(getConfiguredBuildScan().containsBackgroundValue("gitStatus"));
        assertFalse(getConfiguredBuildScan().containsBackgroundValue("gitBranchName"));
    }

    @Test
    public void addGitBranchNameIfAvailable() {
        write(".gitignore", "*", "!fileToCommit.txt");
        write("fileToCommit.txt", "hello");
        execAndGetStdout(projectDir, "git", "init");
        execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");
        execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        execAndGetStdout(projectDir, "git", "add", ".");
        execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");

        succeeds("help");

        assertFalse(getConfiguredBuildScan().containsBackgroundTag("dirty"));
        assertFalse(getConfiguredBuildScan().containsBackgroundValue("gitStatus"));
        assertTrue(getConfiguredBuildScan().containsBackgroundValue("gitBranchName", "new-branch"));
    }
}
