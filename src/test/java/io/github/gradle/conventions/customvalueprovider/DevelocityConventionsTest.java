package io.github.gradle.conventions.customvalueprovider;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.gradle.conventions.customvalueprovider.DevelocityConventions.execAndGetStdout;
import static io.github.gradle.conventions.customvalueprovider.DevelocityConventions.getRemoteGitHubRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DevelocityConventionsTest {
    @TempDir
    Path projectDir;

    @ParameterizedTest
    @CsvSource({"https://github.com/gradle/gradle.git", "git@github.com:gradle/gradle.git", "not_a_url"})
    void getRemoteGitHubRepositoryTest(String url) throws IOException {
        Files.createFile(projectDir.resolve("fileToCommit.txt"));
        execAndGetStdout(projectDir, "git", "init");
        execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", url);

        if ("not_a_url".equals(url)) {
            assertFalse(getRemoteGitHubRepository(projectDir).isPresent());
        } else {
            assertEquals("https://github.com/gradle/gradle", getRemoteGitHubRepository(projectDir).get());
        }
    }
}
