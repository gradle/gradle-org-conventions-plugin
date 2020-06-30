package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class WatchFilesystemCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    public void addWatchFsCustomValue(Boolean watchFsEnabled) {
        succeeds("help", watchFsEnabled ? "--watch-fs" : "--no-watch-fs");

        Assertions.assertTrue(getConfiguredBuildScan().containsValue("watchFileSystem", watchFsEnabled.toString()));
    }
}
