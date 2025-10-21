package io.github.gradle.conventions.customvalueprovider;

import io.github.gradle.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildCacheCustomValueProviderIntegrationTest extends AbstractDevelocityPluginIntegrationTest {
    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void tagCachedIfBuildCacheEnabled(boolean buildCacheEnabled) {
        succeeds("help", buildCacheEnabled ? "--build-cache" : "--no-build-cache");

        assertEquals(buildCacheEnabled, getConfiguredBuildScan().containsTag("CACHED"));
    }
}
