package com.gradle.enterprise.conventions;

import com.gradle.enterprise.conventions.customvalueprovider.BuildCacheCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.BuildScanCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.CITagProvider;
import com.gradle.enterprise.conventions.customvalueprovider.GitInformationCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.LocalBuildCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.WatchFilesystemCustomValueProvider;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.internal.api.BuildScanExtensionWithHiddenFeatures;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import java.util.Arrays;
import java.util.List;

import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.GitHubActionsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.JenkinsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TeamCityCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TravisCustomValueProvider;

public class GradleEnterpriseConventionsPlugin implements Plugin<Settings> {
    public static final String PUBLIC_GRADLE_ENTERPRISE_SERVER = "https://ge.gradle.org";
    public static final String EU_CACHE_NODE = "https://eu-build-cache.gradle.org/cache/";
    public static final String US_CACHE_NODE = "https://us-build-cache.gradle.org/cache/";
    public static final String AU_CACHE_NODE = "https://au-build-cache.gradle.org/cache/";
    public static boolean isCiServer = System.getenv().containsKey("CI") && !System.getenv("CI").isEmpty();
    public static String gradleEnterpriseServerUrl = System.getProperty("gradle.enterprise.url", PUBLIC_GRADLE_ENTERPRISE_SERVER);
    public static String remoteCacheUrl = System.getProperty("gradle.cache.remote.url", determineCacheNode());
    public static boolean remotePush = Boolean.getBoolean("gradle.cache.remote.push");
    public static String remoteCacheUsername = System.getProperty("gradle.cache.remote.username", "");
    public static String remoteCachePassword = System.getProperty("gradle.cache.remote.password", "");
    public static boolean disableLocalCache = Boolean.getBoolean("disableLocalCache");

    private List<BuildScanCustomValueProvider> buildScanCustomValueProviders = Arrays.asList(
        new BuildCacheCustomValueProvider(),
        new WatchFilesystemCustomValueProvider(),
        new CITagProvider(),
        new GitHubActionsCustomValueProvider(),
        new JenkinsCustomValueProvider(),
        new TeamCityCustomValueProvider(),
        new TravisCustomValueProvider(),
        new LocalBuildCustomValueProvider(),
        new GitInformationCustomValueProvider()
    );

    @Override
    public void apply(Settings settings) {
        settings.getPlugins().withType(GradleEnterprisePlugin.class, p -> {
            if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
                settings.buildCache(new BuildCacheConfigureAction());
            }
            if (!settings.getGradle().getStartParameter().isNoBuildScan()) {
                configureBuildScan(settings);
            }
        });
    }

    private void configureBuildScan(Settings settings) {
        BuildScanExtension buildScan = settings.getExtensions().getByType(GradleEnterpriseExtension.class).getBuildScan();

        buildScan.setServer(gradleEnterpriseServerUrl);
        buildScan.setCaptureTaskInputFiles(true);
        buildScan.publishAlways();
        ((BuildScanExtensionWithHiddenFeatures) buildScan).publishIfAuthenticated();
        try {
            buildScan.setUploadInBackground(!isCiServer);
        } catch (NoSuchMethodError e) {
            // GE Plugin version < 3.3. Continue
        }

        buildScanCustomValueProviders.stream()
            .filter(BuildScanCustomValueProvider::isEnabled)
            .forEach(it -> it.accept(settings, buildScan));
    }

    private static String determineCacheNode() {
        String cacheNode = System.getProperty("cacheNode", "eu");
        switch (cacheNode) {
            case "eu":
                return EU_CACHE_NODE;
            case "us":
                return US_CACHE_NODE;
            case "au":
                return AU_CACHE_NODE;
            default:
                throw new IllegalArgumentException("Unrecognized cacheNode: " + cacheNode);
        }

    }

    static class BuildCacheConfigureAction implements Action<BuildCacheConfiguration> {
        @Override
        public void execute(BuildCacheConfiguration buildCache) {
            buildCache.remote(HttpBuildCache.class, remoteBuildCache -> {
                remoteBuildCache.setUrl(remoteCacheUrl);
                if (!remoteCacheUsername.isEmpty() && !remoteCachePassword.isEmpty()) {
                    remoteBuildCache.setPush(isCiServer || remotePush);
                    remoteBuildCache.credentials(credentials -> {
                        credentials.setUsername(remoteCacheUsername);
                        credentials.setPassword(remoteCachePassword);
                    });
                }
            });

            buildCache.local(localBuildCache -> localBuildCache.setEnabled(!disableLocalCache));
        }
    }
}
