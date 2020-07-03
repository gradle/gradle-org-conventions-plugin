package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalBuildCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @Test
    public void tagIDEAVersionIfAvailable() {
        succeeds("help", "-Didea.active", "-Didea.paths.selector=2020.1");

        Assertions.assertTrue(getConfiguredBuildScan().containsTag("LOCAL"));
        Assertions.assertTrue(getConfiguredBuildScan().containsTag("IDEA"));
        Assertions.assertTrue(getConfiguredBuildScan().containsValue("IDEA version", "2020.1"));
    }


    @Test
    public void addGitCommitLinkLocally() {
        write("fileToCommit.txt", "hello");
        Utils.execAndGetStdout(projectDir, "git", "init");
        Utils.execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");
        Utils.execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        Utils.execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        String headCommit = Utils.execAndGetStdout(projectDir, "git", "log", "-1", "--format=%H").get();

        succeeds("help");

        Assertions.assertTrue(getConfiguredBuildScan().containsBackgroundLink("Source", "https://github.com/gradle/gradle/commit/" + headCommit));
    }
}
