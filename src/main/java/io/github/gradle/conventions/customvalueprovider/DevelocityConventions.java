package io.github.gradle.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.gradle.conventions.customvalueprovider.ScanCustomValueNames.GIT_COMMIT_NAME;

public class DevelocityConventions {
    private static final Logger LOGGER = Logging.getLogger(DevelocityConventions.class);
    private static final String DEFAULT_DEVELOCITY_SERVER = "https://ge.gradle.org";
    private static final String AGREE_PUBLIC_BUILD_SCAN_TERM_OF_SERVICE = "agreePublicBuildScanTermOfService";

    private static final String DEVELOCITY_SERVER_URL = "develocity.server.url";
    private static final String DEVELOCITY_EDGE_DISCOVERY = "develocity.edge.discovery";
    private static final String CI_ENV_NAME = "CI";

    private static final String GRADLE_CACHE_REMOTE_SERVER_ENV_NAME = "GRADLE_CACHE_REMOTE_SERVER";
    private static final String GRADLE_CACHE_REMOTE_SERVER_PROPERTY_NAME = "gradle.cache.remote.server";
    private static final String GRADLE_CACHE_NODE_PROPERTY_NAME = "cacheNode";

    private static final Pattern HTTPS_URL_PATTERN = Pattern.compile("https://github\\.com/([\\w-]+)/([\\w-]+)\\.git");
    private static final Pattern SSH_URL_PATTERN = Pattern.compile("git@github\\.com:([\\w-]+)/([\\w-]+)\\.git");
    private static final Pattern SHA_PATTERN = Pattern.compile("[0-9a-fA-F]+");

    private final ProviderFactory providerFactory;
    private final String develocityServerUrl;
    private final boolean isCiServer;
    private final boolean edgeDiscovery;

    public DevelocityConventions(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
        this.edgeDiscovery = determineEdgeDiscovery();
        this.develocityServerUrl = determineDevelocityServerUrl();
        this.isCiServer = !getEnvVariable(CI_ENV_NAME, "").isEmpty();
    }

    private boolean determineEdgeDiscovery() {
        return Boolean.parseBoolean(getSystemProperty(DEVELOCITY_EDGE_DISCOVERY, "true"));
    }

    public Provider<String> getRemoteCacheUrl() {
        return environmentVariableProvider(GRADLE_CACHE_REMOTE_SERVER_ENV_NAME)
            .orElse(systemPropertyProvider(GRADLE_CACHE_REMOTE_SERVER_PROPERTY_NAME));
    }

    public Provider<String> getRemoteCacheNodeName() {
        return systemPropertyProvider(GRADLE_CACHE_NODE_PROPERTY_NAME);
    }

    public boolean isRemoteCacheSpecified() {
        return getRemoteCacheUrl().orElse(getRemoteCacheNodeName()).isPresent();
    }

    private String determineDevelocityServerUrl() {
        String dvServerUrl = System.getProperty(DEVELOCITY_SERVER_URL);
        if (dvServerUrl != null) {
            return dvServerUrl;
        }

        String agreePublicBuildScanTermOfService = System.getProperty(AGREE_PUBLIC_BUILD_SCAN_TERM_OF_SERVICE, "no");
        if ("yes".equals(agreePublicBuildScanTermOfService)) {
            // So that we can publish to default DV instance (https://scans.gradle.com)
            return null;
        } else {
            return DEFAULT_DEVELOCITY_SERVER;
        }
    }

    public Optional<String> customValueSearchUrl(Map<String, String> search) {
        // public DV instance
        if (develocityServerUrl == null) {
            return Optional.empty();
        }
        String query = search.entrySet()
            .stream()
            .map(entry -> String.format("search.names=%s&search.values=%s", urlEncode(entry.getKey()), urlEncode(entry.getValue())))
            .collect(Collectors.joining("&"));
        return Optional.of(String.format("%s/scans?%s", develocityServerUrl, query));
    }

    public boolean getEdgeDiscovery() {
        return edgeDiscovery;
    }

    public String getDevelocityServerUrl() {
        return develocityServerUrl;
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
     * @param buildScan  the build scan extension
     * @param commitId   the commit id
     */
    public void setCommitId(Path projectDir, BuildScanConfiguration buildScan, String commitId) {
        if (!SHA_PATTERN.matcher(commitId).matches()) {
            LOGGER.warn("Detect illegal commitId: {}, skip.", commitId);
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
        Provider<String> remoteGitHubRepositoryProvider = providerFactory.of(
            RemoteGitHubRepositoryValueSource.class,
            it -> it.parameters(
                parameters -> parameters.getProjectDir().set(projectDir.toFile())
            )
        );

        toOptional(remoteGitHubRepositoryProvider)
            .ifPresent(repoUrl -> buildScan.link("Source", String.format("%s/commit/%s", repoUrl, commitId)));
    }

    public Optional<String> getCommitId(File workDir) {
        Provider<String> commitIdProvider = providerFactory.of(
            CurrentCommitIdValueSource.class,
            it -> it.parameters(
                parameters -> parameters.getWorkDir().set(workDir)
            )
        );
        return toOptional(commitIdProvider);
    }

    private static Optional<String> toOptional(Provider<String> remoteGitHubRepositoryProvider) {
        return Optional.ofNullable(remoteGitHubRepositoryProvider.getOrNull());
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
     * @param args       the process to run and its command-line arguments
     * @return the contents of the stdout as a string
     */
    public static Optional<String> execAndGetStdout(Path workingDir, String... args) {
        try {
            Process process = new ProcessBuilder(args).directory(workingDir.toFile()).start();
            process.waitFor(1, TimeUnit.MINUTES);
            String stdout = toString(process.getInputStream());
            String stderr = toString(process.getErrorStream());
            if (process.exitValue() != 0) {
                LOGGER.error("Run {} in {} returns {}, outputs: \n{}\n{}", Arrays.toString(args), workingDir.toFile().getAbsolutePath(), process.exitValue(), stdout, stderr);
                return Optional.empty();
            }

            LOGGER.info("Run {} in {} outputs \n{}\n{}", Arrays.toString(args), workingDir.toFile().getAbsolutePath(), stdout, stderr);
            return Optional.of(stdout.trim());
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Run {} in {} failed:", Arrays.toString(args), workingDir.toFile().getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    static Optional<String> getCurrentCommitId(Path workDir) {
        return execAndGetStdout(workDir, "git", "rev-parse", "--verify", "HEAD");
    }

    public abstract static class CurrentCommitIdValueSource implements ValueSource<String, CurrentCommitIdValueSource.Params> {
        interface Params extends ValueSourceParameters {
            DirectoryProperty getWorkDir();
        }

        @Nullable
        @Override
        public String obtain() {
            return getCurrentCommitId(getParameters().getWorkDir().getAsFile().get().toPath()).orElse(null);
        }
    }

    static Optional<String> getRemoteGitHubRepository(Path projectDir) {
        return execAndGetStdout(projectDir, "git", "config", "--get", "remote.origin.url").flatMap(DevelocityConventions::parseGitHubRemoteUrl);
    }

    public abstract static class RemoteGitHubRepositoryValueSource implements ValueSource<String, RemoteGitHubRepositoryValueSource.Params> {
        interface Params extends ValueSourceParameters {
            DirectoryProperty getProjectDir();
        }

        @Nullable
        @Override
        public String obtain() {
            return getRemoteGitHubRepository(getParameters().getProjectDir().getAsFile().get().toPath()).orElse(null);
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
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
