package com.gradle.develocity.agent.gradle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
import com.gradle.enterprise.fixtures.DevelocityConfigurationForTest;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.model.ObjectFactory;
import org.gradle.caching.BuildCacheEntryReader;
import org.gradle.caching.BuildCacheEntryWriter;
import org.gradle.caching.BuildCacheException;
import org.gradle.caching.BuildCacheKey;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.BuildCacheServiceFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * This is a dummy gradle enterprise plugin for testing. It writes the configuration to json to
 * be verified in integration tests.
 */
public class DevelocityPlugin implements Plugin<Settings> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void apply(Settings settings) {
        ObjectFactory objectFactory = ((GradleInternal) settings.getGradle()).getServices().get(ObjectFactory.class);
        DevelocityConfigurationForTest extension = new DevelocityConfigurationForTest(objectFactory);
        settings.buildCache(cache -> {
            cache.registerBuildCacheService(DevelocityBuildCache.class, DevelocityBuildCacheServiceFactoryForTest.class);
        });
        settings.getExtensions().add("develocityForTest", extension);
        settings.getGradle().afterProject(project -> {
            try {
                Files.write(project.file("develocityConfiguration.json").toPath(), OBJECT_MAPPER.writeValueAsBytes(extension));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static class DevelocityBuildCacheServiceFactoryForTest implements BuildCacheServiceFactory<DevelocityBuildCache> {
        @Override
        public BuildCacheService createBuildCacheService(DevelocityBuildCache buildCache, Describer describer) {
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
