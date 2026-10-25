package io.github.gradle.conventions.publishing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishingConventionsPluginIntegrationTest {

    private static final String PUBLISH_TASK = "publishAllPublicationsToGradleInternalRepository";

    @TempDir
    protected Path projectDir;

    private final Map<String, String> environmentVariables = new LinkedHashMap<>();

    private HttpServer server;
    private final List<String> receivedAuthorizationHeaders = Collections.synchronizedList(new ArrayList<>());
    private final List<String> receivedPaths = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        write("settings.gradle.kts",
            "plugins {",
            "    id(\"io.github.gradle.publishing-conventions-plugin\")",
            "}",
            "rootProject.name = \"test-project\""
        );
        writeBuildScript("1.0-SNAPSHOT");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void targetsLocalStagingDirectoryWhenRepositoryUrlIsAbsent() {
        succeeds("help");

        // Compared loosely: the temporary directory is reported through a symlinked path on macOS.
        String url = repositoryUrl();
        assertTrue(url.startsWith("file:"), url);
        assertTrue(url.endsWith("/build/staging-repo"), url);
    }

    @Test
    void publishesToLocalStagingDirectoryWithoutAnyCredentials() {
        succeeds(PUBLISH_TASK);

        Path stagingRepo = projectDir.resolve("build/staging-repo/org/example/test-project");
        assertTrue(Files.isDirectory(stagingRepo), "expected artifacts under " + stagingRepo);
    }

    @Test
    void targetsSnapshotsRepositoryForSnapshotVersions() {
        withEnvironmentVariable(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME, "https://repo.example.com");
        writeBuildScript("1.0-SNAPSHOT");

        succeeds("help");

        assertEquals("https://repo.example.com/libs-snapshots-local", repositoryUrl());
    }

    @Test
    void targetsReleasesRepositoryForReleaseVersions() {
        withEnvironmentVariable(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME, "https://repo.example.com");
        writeBuildScript("1.0");

        succeeds("help");

        assertEquals("https://repo.example.com/libs-releases-local", repositoryUrl());
    }

    @Test
    void trimsTrailingSlashesFromConfiguredRepositoryUrl() {
        withEnvironmentVariable(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME, "https://repo.example.com//");

        succeeds("help");

        assertEquals("https://repo.example.com/libs-snapshots-local", repositoryUrl());
    }

    @Test
    void failsNamingTheMissingCredentialPropertiesWhenPublishingRemotely() {
        withEnvironmentVariable(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME, "https://repo.example.com");

        String output = fails(PUBLISH_TASK).getOutput();

        assertTrue(output.contains("gradleInternalAuthHeaderName"), output);
        assertTrue(output.contains("gradleInternalAuthHeaderValue"), output);
    }

    @Test
    void doesNotRequireCredentialsForTasksThatDoNotPublishRemotely() {
        withEnvironmentVariable(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME, "https://repo.example.com");

        // Credentials are only demanded when a task publishing to the repository is in the graph.
        succeeds("build");
    }

    @Test
    void sendsTheTokenAsABearerAuthorizationHeader() {
        startRepositoryServer();
        withEnvironmentVariable(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME, repositoryServerUrl());
        withEnvironmentVariable("ORG_GRADLE_PROJECT_gradleInternalAuthHeaderName", "Authorization");
        withEnvironmentVariable("ORG_GRADLE_PROJECT_gradleInternalAuthHeaderValue", "Bearer test-token");

        succeeds(PUBLISH_TASK);

        assertFalse(receivedPaths.isEmpty(), "no artifacts were uploaded");
        assertTrue(
            receivedPaths.stream().allMatch(path -> path.startsWith("/libs-snapshots-local/")),
            "unexpected upload paths: " + receivedPaths
        );
        assertEquals(
            List.of("Bearer test-token"),
            receivedAuthorizationHeaders.stream().distinct().toList()
        );
    }

    @Test
    void keepsRemotePublishingCompatibleWithTheConfigurationCache() {
        startRepositoryServer();
        withEnvironmentVariable(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME, repositoryServerUrl());
        withEnvironmentVariable("ORG_GRADLE_PROJECT_gradleInternalAuthHeaderName", "Authorization");
        withEnvironmentVariable("ORG_GRADLE_PROJECT_gradleInternalAuthHeaderValue", "Bearer test-token");

        String output = succeeds(PUBLISH_TASK, "--configuration-cache").getOutput();

        assertFalse(
            output.contains("Configuration cache disabled"),
            "publishing opted out of the configuration cache:\n" + output
        );
        assertTrue(output.contains("Configuration cache entry stored"), output);
    }

    @Test
    void leavesProjectsWithoutMavenPublishAlone() {
        write("build.gradle.kts", "plugins { `java-library` }");

        String output = succeeds("tasks", "--all").getOutput();

        assertFalse(output.contains(PUBLISH_TASK), output);
    }

    private void writeBuildScript(String version) {
        write("build.gradle.kts",
            "import org.gradle.api.artifacts.repositories.MavenArtifactRepository",
            "",
            "plugins {",
            "    `java-library`",
            "    `maven-publish`",
            "}",
            "",
            "group = \"org.example\"",
            "version = \"" + version + "\"",
            "",
            "publishing {",
            "    publications.create<MavenPublication>(\"lib\") { from(components[\"java\"]) }",
            "}",
            "",
            // Record the configured repositories so the tests can assert on them. This runs after
            // the script body so the lazily computed URL sees the final project version.
            "afterEvaluate {",
            "    file(\"repositories.txt\").writeText(",
            "        publishing.repositories.filterIsInstance<MavenArtifactRepository>()",
            "            .joinToString(\"\\n\") { \"${it.name}|${it.url}\" }",
            "    )",
            "}"
        );
    }

    private String repositoryUrl() {
        String recorded = assertDoesNotThrow(() -> Files.readString(projectDir.resolve("repositories.txt")));
        List<String> entries = Arrays.stream(recorded.split("\n")).filter(line -> !line.isBlank()).toList();
        assertEquals(1, entries.size(), "expected exactly one repository, got: " + entries);
        String[] nameAndUrl = entries.get(0).split("\\|", 2);
        assertEquals(PublishingConventionsPlugin.REPOSITORY_NAME, nameAndUrl[0]);
        return nameAndUrl[1];
    }

    private void startRepositoryServer() {
        server = assertDoesNotThrow(() ->
            HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        );
        server.createContext("/", this::handle);
        server.start();
    }

    private String repositoryServerUrl() {
        return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (InputStream body = exchange.getRequestBody()) {
            body.readAllBytes();
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            receivedPaths.add(exchange.getRequestURI().getPath());
            receivedAuthorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(201, -1);
        } else {
            // Nothing is published yet, so metadata lookups must report "absent".
            exchange.sendResponseHeaders(404, -1);
        }
        exchange.close();
    }

    private void withEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }

    private Path write(String relativePath, String... lines) {
        try {
            Path targetFile = projectDir.resolve(relativePath);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, Arrays.asList(lines));
            return targetFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BuildResult succeeds(String... args) {
        return runner(args).build();
    }

    private BuildResult fails(String... args) {
        return runner(args).buildAndFail();
    }

    private GradleRunner runner(String... args) {
        Path gradleHomeDir = projectDir.resolve("gradleHome");
        assertDoesNotThrow(() -> Files.createDirectories(gradleHomeDir));

        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        arguments.add("--stacktrace");

        Map<String, String> environment = new LinkedHashMap<>(System.getenv());
        // Keep the plugin's behaviour independent of the environment the tests happen to run in.
        environment.remove(PublishingConventionsPlugin.REPOSITORY_URL_ENV_NAME);
        environment.putAll(environmentVariables);

        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withEnvironment(environment)
            .withPluginClasspath(Stream.of(System.getProperty("java.class.path").split(File.pathSeparator)).map(File::new).toList())
            .withTestKitDir(gradleHomeDir.toFile())
            .forwardOutput()
            .withArguments(arguments);
    }
}
