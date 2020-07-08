package com.gradle.enterprise.conventions.customvalueprovider;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;

import static com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions.execAndGetStdout;
import static com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions.getRemoteGitHubRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GradleEnterpriseConventionsTest {
    @TempDir
    File projectDir;

    @ParameterizedTest
    @CsvSource({"https://github.com/gradle/gradle.git", "git@github.com:gradle/gradle.git", "not_a_url"})
    public void getRemoteGitHubRepositoryTest(String url) throws IOException {
        new File(projectDir, "fileToCommit.txt").createNewFile();
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
