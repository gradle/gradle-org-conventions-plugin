package io.github.gradle.conventions.customvalueprovider;

import io.github.gradle.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WatchFilesystemCustomValueProviderIntegrationTest extends AbstractDevelocityPluginIntegrationTest {
    @ParameterizedTest
    @CsvSource({
        "true, ENABLED",
        "false, DISABLED"
    })
    void addWatchFsCustomValue(Boolean watchFsEnabled, String status) {
        succeeds("help", watchFsEnabled ? "--watch-fs" : "--no-watch-fs");

        assertTrue(getConfiguredBuildScan().containsValue("watchFileSystem", status));
    }
}
