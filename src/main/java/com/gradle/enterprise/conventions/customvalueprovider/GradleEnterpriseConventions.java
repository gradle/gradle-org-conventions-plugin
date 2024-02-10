package com.gradle.enterprise.conventions.customvalueprovider;


import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

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
    private static final String DEFAULT_GRADLE_ENTERPRISE_SERVER = "https://ge.gradle.org";
    private static final String AGREE_PUBLIC_BUILD_SCAN_TERM_OF_SERVICE = "agreePublicBuildScanTermOfService";
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
        this.gradleEnterpriseServerUrl = determineGradleEnterpriseUrl();
        this.isCiServer = !getEnvVariable(CI_ENV_NAME, "").isEmpty();
    }

    private String determineGradleEnterpriseUrl() {
        String geServerUrl = System.getProperty(GRADLE_ENTERPRISE_URL_PROPERTY_NAME);
        if (geServerUrl != null) {
            return geServerUrl;
        }

        String agreePublicBuildScanTermOfService = System.getProperty(AGREE_PUBLIC_BUILD_SCAN_TERM_OF_SERVICE, "no");
        if ("yes".equals(agreePublicBuildScanTermOfService)) {
            // So that we can publish to default GE instance (https://gradle.com)
            return null;
        } else {
            return DEFAULT_GRADLE_ENTERPRISE_SERVER;
        }
    }

    public Optional<String> customValueSearchUrl(Map<String, String> search) {
        // public GE instance
        if (gradleEnterpriseServerUrl == null) {
            return Optional.empty();
        }
        String query = search.entrySet()
            .stream()
            .map(entry -> String.format("search.names=%s&search.values=%s", urlEncode(entry.getKey()), urlEncode(entry.getValue())))
            .collect(Collectors.joining("&"));
        return Optional.of(String.format("%s/scans?%s", gradleEnterpriseServerUrl, query));
    }

    public String getGradleEnterpriseServerUrl() {
        return gradleEnterpriseServerUrl;
    }

    public boolean isCiServer() {
        return isCiServer;
    }

    public String getEnvVariableThenSystemProperty(String envName, String systemPropertyName, String defaultValue) {
        String value = getEnv(envName);
        return value != null ? value : getSystemProperty(systemPropertyName, defaultValue);
    }

    public String getSystemProperty(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }

    @Nullable
    public String getSystemProperty(String name) {
        return getSystemProperty(name, null);
    }

    public String getEnvVariable(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }

    @Nullable
    public String getEnv(String name) {
        return getEnvVariable(name, null);
    }

    public Provider<String> systemPropertyProvider(String name) {
        return providerFactory.systemProperty(name);
    }

    public Provider<String> environmentVariableProvider(String name) {
        return providerFactory.environmentVariable(name);
    }

    /**
     * Add commit ID to tags.
     *
     * @param projectDir the project directory
     * @param buildScan the build scan extension
     * @param commitId the commit id
     */
    public void setCommitId(File projectDir, BuildScanExtension buildScan, String commitId) {
        if (!SHA_PATTERN.matcher(commitId).matches()) {
            LOGGER.warn("Detect illegal commitId: " + commitId + ", skip.");
            return;
        }

        buildScan.value(GIT_COMMIT_NAME, commitId);
        customValueSearchUrl(Collections.singletonMap(GIT_COMMIT_NAME, commitId))
            .ifPresent(url -> buildScan.link("Git Commit Scans", url));
        // This is a configuration-safe way of invoking external process at the configuration time since Gradle 7.5.
        // As of Gradle 8.7, it still may add the remote url to the configuration cache inputs when used in the
        // buildScan.background callback, but this value doesn't change very often.
        // CI value providers call this method outside the background callback, so the ValueSource is mandatory to avoid
        // failing the build because of CC errors there.
        @SuppressWarnings("UnstableApiUsage")
        Provider<String> remoteGitHubRepositoryProvider = providerFactory.of(
            RemoteGitHubRepositoryValueSource.class,
            it -> it.parameters(
                parameters -> parameters.getProjectDir().set(projectDir)
            )
        );

        toOptional(remoteGitHubRepositoryProvider)
            .ifPresent(repoUrl -> buildScan.link("Source", String.format("%s/commit/%s", repoUrl, commitId)));
    }

    private static Optional<String> toOptional(Provider<String> remoteGitHubRepositoryProvider) {
        return remoteGitHubRepositoryProvider.map(Optional::of).getOrElse(Optional.empty());
    }

    private static String toString(InputStream is) {
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    }

    /**
     * Executes the external process and returns its standard output. An empty optional is returned when the process
     * fails to start or returns an error code.
     * <p>
     * Avoid using this method at configuration time to keep configuration cache compatibility. It is an error with
     * Gradle 7.5+. Since Gradle 7.6 it is safe to use this method in {@code buildScan.background} callback.
     * Consider implementing {@link ValueSource} if you need to obtain external process output at configuration time.
     *
     * @param workingDir the working directory
     * @param args the process to run and its command-line arguments
     * @return the contents of the stdout as a string
     */
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

    static Optional<String> getRemoteGitHubRepository(File projectDir) {
        return execAndGetStdout(projectDir, "git", "config", "--get", "remote.origin.url").flatMap(GradleEnterpriseConventions::parseGitHubRemoteUrl);
    }

    @SuppressWarnings("UnstableApiUsage")
    public abstract static class RemoteGitHubRepositoryValueSource implements ValueSource<String, RemoteGitHubRepositoryValueSource.Params> {
        interface Params extends ValueSourceParameters {
            DirectoryProperty getProjectDir();
        }

        @Nullable
        @Override
        public String obtain() {
            return getRemoteGitHubRepository(getParameters().getProjectDir().getAsFile().get()).orElse(null);
        }
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
