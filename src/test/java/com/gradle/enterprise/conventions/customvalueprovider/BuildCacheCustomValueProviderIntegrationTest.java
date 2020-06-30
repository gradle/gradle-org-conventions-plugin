package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class BuildCacheCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    public void tagCachedIfBuildCacheEnabled(boolean buildCacheEnabled) {
        succeeds("help", buildCacheEnabled ? "--build-cache" : "--no-build-cache");

        Assertions.assertEquals(buildCacheEnabled, getConfiguredBuildScan().containsTag("CACHED"));
    }
}
