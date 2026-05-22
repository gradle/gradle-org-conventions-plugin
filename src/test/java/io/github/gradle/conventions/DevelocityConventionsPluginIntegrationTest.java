package io.github.gradle.conventions;

import io.github.gradle.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DevelocityConventionsPluginIntegrationTest extends AbstractDevelocityPluginIntegrationTest {
    private static final String EU_CACHE_NODE = "https://eun-edge.gradle.org";
    private static final String PUBLIC_DEVELOCITY_SERVER = "https://ge.gradle.org";

    @Test
    void configureBuildCacheOnlyWhenBuildCacheEnabled() throws URISyntaxException {
        succeeds("help", "--build-cache");

        assertTrue(getConfiguredDevelocity().getEdgeDiscoveryValue());
        assertEquals(PUBLIC_DEVELOCITY_SERVER, getConfiguredDevelocity().getServerValue());
        assertEquals(new URI(PUBLIC_DEVELOCITY_SERVER), getConfiguredRemoteCache().getUrl());
        assertFalse(getConfiguredRemoteCache().isPush());
        assertTrue(getConfiguredLocalCache().isEnabled());
    }

    @Test
    void configureBuildCacheWithEdgeNodeUrl() throws URISyntaxException {
        succeeds("help", "--build-cache", "-Ddevelocity.server.url=" + EU_CACHE_NODE);

        assertTrue(getConfiguredDevelocity().getEdgeDiscoveryValue());
        assertEquals(EU_CACHE_NODE, getConfiguredDevelocity().getServerValue());
        assertEquals(new URI(EU_CACHE_NODE), getConfiguredRemoteCache().getUrl());
        assertFalse(getConfiguredRemoteCache().isPush());
        assertTrue(getConfiguredLocalCache().isEnabled());
    }

    @Test
    void configureEdgeDiscoveryFromEnvironmentVariable() {
        withEnvironmentVariable("DEVELOCITY_EDGE_DISCOVERY", "false");

        succeeds("help", "--build-cache");

        assertFalse(getConfiguredDevelocity().getEdgeDiscoveryValue());
    }

    @Test
    void systemPropertyTakesPrecedenceOverEdgeDiscoveryEnvironmentVariable() {
        withEnvironmentVariable("DEVELOCITY_EDGE_DISCOVERY", "false");

        succeeds("help", "--build-cache", "-Ddevelocity.edge.discovery=true");

        assertTrue(getConfiguredDevelocity().getEdgeDiscoveryValue());
    }

    @Test
    void warnWhenUnsupportedCacheNodeSystemPropertyIsSet() {
        String output = succeeds("help", "-DcacheNode=eu").getOutput();

        assertTrue(output.contains("-DcacheNode=eu is no longger supported. Please set prefered location in https://ge.gradle.org/settings/location"));
    }

    @Test
    void configureDevelocityServerUrlFromEnvironmentVariable() {
        withEnvironmentVariable("DEVELOCITY_SERVER_URL", "https://ge.mycompany.com");

        succeeds("help");

        assertEquals("https://ge.mycompany.com", getConfiguredDevelocity().getServerValue());
    }

    @Test
    void systemPropertyTakesPrecedenceOverDevelocityServerUrlEnvironmentVariable() {
        withEnvironmentVariable("DEVELOCITY_SERVER_URL", "https://ge.mycompany.com");

        succeeds("help", "-Ddevelocity.server.url=https://ge.example.com");

        assertEquals("https://ge.example.com", getConfiguredDevelocity().getServerValue());
    }

    @Test
    void configurePublicBuildScanServerIfAgreePublicBuildScanTermOfService() {
        succeeds("help", "-DagreePublicBuildScanTermOfService=yes");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertNull(getConfiguredDevelocity().getServer());
        assertTrue(getConfiguredBuildScan().isCaptureFileFingerprints());
        assertFalse(getConfiguredBuildScan().isPublishIfAuthenticated());
        assertTrue(getConfiguredBuildScan().isUploadInBackground());
    }

    @Test
    void configureBuildScanButNotBuildCacheByDefault() {
        succeeds("help");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertEquals(PUBLIC_DEVELOCITY_SERVER, getConfiguredDevelocity().getServerValue());
        assertTrue(getConfiguredBuildScan().isCaptureFileFingerprints());
        assertTrue(getConfiguredBuildScan().isPublishIfAuthenticated());
        assertTrue(getConfiguredBuildScan().isUploadInBackground());
    }

    @ParameterizedTest
    @ValueSource(strings = {"publishOnFailure", "publishAlways", "custom"})
    void configurePublishStrategy(String strategy) {
        succeeds("help", "-DpublishStrategy=" + strategy, "-Ddevelocity.server.url=https://ge.gradle.org");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertEquals(PUBLIC_DEVELOCITY_SERVER, getConfiguredDevelocity().getServerValue());
        switch (strategy) {
            case "publishOnFailure":
                assertTrue(getConfiguredBuildScan().isPublishOnFailure());
                break;
            case "publishAlways":
                assertTrue(getConfiguredBuildScan().isPublishAlways());
                break;
            case "custom":
                assertTrue(getConfiguredBuildScan().isCustomPublish());
                break;
            default:
                fail("Unexpected test input");
        }
    }

    @Test
    void defaultPublishStrategyIsPublishIfAuthenticated() {
        succeeds("help", "-Ddevelocity.server.url=https://ge.gradle.org");

        assertNull(getConfiguredRemoteCache().getUrl());
        assertTrue(getConfiguredBuildScan().isPublishIfAuthenticated());
        assertEquals(PUBLIC_DEVELOCITY_SERVER, getConfiguredDevelocity().getServerValue());
    }

    @Test
    void disableBuildScanWithNoBuildScan() {
        succeeds("help", "--no-scan");

        // The DV server is still configured because it may be used elsewhere
        assertEquals(PUBLIC_DEVELOCITY_SERVER, getConfiguredDevelocity().getServerValue());
        assertNull(getConfiguredBuildScan().getTermsOfUseUrl());
        assertNull(getConfiguredBuildScan().getTermsOfUseAgree());
        assertNull(getConfiguredBuildScan().getUploadInBackground());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CI", "LOCAL"})
    void disableBuildScanUponPropertiesTask(String env) {
        withEnvironmentVariable(env, "1");

        succeeds("properties");

        assertNull(getConfiguredDevelocity().getServer());
    }

    @Test
    void disableBuildScanUpSubprojectPropertiesTask() {
        write("settings.gradle", "include(\"subprojectA\")");

        subproject("subprojectA");

        succeeds(":subprojectA:properties");

        assertNull(getConfiguredDevelocity().getServer());
    }

    @ParameterizedTest
    @ValueSource(strings = {"develocity.server.url"})
    void configureBuildScanViaSystemProperties(String paramName) {
        succeeds("help", "-D" + paramName + "=https://ge.mycompany.com");

        assertEquals("https://ge.mycompany.com", getConfiguredDevelocity().getServerValue());
    }
}
