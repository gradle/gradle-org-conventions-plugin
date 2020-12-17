package com.gradle.enterprise.conventions.customvalueprovider;


import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_COMMIT_NAME;

public class GradleEnterpriseConventions {
    private static final Logger LOGGER = Logging.getLogger(GradleEnterpriseConventions.class);
    private static final String GRADLE_ENTERPRISE_URL_PROPERTY_NAME = "gradle.enterprise.url";
    private static final String CI_ENV_NAME = "CI";

    private static final Pattern HTTPS_URL_PATTERN = Pattern.compile("https://github\\.com/([\\w-]+)/([\\w-]+)\\.git");
    private static final Pattern SSH_URL_PATTERN = Pattern.compile("git@github\\.com:([\\w-]+)/([\\w-]+)\\.git");
    private static final Pattern SHA_PATTERN = Pattern.compile("[0-9a-fA-F]+");

    private final ProviderFactory providerFactory;
    private final String gradleEnterpriseServerUrl;
    private final boolean isCiServer;

    public GradleEnterpriseConventions(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
        this.gradleEnterpriseServerUrl = getSystemProperty(GRADLE_ENTERPRISE_URL_PROPERTY_NAME, "", providerFactory);
        this.isCiServer = !providerFactory.environmentVariable(CI_ENV_NAME).forUseAtConfigurationTime().getOrElse("").isEmpty();

    }

    public String customValueSearchUrl(Map<String, String> search) {
        String query = search.entrySet()
            .stream()
            .map(entry -> String.format("search.names=%s&search.values=%s", urlEncode(entry.getKey()), urlEncode(entry.getValue())))
            .collect(Collectors.joining("&"));
        return String.format("%s/scans?%s", gradleEnterpriseServerUrl, query);
    }

    public String getGradleEnterpriseServerUrl() {
        return gradleEnterpriseServerUrl;
    }

    public boolean isCiServer() {
        return isCiServer;
    }

    public String getSystemProperty(String name, String defaultValue) {
        return getSystemProperty(name, defaultValue, getProviderFactory());
    }

    private static String getSystemProperty(String name, String defaultValue, ProviderFactory providerFactory) {
        return providerFactory.systemProperty(name).forUseAtConfigurationTime().orElse(defaultValue).get();
    }

    public Provider<String> systemPropertyProvider(String name) {
        return providerFactory.systemProperty(name).forUseAtConfigurationTime();
    }

    @Nullable
    public String getEnv(String name) {
        return environmentVariableProvider(name).getOrNull();
    }

    public Provider<String> environmentVariableProvider(String name) {
        return providerFactory.environmentVariable(name).forUseAtConfigurationTime();
    }

    public ProviderFactory getProviderFactory() {
        return providerFactory;
    }

    public void setCommitId(File projectDir, BuildScanExtension buildScan, String commitId) {
        if (!SHA_PATTERN.matcher(commitId).matches()) {
            LOGGER.warn("Detect illegal commitId: " + commitId + ", skip.");
            return;
        }

        buildScan.value(GIT_COMMIT_NAME, commitId);
        buildScan.link("Git Commit Scans", customValueSearchUrl(Collections.singletonMap(GIT_COMMIT_NAME, commitId)));
        buildScan.background(__ -> getRemoteGitHubRepository(projectDir).ifPresent(repoUrl -> buildScan.link("Source", String.format("%s/commit/%s", repoUrl, commitId))));
    }

    private static String toString(InputStream is) {
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    }

    public static Optional<String> execAndGetStdout(File workingDir, String... args) {
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

    public static Optional<String> getRemoteGitHubRepository(File projectDir) {
        return execAndGetStdout(projectDir, "git", "config", "--get", "remote.origin.url").flatMap(GradleEnterpriseConventions::parseGitHubRemoteUrl);
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
