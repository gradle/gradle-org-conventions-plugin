package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CICustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    private String headCommitId;

    @BeforeEach
    public void setUp() {
        write("fileToCommit.txt", "hello");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "init");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");

        headCommitId = GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "log", "-1", "--format=%H").get();
    }

    @Test
    public void workWithJenkins() {
        withEnvironmentVariable("CI", "1");
        withEnvironmentVariable("JENKINS_HOME", "1");
        withEnvironmentVariable("BUILD_URL", "https://jenkins");
        withEnvironmentVariable("BUILD_ID", "jenkins_id");
        withEnvironmentVariable("GIT_COMMIT", headCommitId);

        succeeds("help");

        Assertions.assertTrue(getConfiguredBuildScan().containsLink("Jenkins Build", "https://jenkins"));
        Assertions.assertTrue(getConfiguredBuildScan().containsValue("Build ID", "jenkins_id"));
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

        Assertions.assertTrue(getConfiguredBuildScan().containsLink("TeamCity Build", "https://teamcity"));
        Assertions.assertTrue(getConfiguredBuildScan().containsValue("Build ID", "teamcity_id"));
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

        Assertions.assertTrue(getConfiguredBuildScan().containsLink("Travis Build", "https://travis"));
        Assertions.assertTrue(getConfiguredBuildScan().containsValue("Build ID", "travis_id"));
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

        Assertions.assertTrue(getConfiguredBuildScan().containsValue("Build ID", "123 456"));
        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundLink("GitHub Actions Build", "https://github.com/gradle/gradle/runs/123"));
        verifyGitCommitInformation();
    }

    private void verifyGitCommitInformation() {
        Assertions.assertTrue(getConfiguredBuildScan().containsValue("Git Commit ID", headCommitId));
        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundLink("Source", String.format("https://github.com/gradle/gradle/commit/%s", headCommitId)));
        Assertions.assertTrue(getConfiguredBuildScan().containsLink("Git Commit Scans", "https://ge.gradle.org/scans?search.names=Git+Commit+ID&search.values=" + headCommitId));
    }
}
