package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalBuildCustomValueProviderIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    @Test
    public void tagIDEAVersionIfAvailable() {
        succeeds("help", "-Didea.active", "-Didea.paths.selector=2020.1");

        Assertions.assertTrue(getConfiguredBuildScan().containsTag("LOCAL"));
        Assertions.assertTrue(getConfiguredBuildScan().containsTag("IDEA"));
        Assertions.assertTrue(getConfiguredBuildScan().containsValue("IDEA version", "2020.1"));
    }
}
