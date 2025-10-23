package io.github.gradle.conventions.customvalueprovider;

import io.github.gradle.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.github.gradle.conventions.customvalueprovider.DevelocityConventions.execAndGetStdout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CICustomValueProviderIntegrationTest extends AbstractDevelocityPluginIntegrationTest {
    private String headCommitId;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        write("fileToCommit.txt", "hello");
        execAndGetStdout(projectDir, "git", "init");
        execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");

        headCommitId = execAndGetStdout(projectDir, "git", "rev-parse", "--verify", "HEAD").get();
    }

    @Test
    void workWithTeamCity() {
        withEnvironmentVariable("CI", "1");
        withEnvironmentVariable("TEAMCITY_VERSION", "1");
        withEnvironmentVariable("BUILD_URL", "https://teamcity");
        withEnvironmentVariable("BUILD_ID", "teamcity_id");
        withEnvironmentVariable("BUILD_VCS_NUMBER", headCommitId);

        succeeds("help", "-Ddevelocity.server.url=https://ge.gradle.org");

        assertTrue(getConfiguredBuildScan().containsLink("TeamCity Build", "https://teamcity"));
        assertTrue(getConfiguredBuildScan().containsValue("buildId", "teamcity_id"));
        verifyGitCommitInformation();
    }

    @Test
    void workWithGitHubActions() {
        withEnvironmentVariable("CI", "1");
        withEnvironmentVariable("GITHUB_ACTIONS", "1");
        withEnvironmentVariable("GITHUB_RUN_ID", "123");
        withEnvironmentVariable("GITHUB_RUN_NUMBER", "456");
        withEnvironmentVariable("GITHUB_HEAD_REF", "myBranch");
        withEnvironmentVariable("GITHUB_REPOSITORY", "gradle/gradle");
        withEnvironmentVariable("GITHUB_SHA", headCommitId);

        succeeds("help", "-Ddevelocity.server.url=https://ge.gradle.org");

        assertTrue(getConfiguredBuildScan().containsValue("buildId", "123 456"));
        assertTrue(getConfiguredBuildScan().containsBackgroundValue("gitBranchName", "myBranch"));
        assertTrue(getConfiguredBuildScan().containsLink("GitHub Actions Build", "https://github.com/gradle/gradle/actions/runs/123"));
        verifyGitCommitInformation();
    }

    private void verifyGitCommitInformation() {
        assertTrue(getConfiguredBuildScan().containsValue("gitCommitId", headCommitId));
        assertTrue(getConfiguredBuildScan().containsLink("Source", String.format("https://github.com/gradle/gradle/commit/%s", headCommitId)));
        assertTrue(getConfiguredBuildScan().containsLink("Git Commit Scans", "https://ge.gradle.org/scans?search.names=gitCommitId&search.values=" + headCommitId));
    }
}
