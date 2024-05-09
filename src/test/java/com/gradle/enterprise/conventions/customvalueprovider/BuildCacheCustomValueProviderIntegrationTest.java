package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BuildCacheCustomValueProviderIntegrationTest extends AbstractDevelocityPluginIntegrationTest {
    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    public void tagCachedIfBuildCacheEnabled(boolean buildCacheEnabled) {
        succeeds("help", buildCacheEnabled ? "--build-cache" : "--no-build-cache");

        assertEquals(buildCacheEnabled, getConfiguredBuildScan().containsTag("CACHED"));
    }
}
