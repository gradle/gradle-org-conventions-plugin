package com.gradle.enterprise.conventions.customvalueprovider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;

class GradleEnterpriseConventionsTest {
    @TempDir
    File projectDir;

    @ParameterizedTest
    @CsvSource({"https://github.com/gradle/gradle.git", "git@github.com:gradle/gradle.git", "not_a_url"})
    public void getRemoteGitHubRepositoryTest(String url) throws IOException {
        new File(projectDir, "fileToCommit.txt").createNewFile();
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "init");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "checkout", "-b", "new-branch");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "add", "fileToCommit.txt");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "commit", "-m", "Initial commit");
        GradleEnterpriseConventions.execAndGetStdout(projectDir, "git", "config", "--add", "remote.origin.url", url);

        if ("not_a_url".equals(url)) {
            Assertions.assertFalse(GradleEnterpriseConventions.getRemoteGitHubRepository(projectDir).isPresent());
        } else {
            Assertions.assertEquals("https://github.com/gradle/gradle", GradleEnterpriseConventions.getRemoteGitHubRepository(projectDir).get());
        }
    }
}
