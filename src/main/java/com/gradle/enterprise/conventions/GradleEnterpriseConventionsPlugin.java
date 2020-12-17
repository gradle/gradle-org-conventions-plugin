package com.gradle.enterprise.conventions;

import com.gradle.enterprise.conventions.customvalueprovider.BuildCacheCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.BuildScanCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.CITagProvider;
import com.gradle.enterprise.conventions.customvalueprovider.GitInformationCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions;
import com.gradle.enterprise.conventions.customvalueprovider.LocalBuildCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.WatchFilesystemCustomValueProvider;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.GitHubActionsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.JenkinsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TeamCityCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TravisCustomValueProvider;

public abstract class GradleEnterpriseConventionsPlugin implements Plugin<Settings> {

    private List<BuildScanCustomValueProvider> createBuildScanCustomValueProviders(GradleEnterpriseConventions conventions) {
        return Arrays.asList(
            new BuildCacheCustomValueProvider(conventions),
            new WatchFilesystemCustomValueProvider(conventions),
            new CITagProvider(conventions),
            new GitHubActionsCustomValueProvider(conventions),
            new JenkinsCustomValueProvider(conventions),
            new TeamCityCustomValueProvider(conventions),
            new TravisCustomValueProvider(conventions),
            new LocalBuildCustomValueProvider(conventions),
            new GitInformationCustomValueProvider(conventions)
        );
    }

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Override
    public void apply(Settings settings) {
        settings.getPlugins().withType(GradleEnterprisePlugin.class, p -> {
            GradleEnterpriseConventions conventions = new GradleEnterpriseConventions(getProviderFactory());
            if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
                settings.buildCache(new BuildCacheConfigureAction(conventions));
            }
            if (!settings.getGradle().getStartParameter().isNoBuildScan()
                && !containsPropertiesTask(settings)
                && !conventions.getGradleEnterpriseServerUrl().isEmpty()) {
                configureBuildScan(settings, conventions);
            }
        });
    }

    // Disable build scan for security reason
    // https://github.com/gradle/gradle-enterprise-conventions-plugin/issues/9
    private boolean containsPropertiesTask(Settings settings) {
        return settings.getGradle().getStartParameter().getTaskNames().contains("properties")
            || settings.getGradle().getStartParameter().getTaskNames().stream().anyMatch(it -> it.endsWith(":properties"));
    }

    private void configureBuildScan(Settings settings, GradleEnterpriseConventions conventions) {
        BuildScanExtension buildScan = settings.getExtensions().getByType(GradleEnterpriseExtension.class).getBuildScan();

        buildScan.setServer(conventions.getGradleEnterpriseServerUrl());
        buildScan.setCaptureTaskInputFiles(true);
        configurePublishStrategy(conventions, buildScan);
        try {
            buildScan.setUploadInBackground(!conventions.isCiServer());
        } catch (NoSuchMethodError e) {
            // GE Plugin version < 3.3. Continue
        }

        createBuildScanCustomValueProviders(conventions).stream()
            .filter(BuildScanCustomValueProvider::isEnabled)
            .forEach(it -> it.accept(settings, buildScan));
    }

    private void configurePublishStrategy(GradleEnterpriseConventions conventions, BuildScanExtension buildScan) {
        String strategy = conventions.systemPropertyProvider("publishStrategy").orElse("publishAlways").get();
        switch (strategy) {
            case "publishAlways":
                buildScan.publishAlways();
                break;
            case "publishOnFailure":
                buildScan.publishOnFailure();
                break;
            default:
                throw new IllegalStateException("Unknown strategy: " + strategy);
        }

        try {
            buildScan.getClass().getMethod("publishIfAuthenticated").invoke(buildScan);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Could not call publishIfAuthenticated()", e);
        }
    }

    private static class BuildCacheConfigureAction implements Action<BuildCacheConfiguration> {
        private static final String EU_CACHE_NODE = "https://eu-build-cache.gradle.org/cache/";
        private static final String US_CACHE_NODE = "https://us-build-cache.gradle.org/cache/";
        private static final String AU_CACHE_NODE = "https://au-build-cache.gradle.org/cache/";

        private static final String GRADLE_CACHE_REMOTE_URL_PROPERTY_NAME = "gradle.cache.remote.url";
        private static final String GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME = "gradle.cache.remote.push";
        private static final String GRADLE_CACHE_REMOTE_USERNAME_PROPERTY_NAME = "gradle.cache.remote.username";
        private static final String GRADLE_CACHE_REMOTE_PASSWORD_PROPERTY_NAME = "gradle.cache.remote.password";
        private static final String GRADLE_CACHE_NODE_PROPERTY_NAME = "cacheNode";
        private final GradleEnterpriseConventions conventions;

        public BuildCacheConfigureAction(GradleEnterpriseConventions conventions) {
            this.conventions = conventions;
        }

        @Override
        public void execute(BuildCacheConfiguration buildCache) {
            String remoteCacheUrl = determineRemoteCacheUrl();
            boolean remotePush = Boolean.parseBoolean(conventions.getSystemProperty(GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME, "false"));
            String remoteCacheUsername = conventions.getSystemProperty(GRADLE_CACHE_REMOTE_USERNAME_PROPERTY_NAME, "");
            String remoteCachePassword = conventions.getSystemProperty(GRADLE_CACHE_REMOTE_PASSWORD_PROPERTY_NAME, "");
            boolean disableLocalCache = Boolean.parseBoolean(conventions.getSystemProperty("disableLocalCache", "false"));
            buildCache.remote(HttpBuildCache.class, remoteBuildCache -> {
                remoteBuildCache.setUrl(remoteCacheUrl);
                if (!remoteCacheUsername.isEmpty() && !remoteCachePassword.isEmpty()) {
                    remoteBuildCache.setPush(conventions.isCiServer() || remotePush);
                    remoteBuildCache.credentials(credentials -> {
                        credentials.setUsername(remoteCacheUsername);
                        credentials.setPassword(remoteCachePassword);
                    });
                }
            });

            buildCache.local(localBuildCache -> localBuildCache.setEnabled(!disableLocalCache));
        }

        private String determineRemoteCacheUrl() {
            return conventions.systemPropertyProvider(GRADLE_CACHE_REMOTE_URL_PROPERTY_NAME)
                .orElse(conventions.systemPropertyProvider(GRADLE_CACHE_NODE_PROPERTY_NAME)
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
