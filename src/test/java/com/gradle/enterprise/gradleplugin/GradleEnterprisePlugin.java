package com.gradle.enterprise.gradleplugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradle.enterprise.fixtures.GradleEnterpriseExtensionForTest;
import groovy.json.JsonOutput;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.*;
import org.gradle.caching.configuration.BuildCache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * This is a dummy gradle enterprise plugin for testing. It writes the configuration to json to
 * be verified in integration tests.
 */
public class GradleEnterprisePlugin implements Plugin<Settings> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void apply(Settings settings) {
        GradleEnterpriseExtensionForTest extension = new GradleEnterpriseExtensionForTest();
        settings.buildCache(cache -> {
            cache.registerBuildCacheService(GradleEnterpriseBuildCache.class, GradleEnterpriseBuildCacheServiceFactoryForTest.class);
        });
        settings.getExtensions().add("gradleEnterpriseForTest", extension);
        settings.getGradle().afterProject(project -> {
            try {
                Files.write(project.file("gradleEnterpriseConfiguration.json").toPath(), OBJECT_MAPPER.writeValueAsBytes(extension));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static class GradleEnterpriseBuildCacheServiceFactoryForTest implements BuildCacheServiceFactory<GradleEnterpriseBuildCache> {
        @Override
        public BuildCacheService createBuildCacheService(GradleEnterpriseBuildCache buildCache, Describer describer) {
            return new BuildCacheServiceForTest();
        }
    }

    private static class BuildCacheServiceForTest implements BuildCacheService {
        @Override
        public boolean load(BuildCacheKey key, BuildCacheEntryReader reader) throws BuildCacheException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void store(BuildCacheKey key, BuildCacheEntryWriter writer) throws BuildCacheException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void close() {
        }
    }
}
