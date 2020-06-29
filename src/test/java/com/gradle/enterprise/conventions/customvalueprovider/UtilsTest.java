package com.gradle.enterprise.conventions.customvalueprovider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;

class UtilsTest {
    @TempDir
    File projectDir;

    @ParameterizedTest
    @CsvSource({"https://github.com/gradle/gradle.git", "git@github.com:gradle/gradle.git", "not_a_url"})
    public void getRemoteGitHubRepositoryTest(String url) throws IOException {
        new File(projectDir, "fileToCommit.txt").createNewFile();
        Utils.execAndGetStdout(projectDir, "git", "init");
        Utils.execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        Utils.execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        Utils.execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        Utils.execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", url);

        if ("not_a_url".equals(url)) {
            Assertions.assertFalse(Utils.getRemoteGitHubRepository(projectDir).isPresent());
        } else {
            Assertions.assertEquals("https://github.com/gradle/gradle", Utils.getRemoteGitHubRepository(projectDir).get());
        }
    }
}
