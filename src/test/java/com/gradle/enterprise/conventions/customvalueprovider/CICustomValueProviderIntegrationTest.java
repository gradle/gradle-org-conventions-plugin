package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions.execAndGetStdout;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CICustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    private String headCommitId;

    @BeforeEach
    public void setUp() {
        super.setUp();
        withPublicGradleEnterpriseUrl();
        write("fileToCommit.txt", "hello");
        execAndGetStdout(projectDir, "git", "init");
        execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");

        headCommitId = execAndGetStdout(projectDir, "git", "log", "-1", "--format=%H").get();
    }

    @Test
    public void workWithJenkins() {
        withEnvironmentVariable("CI", "1");
        withEnvironmentVariable("JENKINS_HOME", "1");
        withEnvironmentVariable("BUILD_URL", "https://jenkins");
        withEnvironmentVariable("BUILD_ID", "jenkins_id");
        withEnvironmentVariable("GIT_COMMIT", headCommitId);

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsLink("Jenkins Build", "https://jenkins"));
        assertTrue(getConfiguredBuildScan().containsValue("buildId", "jenkins_id"));
        verifyGitCommitInformation();
    }

    @Test
    public void workWithTeamCity() {
        withEnvironmentVariable("CI", "1");
        withEnvironmentVariable("TEAMCITY_VERSION", "1");
        withEnvironmentVariable("BUILD_URL", "https://teamcity");
        withEnvironmentVariable("BUILD_ID", "teamcity_id");
        withEnvironmentVariable("BUILD_VCS_NUMBER", headCommitId);

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsLink("TeamCity Build", "https://teamcity"));
        assertTrue(getConfiguredBuildScan().containsValue("buildId", "teamcity_id"));
        verifyGitCommitInformation();
    }

    @Test
    public void workWithTravis() {
        withEnvironmentVariable("CI", "1");
        withEnvironmentVariable("TRAVIS", "1");
        withEnvironmentVariable("TRAVIS_BUILD_WEB_URL", "https://travis");
        withEnvironmentVariable("TRAVIS_BUILD_ID", "travis_id");
        withEnvironmentVariable("TRAVIS_COMMIT", headCommitId);

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsLink("Travis Build", "https://travis"));
        assertTrue(getConfiguredBuildScan().containsValue("buildId", "travis_id"));
        verifyGitCommitInformation();
    }

    @Test
    public void workWithGitHubActions() {
        withEnvironmentVariable("CI", "1");
        withEnvironmentVariable("GITHUB_ACTIONS", "1");
        withEnvironmentVariable("GITHUB_RUN_ID", "123");
        withEnvironmentVariable("GITHUB_RUN_NUMBER", "456");
        withEnvironmentVariable("GITHUB_SHA", headCommitId);

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsValue("buildId", "123 456"));
        assertTrue(getConfiguredBuildScan().containsBackgroundLink("GitHub Actions Build", "https://github.com/gradle/gradle/runs/123"));
        verifyGitCommitInformation();
    }

    private void verifyGitCommitInformation() {
        assertTrue(getConfiguredBuildScan().containsValue("gitCommitId", headCommitId));
        assertTrue(getConfiguredBuildScan().containsBackgroundLink("Source", String.format("https://github.com/gradle/gradle/commit/%s", headCommitId)));
        assertTrue(getConfiguredBuildScan().containsLink("Git Commit Scans", "https://ge.gradle.org/scans?search.names=gitCommitId&search.values=" + headCommitId));
    }
}
