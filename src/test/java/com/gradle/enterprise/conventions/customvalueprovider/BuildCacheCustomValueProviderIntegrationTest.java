package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BuildCacheCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    public void tagCachedIfBuildCacheEnabled(boolean buildCacheEnabled) {
        withPublicGradleEnterpriseUrl();
        succeeds("help", buildCacheEnabled ? "--build-cache" : "--no-build-cache");

        assertEquals(buildCacheEnabled, getConfiguredBuildScan().containsTag("CACHED"));
    }
}
