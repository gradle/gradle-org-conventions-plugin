package com.gradle.enterprise.conventions;

import com.gradle.enterprise.conventions.customvalueprovider.BuildCacheCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.BuildScanCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.CITagProvider;
import com.gradle.enterprise.conventions.customvalueprovider.GitInformationCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.LocalBuildCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.Utils;
import com.gradle.enterprise.conventions.customvalueprovider.WatchFilesystemCustomValueProvider;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.internal.api.BuildScanExtensionWithHiddenFeatures;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.GitHubActionsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.JenkinsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TeamCityCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TravisCustomValueProvider;

public abstract class GradleEnterpriseConventionsPlugin implements Plugin<Settings> {
    public static final String PUBLIC_GRADLE_ENTERPRISE_SERVER = "https://ge.gradle.org";
    public static boolean isCiServer = System.getenv().containsKey("CI") && !System.getenv("CI").isEmpty();
    private static final String GRADLE_ENTERPRISE_URL_PROPERTY_NAME = "gradle.enterprise.url";
    public static String gradleEnterpriseServerUrl = System.getProperty(GRADLE_ENTERPRISE_URL_PROPERTY_NAME, PUBLIC_GRADLE_ENTERPRISE_SERVER);

    private List<BuildScanCustomValueProvider> createBuildScanCustomValueProviders() {
        Utils utils = new Utils(getProviderFactory());
        return Arrays.asList(
            new BuildCacheCustomValueProvider(utils),
            new WatchFilesystemCustomValueProvider(utils),
            new CITagProvider(utils),
            new GitHubActionsCustomValueProvider(utils),
            new JenkinsCustomValueProvider(utils),
            new TeamCityCustomValueProvider(utils),
            new TravisCustomValueProvider(utils),
            new LocalBuildCustomValueProvider(utils),
            new GitInformationCustomValueProvider(utils)
        );
    }

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Override
    public void apply(Settings settings) {
        settings.getPlugins().withType(GradleEnterprisePlugin.class, p -> {
            if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
                settings.buildCache(new BuildCacheConfigureAction(getProviderFactory()));
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

        createBuildScanCustomValueProviders().stream()
            .filter(BuildScanCustomValueProvider::isEnabled)
            .forEach(it -> it.accept(settings, buildScan));
    }

    private String getSystemProperty(String name, String defaultValue) {
        return getProviderFactory().systemProperty(name).forUseAtConfigurationTime().orElse(defaultValue).get();
    }

    private class BuildCacheConfigureAction implements Action<BuildCacheConfiguration> {
        private static final String EU_CACHE_NODE = "https://eu-build-cache.gradle.org/cache/";
        private static final String US_CACHE_NODE = "https://us-build-cache.gradle.org/cache/";
        private static final String AU_CACHE_NODE = "https://au-build-cache.gradle.org/cache/";

        private static final String GRADLE_CACHE_REMOTE_URL_PROPERTY_NAME = "gradle.cache.remote.url";
        private static final String GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME = "gradle.cache.remote.push";
        private static final String GRADLE_CACHE_REMOTE_USERNAME_PROPERTY_NAME = "gradle.cache.remote.username";
        private static final String GRADLE_CACHE_REMOTE_PASSWORD_PROPERTY_NAME = "gradle.cache.remote.password";
        private static final String GRADLE_CACHE_NODE_PROPERTY_NAME = "cacheNode";
        private final ProviderFactory providerFactory;

        public BuildCacheConfigureAction(ProviderFactory providerFactory) {
            this.providerFactory = providerFactory;
        }

        @Override
        public void execute(BuildCacheConfiguration buildCache) {
            String remoteCacheUrl = determineRemoteCacheUrl();
            boolean remotePush = Boolean.parseBoolean(getSystemProperty(GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME, "false"));
            String remoteCacheUsername = getSystemProperty(GRADLE_CACHE_REMOTE_USERNAME_PROPERTY_NAME, "");
            String remoteCachePassword = getSystemProperty(GRADLE_CACHE_REMOTE_PASSWORD_PROPERTY_NAME, "");
            boolean disableLocalCache = Boolean.parseBoolean(getSystemProperty("disableLocalCache", "false"));
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

        private String determineRemoteCacheUrl() {
            return providerFactory.systemProperty(GRADLE_CACHE_REMOTE_URL_PROPERTY_NAME).forUseAtConfigurationTime()
                .orElse(providerFactory.systemProperty(GRADLE_CACHE_NODE_PROPERTY_NAME).forUseAtConfigurationTime()
                    .map(cacheNode -> {
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
                    })).orElse(EU_CACHE_NODE).get();
        }
    }
}
