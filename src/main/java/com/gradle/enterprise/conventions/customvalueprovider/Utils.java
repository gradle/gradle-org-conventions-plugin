package com.gradle.enterprise.conventions.customvalueprovider;


import com.gradle.enterprise.conventions.GradleEnterpriseConventionsPlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ProviderFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {
    private static final Logger LOGGER = Logging.getLogger(Utils.class);
    static String GIT_COMMIT_NAME = "Git Commit ID";
    private final static Pattern SSH_URL_PATTERN = Pattern.compile("git@github\\.com:([\\w-]+)/([\\w-]+)\\.git");
    private final static Pattern HTTPS_URL_PATTERN = Pattern.compile("https://github\\.com/([\\w-]+)/([\\w-]+)\\.git");
    private final ProviderFactory providerFactory;

    public Utils(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    static String customValueSearchUrl(Map<String, String> search) {
        String query = search.entrySet()
            .stream()
            .map(entry -> String.format("search.names=%s&search.values=%s", urlEncode(entry.getKey()), urlEncode(entry.getValue())))
            .collect(Collectors.joining("&"));
        return String.format("%s/scans?%s", GradleEnterpriseConventionsPlugin.gradleEnterpriseServerUrl, query);
    }

    static void setCommitId(File projectDir, BuildScanExtension buildScan, String commitId) {
        buildScan.value(GIT_COMMIT_NAME, commitId);
        buildScan.link("Git Commit Scans", customValueSearchUrl(mapOf(GIT_COMMIT_NAME, commitId)));
        buildScan.background(__ -> {
            getRemoteGitHubRepository(projectDir).ifPresent(repoUrl -> buildScan.link("Source", String.format("%s/commit/%s", repoUrl, commitId)));
        });
    }

    static Map<String, String> mapOf(String key, String value) {
        Map<String, String> ret = new HashMap<>();
        ret.put(key, value);
        return ret;
    }


    private static String toString(InputStream is) {
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    }

    static Optional<String> execAndGetStdout(File workingDir, String... args) {
        try {
            Process process = new ProcessBuilder(args).directory(workingDir).start();
            process.waitFor(1, TimeUnit.MINUTES);
            String stdout = toString(process.getInputStream());
            String stderr = toString(process.getErrorStream());
            if (process.exitValue() != 0) {
                LOGGER.error("Run " + Arrays.toString(args) + " in " + workingDir.getAbsolutePath()
                    + " returns " + process.exitValue() + ", outputs: \n" + stdout + "\n" + stderr);
                return Optional.empty();
            }

            LOGGER.info("Run " + Arrays.toString(args) + " in " + workingDir.getAbsolutePath() + " outputs \n" + stdout + "\n" + stderr);
            return Optional.of(stdout.trim());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Run " + Arrays.toString(args) + " in " + workingDir.getAbsolutePath() + " failed:", e);
            return Optional.empty();
        }
    }

    static Optional<String> getRemoteGitHubRepository(File projectDir) {
        return execAndGetStdout(projectDir, "git", "config", "--get", "remote.origin.url").flatMap(Utils::parseGitHubRemoteUrl);
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> parseGitHubRemoteUrl(String gitOutput) {
        Matcher sshMatcher = SSH_URL_PATTERN.matcher(gitOutput);
        if (sshMatcher.find()) {
            return Optional.of(String.format("https://github.com/%s/%s", sshMatcher.group(1), sshMatcher.group(2)));
        } else {
            Matcher httpsMatcher = HTTPS_URL_PATTERN.matcher(gitOutput);
            if (httpsMatcher.find()) {
                return Optional.of(String.format("https://github.com/%s/%s", httpsMatcher.group(1), httpsMatcher.group(2)));
            } else {
                return Optional.empty();
            }
        }
    }
}
