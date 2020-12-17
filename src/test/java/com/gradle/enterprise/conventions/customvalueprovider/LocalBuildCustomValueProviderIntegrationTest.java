package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions.execAndGetStdout;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalBuildCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @BeforeEach
    public void setUp() {
        super.setUp();
        withPublicGradleEnterpriseUrl();
    }

    @Test
    public void tagIDEAVersionIfAvailable() {
        succeeds("help", "-Didea.active", "-Didea.paths.selector=2020.1");

        assertTrue(getConfiguredBuildScan().containsTag("LOCAL"));
        assertTrue(getConfiguredBuildScan().containsTag("IDEA"));
        assertTrue(getConfiguredBuildScan().containsValue("ideaVersion", "2020.1"));
    }


    @Test
    public void addGitCommitLinkLocally() {
        write("fileToCommit.txt", "hello");
        execAndGetStdout(projectDir, "git", "init");
        execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", "https://github.com/gradle/gradle.git");
        execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        String headCommit = execAndGetStdout(projectDir, "git", "log", "-1", "--format=%H").get();

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsBackgroundLink("Source", "https://github.com/gradle/gradle/commit/" + headCommit));
    }
}
