package io.github.gradle.conventions.customvalueprovider;

import io.github.gradle.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.api.Test;

import static io.github.gradle.conventions.customvalueprovider.DevelocityConventions.execAndGetStdout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalBuildCustomValueProviderIntegrationTest extends AbstractDevelocityPluginIntegrationTest {
    @Test
    void tagIDEAVersionIfAvailable() {
        succeeds("help", "-Didea.active", "-Didea.paths.selector=2020.1");

        assertTrue(getConfiguredBuildScan().containsTag("LOCAL"));
        assertTrue(getConfiguredBuildScan().containsBackgroundTag("IDEA"));
        assertTrue(getConfiguredBuildScan().containsBackgroundValue("ideaVersion", "2020.1"));
    }


    @Test
    void addGitCommitLinkLocally() {
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
