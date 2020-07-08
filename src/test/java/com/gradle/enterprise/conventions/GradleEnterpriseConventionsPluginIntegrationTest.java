package com.gradle.enterprise.conventions;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GradleEnterpriseConventionsPluginIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    private static final String EU_CACHE_NODE = "https://eu-build-cache.gradle.org/cache/";
    private static final String US_CACHE_NODE = "https://us-build-cache.gradle.org/cache/";
    private static final String PUBLIC_GRADLE_ENTERPRISE_SERVER = "https://ge.gradle.org";

    @Test
    void configureBuildCacheOnlyWhenBuildCacheEnabled() throws URISyntaxException {
        succeeds("help", "--build-cache");

        assertEquals(new URI(EU_CACHE_NODE), getConfiguredRemoteCache().getUrl());
        assertFalse(getConfiguredRemoteCache().isPush());
        assertTrue(getConfiguredLocalCache().isEnabled());
    }

    @Test
    void configureBuildScanButNotBuildCacheByDefault() {
        succeeds("help");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertEquals(PUBLIC_GRADLE_ENTERPRISE_SERVER, getConfiguredBuildScan().getServer());
        assertTrue(getConfiguredBuildScan().isCaptureTaskInputFiles());
        assertTrue(getConfiguredBuildScan().isPublishIfAuthenticated());
        assertTrue(getConfiguredBuildScan().isUploadInBackground());
    }

    @Test
    void disableBuildScanWithNoBuildScan() {
        withEnvironmentVariable("CI", "1");

        succeeds("help", "--no-scan");

        assertNull(getConfiguredBuildScan().getServer());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void configureBuildCacheViaSystemProperties(boolean configurationCacheEnabled) throws URISyntaxException {
        withEnvironmentVariable("CI", "1");

        succeeds("help", "--build-cache", "-DcacheNode=us", "-Dgradle.cache.remote.username=MyUsername", "-Dgradle.cache.remote.password=MyPassword", configurationCacheEnabled ? "--configuration-cache" : "--no-configuration-cache");

        assertEquals(new URI(US_CACHE_NODE), getConfiguredRemoteCache().getUrl());
        assertTrue(getConfiguredRemoteCache().isPush());
        assertEquals("MyUsername", getConfiguredRemoteCache().getCredentials().getUsername());
        assertEquals("MyPassword", getConfiguredRemoteCache().getCredentials().getPassword());
    }

    @Test
    void configureBuildScanViaSystemProperties() {
        succeeds("help", "-DcacheNode=us", "-Dgradle.enterprise.url=https://ge.mycompany.com");

        assertEquals("https://ge.mycompany.com", getConfiguredBuildScan().getServer());
    }
}
