package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WatchFilesystemCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    public void addWatchFsCustomValue(Boolean watchFsEnabled) {
        withPublicGradleEnterpriseUrl();
        succeeds("help", watchFsEnabled ? "--watch-fs" : "--no-watch-fs");

        assertTrue(getConfiguredBuildScan().containsValue("watchFileSystem", watchFsEnabled.toString()));
    }
}
