package com.gradle.enterprise.conventions;

import com.gradle.enterprise.fixtures.AbstractGradleEnterprisePluginIntegrationTest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GradleEnterpriseConventionsPluginIntegrationTest extends AbstractGradleEnterprisePluginIntegrationTest {
    private static final String EU_CACHE_NODE = "https://eu-build-cache.gradle.org";
    private static final String US_CACHE_NODE = "https://us-build-cache.gradle.org";
    private static final String PUBLIC_GRADLE_ENTERPRISE_SERVER = "https://ge.gradle.org";

    @Test
    public void configureBuildCacheOnlyWhenBuildCacheEnabled() throws URISyntaxException {
        succeeds("help", "--build-cache");

        assertEquals(new URI(EU_CACHE_NODE), getConfiguredRemoteCache().getUrl());
        assertFalse(getConfiguredRemoteCache().isPush());
        assertTrue(getConfiguredLocalCache().isEnabled());
    }

    @Test
    public void configurePublicBuildScanServerIfAgreePublicBuildScanTermOfService() {
        succeeds("help", "-DagreePublicBuildScanTermOfService=yes");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertNull(getConfiguredBuildScan().getServer());
        assertTrue(getConfiguredBuildScan().isCaptureTaskInputFiles());
        assertFalse(getConfiguredBuildScan().isPublishIfAuthenticated());
        assertTrue(getConfiguredBuildScan().isUploadInBackground());
    }

    @Test
    public void configureBuildScanButNotBuildCacheByDefault() {
        succeeds("help");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertTrue(getConfiguredBuildScan().isPublishAlways());
        assertEquals(PUBLIC_GRADLE_ENTERPRISE_SERVER, getConfiguredBuildScan().getServer());
        assertTrue(getConfiguredBuildScan().isCaptureTaskInputFiles());
        assertTrue(getConfiguredBuildScan().isPublishIfAuthenticated());
        assertTrue(getConfiguredBuildScan().isUploadInBackground());
    }

    @Test
    public void configurePublishOnFailure() {
        succeeds("help", "-DpublishStrategy=publishOnFailure", "-Dgradle.enterprise.url=https://ge.gradle.org");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertTrue(getConfiguredBuildScan().isPublishOnFailure());
        assertEquals(PUBLIC_GRADLE_ENTERPRISE_SERVER, getConfiguredBuildScan().getServer());
        assertTrue(getConfiguredBuildScan().isCaptureTaskInputFiles());
        assertTrue(getConfiguredBuildScan().isPublishIfAuthenticated());
        assertTrue(getConfiguredBuildScan().isUploadInBackground());
    }

    @Test
    public void disableBuildScanWithNoBuildScan() {
        withEnvironmentVariable("CI", "1");

        succeeds("help", "--no-scan");

        assertNull(getConfiguredBuildScan().getServer());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CI", "LOCAL"})
    public void disableBuildScanUponPropertiesTask(String env) {
        withEnvironmentVariable(env, "1");

        succeeds("properties");

        assertNull(getConfiguredBuildScan().getServer());
    }

    @Test
    public void disableBuildScanUpSubprojectPropertiesTask() {
        write("settings.gradle", "include(\"subprojectA\")");
        new File(projectDir, "subprojectA").mkdir();

        succeeds(":subprojectA:properties");

        assertNull(getConfiguredBuildScan().getServer());
    }

     @Test
    void configureBuildScanViaSystemProperties() {
        succeeds("help", "-DcacheNode=us", "-Dgradle.enterprise.url=https://ge.mycompany.com");

        assertEquals("https://ge.mycompany.com", getConfiguredBuildScan().getServer());
    }
}
