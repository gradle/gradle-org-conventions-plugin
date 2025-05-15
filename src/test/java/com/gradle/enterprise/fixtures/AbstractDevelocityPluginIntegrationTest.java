package com.gradle.enterprise.fixtures;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.gradle.caching.http.HttpBuildCache;
import org.gradle.caching.local.DirectoryBuildCache;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractDevelocityPluginIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        // Conflict setter: setUrl(URI)/setUrl(String)
        module.addDeserializer(TestHttpBuildCache.class, new JsonDeserializer<TestHttpBuildCache>() {
            @Override
            public TestHttpBuildCache deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                TestHttpBuildCache buildCache = new TestHttpBuildCache();

                JsonNode node = p.getCodec().readTree(p);
                String url = stringOrNull(node.get("url"));
                if (url == null) {
                    buildCache.setUrl((URI) null);
                } else {
                    buildCache.setUrl(url);
                }
                buildCache.setAllowInsecureProtocol(node.get("allowInsecureProtocol").asBoolean());
                buildCache.setAllowUntrustedServer(node.get("allowUntrustedServer").asBoolean());
                buildCache.setPush(node.get("push").asBoolean());
                buildCache.setEnabled(node.get("enabled").asBoolean());

                JsonNode credentialsNode = node.get("credentials");
                if (credentialsNode != null) {
                    buildCache.getCredentials().setUsername(stringOrNull(credentialsNode.get("username")));
                    buildCache.getCredentials().setPassword(stringOrNull(credentialsNode.get("password")));
                }
                return buildCache;
            }

            private String stringOrNull(JsonNode node) {
                return node.isNull() ? null : node.asText();
            }
        });
        OBJECT_MAPPER.registerModules(module);
    }

    @TempDir
    protected File projectDir;

    private final LinkedHashMap<String, String> environmentVariables = new LinkedHashMap<>();

    private HttpBuildCache configuredRemoteCache;
    private DirectoryBuildCache configuredLocalCache;
    private DevelocityConfigurationForTest configuredDevelocity;
    private File gradleHomeDir;

    @BeforeEach
    public void setUp() {
        write("settings.gradle", toString(getClass().getResourceAsStream("/testdata/settings.gradle")));
    }

    /**
     * Write content to a file, relative to project directory.
     */
    protected File write(String relativePath, String... lines) {
        return write(relativePath, Arrays.asList(lines));
    }

    protected File write(String relativePath, List<String> lines) {
        try {
            assertFalse(new File(relativePath).isAbsolute());
            File targetFile = new File(projectDir, relativePath);
            targetFile.getParentFile().mkdirs();
            targetFile.createNewFile();

            List<String> originalLines = new ArrayList<>(Files.readAllLines(targetFile.toPath()));
            originalLines.addAll(lines);

            Files.write(targetFile.toPath(), originalLines);
            return targetFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void withEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }

    private static String toString(InputStream is) {
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    }

    protected void succeeds(String... args) {
        gradleHomeDir = new File(projectDir, "gradleHome");
        assertTrue(gradleHomeDir.mkdirs());

        // Separate tasks and system properties as withJvmArguments is not public API
        // https://github.com/gradle/gradle/issues/1043
        List<String> tasksAndArguments = new ArrayList<>();
        Stream.of(args).filter(s -> !s.startsWith("-D")).forEach(tasksAndArguments::add);
        tasksAndArguments.add("--stacktrace");
        tasksAndArguments.add("--info");
        writeSystemProperties(Stream.of(args).filter(s -> s.startsWith("-D")).collect(Collectors.toList()));

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withEnvironment(buildEnvs())
            .withPluginClasspath(Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(File::new).collect(Collectors.toList()))
            .withTestKitDir(new File(projectDir, "gradleHome"))
            .forwardOutput()
            .withArguments(tasksAndArguments)
            .build();
    }

    private Map<String, String> buildEnvs() {
        Map<String, String> ret = new HashMap<>(System.getenv());
        ret.putAll(environmentVariables);
        if (!environmentVariables.containsKey("CI")) {
            ret.put("CI", "");
        }
        return ret;
    }

    private void writeSystemProperties(List<String> systemProperties) {
        write("gradle.properties",
            systemProperties.stream()
                .map(s -> s.replace("-D", "systemProp."))
                .collect(Collectors.toList())
        );
    }

    /**
     * Returns the configured remote cache for inspection.
     */
    protected HttpBuildCache getConfiguredRemoteCache() {
        if (configuredRemoteCache == null) {
            try {
                String json = toString(new FileInputStream(new File(projectDir, "remoteCacheConfiguration.json")));
                System.out.println("configuredRemoteCache: " + json);
                configuredRemoteCache = OBJECT_MAPPER.readValue(json, TestHttpBuildCache.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return configuredRemoteCache;
    }

    /**
     * Returns the configured local cache for inspection.
     */
    protected DirectoryBuildCache getConfiguredLocalCache() {
        if (configuredLocalCache == null) {
            try {
                String json = toString(new FileInputStream(new File(projectDir, "localCacheConfiguration.json")));
                System.out.println("configuredLocalCache: " + json);
                configuredLocalCache = OBJECT_MAPPER.readValue(json, TestDirectoryBuildCache.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return configuredLocalCache;
    }

    protected DevelocityConfigurationForTest getConfiguredDevelocity() {
        if (configuredDevelocity == null) {
            try {
                String json = toString(new FileInputStream(new File(projectDir, "develocityConfiguration.json")));
                System.out.println("configuredDevelocity: " + json);
                configuredDevelocity = OBJECT_MAPPER.readValue(json, DevelocityConfigurationForTest.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return configuredDevelocity;
    }

    protected BuildScanConfigurationForTest getConfiguredBuildScan() {
        return getConfiguredDevelocity().getBuildScan();
    }

    static class TestHttpBuildCache extends HttpBuildCache {}

    static class TestDirectoryBuildCache extends DirectoryBuildCache {}
}
