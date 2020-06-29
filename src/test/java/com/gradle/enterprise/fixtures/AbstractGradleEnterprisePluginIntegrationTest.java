package com.gradle.enterprise.fixtures;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.io.IOUtils;
import org.gradle.caching.http.HttpBuildCache;
import org.gradle.caching.local.DirectoryBuildCache;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
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

public class AbstractGradleEnterprisePluginIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();
        // Conflict setter: setUrl(URI)/setUrl(String)
        module.addDeserializer(HttpBuildCache.class, new JsonDeserializer<HttpBuildCache>() {
            @Override
            public HttpBuildCache deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                HttpBuildCache buildCache = new HttpBuildCache();

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
    private GradleEnterpriseExtensionForTest configuredGradleEnterprise;
    private File gradleHomeDir;

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
            Files.write(targetFile.toPath(), lines);
            return targetFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void withEnvironmentVariable(String key, String value) {
        environmentVariables.put(key, value);
    }

    protected void succeeds(String... args) {
        try {
            write("settings.gradle", IOUtils.toString(getClass().getResourceAsStream("/testdata/settings.gradle"), Charset.defaultCharset()));
            gradleHomeDir = new File(projectDir, "gradleHome");
            Assertions.assertTrue(gradleHomeDir.mkdirs());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

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
                configuredRemoteCache = OBJECT_MAPPER.readValue(new File(projectDir, "remoteCacheConfiguration.json"), HttpBuildCache.class);
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
                configuredLocalCache = OBJECT_MAPPER.readValue(new File(projectDir, "localCacheConfiguration.json"), DirectoryBuildCache.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return configuredLocalCache;
    }

    protected GradleEnterpriseExtensionForTest getConfiguredGradleEnterprise() {
        if (configuredGradleEnterprise == null) {
            try {
                configuredGradleEnterprise = OBJECT_MAPPER.readValue(new File(projectDir, "gradleEnterpriseConfiguration.json"), GradleEnterpriseExtensionForTest.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return configuredGradleEnterprise;
    }

    protected BuildScanExtensionForTest getConfiguredBuildScan() {
        return getConfiguredGradleEnterprise().getBuildScan();
    }
}
