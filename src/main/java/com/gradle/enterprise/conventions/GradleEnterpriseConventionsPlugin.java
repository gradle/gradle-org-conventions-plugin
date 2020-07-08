package com.gradle.enterprise.conventions;

import com.gradle.enterprise.conventions.customvalueprovider.BuildCacheCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.BuildScanCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.CITagProvider;
import com.gradle.enterprise.conventions.customvalueprovider.GitInformationCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.LocalBuildCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions;
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

    private List<BuildScanCustomValueProvider> createBuildScanCustomValueProviders(GradleEnterpriseConventions gradleEnterpriseConventions) {
        return Arrays.asList(
            new BuildCacheCustomValueProvider(gradleEnterpriseConventions),
            new WatchFilesystemCustomValueProvider(gradleEnterpriseConventions),
            new CITagProvider(gradleEnterpriseConventions),
            new GitHubActionsCustomValueProvider(gradleEnterpriseConventions),
            new JenkinsCustomValueProvider(gradleEnterpriseConventions),
            new TeamCityCustomValueProvider(gradleEnterpriseConventions),
            new TravisCustomValueProvider(gradleEnterpriseConventions),
            new LocalBuildCustomValueProvider(gradleEnterpriseConventions),
            new GitInformationCustomValueProvider(gradleEnterpriseConventions)
        );
    }

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Override
    public void apply(Settings settings) {
        settings.getPlugins().withType(GradleEnterprisePlugin.class, p -> {
            GradleEnterpriseConventions gradleEnterpriseConventions = new GradleEnterpriseConventions(getProviderFactory());
            if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
                settings.buildCache(new BuildCacheConfigureAction(gradleEnterpriseConventions));
            }
            if (!settings.getGradle().getStartParameter().isNoBuildScan()) {
                configureBuildScan(settings, gradleEnterpriseConventions);
            }
        });
    }

    private void configureBuildScan(Settings settings, GradleEnterpriseConventions gradleEnterpriseConventions) {
        BuildScanExtension buildScan = settings.getExtensions().getByType(GradleEnterpriseExtension.class).getBuildScan();

        buildScan.setServer(gradleEnterpriseConventions.getGradleEnterpriseServerUrl());
        buildScan.setCaptureTaskInputFiles(true);
        buildScan.publishAlways();
        ((BuildScanExtensionWithHiddenFeatures) buildScan).publishIfAuthenticated();
        try {
            buildScan.setUploadInBackground(!gradleEnterpriseConventions.isCiServer());
        } catch (NoSuchMethodError e) {
            // GE Plugin version < 3.3. Continue
        }

        createBuildScanCustomValueProviders(gradleEnterpriseConventions).stream()
            .filter(BuildScanCustomValueProvider::isEnabled)
            .forEach(it -> it.accept(settings, buildScan));
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
        private final GradleEnterpriseConventions gradleEnterpriseConventions;

        public BuildCacheConfigureAction(GradleEnterpriseConventions gradleEnterpriseConventions) {
            this.gradleEnterpriseConventions = gradleEnterpriseConventions;
        }

        @Override
        public void execute(BuildCacheConfiguration buildCache) {
            String remoteCacheUrl = determineRemoteCacheUrl();
            boolean remotePush = Boolean.parseBoolean(gradleEnterpriseConventions.getSystemProperty(GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME, "false"));
            String remoteCacheUsername = gradleEnterpriseConventions.getSystemProperty(GRADLE_CACHE_REMOTE_USERNAME_PROPERTY_NAME, "");
            String remoteCachePassword = gradleEnterpriseConventions.getSystemProperty(GRADLE_CACHE_REMOTE_PASSWORD_PROPERTY_NAME, "");
            boolean disableLocalCache = Boolean.parseBoolean(gradleEnterpriseConventions.getSystemProperty("disableLocalCache", "false"));
            buildCache.remote(HttpBuildCache.class, remoteBuildCache -> {
                remoteBuildCache.setUrl(remoteCacheUrl);
                if (!remoteCacheUsername.isEmpty() && !remoteCachePassword.isEmpty()) {
                    remoteBuildCache.setPush(gradleEnterpriseConventions.isCiServer() || remotePush);
                    remoteBuildCache.credentials(credentials -> {
                        credentials.setUsername(remoteCacheUsername);
                        credentials.setPassword(remoteCachePassword);
                    });
                }
            });

            buildCache.local(localBuildCache -> localBuildCache.setEnabled(!disableLocalCache));
        }

        private String determineRemoteCacheUrl() {
            return gradleEnterpriseConventions.systemPropertyProvider(GRADLE_CACHE_REMOTE_URL_PROPERTY_NAME)
                .orElse(gradleEnterpriseConventions.systemPropertyProvider(GRADLE_CACHE_NODE_PROPERTY_NAME)
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
