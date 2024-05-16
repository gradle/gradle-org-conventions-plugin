package com.gradle.enterprise.conventions;

import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.DevelocityPlugin;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import com.gradle.enterprise.conventions.customvalueprovider.BuildCacheCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.BuildScanCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.CITagProvider;
import com.gradle.enterprise.conventions.customvalueprovider.DevelocityConventions;
import com.gradle.enterprise.conventions.customvalueprovider.GitInformationCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.LocalBuildCustomValueProvider;
import com.gradle.enterprise.conventions.customvalueprovider.WatchFilesystemCustomValueProvider;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static com.gradle.enterprise.conventions.PublishingConfigurationAction.CUSTOM;
import static com.gradle.enterprise.conventions.PublishingConfigurationAction.PUBLISH_ALWAYS;
import static com.gradle.enterprise.conventions.PublishingConfigurationAction.PUBLISH_IF_AUTHENTICATED;
import static com.gradle.enterprise.conventions.PublishingConfigurationAction.PUBLISH_ON_FAILURE;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.GitHubActionsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.JenkinsCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TeamCityCustomValueProvider;
import static com.gradle.enterprise.conventions.customvalueprovider.CIBuildCustomValueProvider.TravisCustomValueProvider;

public abstract class DevelocityConventionsPlugin implements Plugin<Settings> {
    private List<BuildScanCustomValueProvider> createBuildScanCustomValueProviders(DevelocityConventions conventions) {
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
        settings.getPlugins().withType(DevelocityPlugin.class, p -> {
            DevelocityConventions conventions = new DevelocityConventions(getProviderFactory());
            if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
                DevelocityConfiguration dv = settings.getExtensions().getByType(DevelocityConfiguration.class);
                settings.buildCache(new BuildCacheConfigureAction(conventions, dv));
            }
            if (!settings.getGradle().getStartParameter().isNoBuildScan() && !containsPropertiesTask(settings)) {
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


    private void configureBuildScan(Settings settings, DevelocityConventions conventions) {
        DevelocityConfiguration dv = settings.getExtensions().getByType(DevelocityConfiguration.class);
        BuildScanConfiguration buildScan = dv.getBuildScan();

        if (conventions.getDevelocityServerUrl() == null) {
            // This means `-DagreePublicBuildScanTermOfService=yes` is present
            buildScan.getTermsOfUseAgree().set("yes");
            buildScan.getTermsOfUseUrl().set("https://gradle.com/terms-of-service");
            configurePublishStrategy(conventions, buildScan, PUBLISH_ALWAYS);
        } else {
            dv.getServer().set(conventions.getDevelocityServerUrl());
            configurePublishStrategy(conventions, buildScan, PUBLISH_IF_AUTHENTICATED);
        }
        buildScan.capture(buildScanCaptureConfiguration -> buildScanCaptureConfiguration.getFileFingerprints().set(true));
        buildScan.getUploadInBackground().set(!conventions.isCiServer());

        createBuildScanCustomValueProviders(conventions).stream()
            .filter(BuildScanCustomValueProvider::isEnabled)
            .forEach(it -> it.accept(settings, buildScan));
    }

    private void configurePublishStrategy(DevelocityConventions conventions, BuildScanConfiguration buildScan, PublishingConfigurationAction defaultStrategy) {
        String strategy = conventions.getSystemProperty("publishStrategy", defaultStrategy.name);
        switch (strategy) {
            case "publishIfAuthenticated":
                buildScan.publishing(PUBLISH_IF_AUTHENTICATED);
                break;
            case "publishAlways":
                buildScan.publishing(PUBLISH_ALWAYS);
                break;
            case "publishOnFailure":
                buildScan.publishing(PUBLISH_ON_FAILURE);
                break;
            case "custom":
                buildScan.publishing(CUSTOM);
                break;
            default:
                throw new IllegalStateException("Unknown strategy: " + strategy);
        }
    }

    private static class BuildCacheConfigureAction implements Action<BuildCacheConfiguration> {
        private static final String EU_CACHE_NODE = "https://eu-build-cache.gradle.org";
        private static final String US_CACHE_NODE = "https://us-build-cache.gradle.org";
        private static final String AU_CACHE_NODE = "https://au-build-cache.gradle.org";

        private static final String GRADLE_CACHE_REMOTE_SERVER_PROPERTY_NAME = "gradle.cache.remote.server";
        private static final String GRADLE_CACHE_REMOTE_SERVER_ENV_NAME = "GRADLE_CACHE_REMOTE_SERVER";
        private static final String DEVELOCITY_ACCESS_KEY = "DEVELOCITY_ACCESS_KEY";
        private static final String GRADLE_ENTERPRISE_ACCESS_KEY = "GRADLE_ENTERPRISE_ACCESS_KEY";
        private static final String GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME = "gradle.cache.remote.push";
        private static final String GRADLE_CACHE_NODE_PROPERTY_NAME = "cacheNode";
        private final DevelocityConventions conventions;
        private final DevelocityConfiguration dv;

        public BuildCacheConfigureAction(DevelocityConventions conventions, DevelocityConfiguration dv) {
            this.conventions = conventions;
            this.dv = dv;
        }

        @Override
        public void execute(BuildCacheConfiguration buildCache) {
            String remoteCacheUrl = determineRemoteCacheUrl();
            boolean remotePush = Boolean.parseBoolean(conventions.getSystemProperty(GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME, "false"));
            String develocityAccessKey = conventions.getEnvVariableThenSystemProperty(DEVELOCITY_ACCESS_KEY, DEVELOCITY_ACCESS_KEY, "");
            String geAccessKey = conventions.getEnvVariableThenSystemProperty(GRADLE_ENTERPRISE_ACCESS_KEY, GRADLE_ENTERPRISE_ACCESS_KEY, "");
            boolean disableLocalCache = Boolean.parseBoolean(conventions.getSystemProperty("disableLocalCache", "false"));
            buildCache.remote(dv.getBuildCache(), remoteBuildCache -> {
                remoteBuildCache.setEnabled(true);
                remoteBuildCache.setServer(remoteCacheUrl);
                boolean accessKeySet = notNullOrEmpty(develocityAccessKey) || notNullOrEmpty(geAccessKey);
                boolean push = (conventions.isCiServer() || remotePush) && accessKeySet;
                remoteBuildCache.setPush(push);
            });

            buildCache.local(localBuildCache -> localBuildCache.setEnabled(!disableLocalCache));
        }

        private String determineRemoteCacheUrl() {
            return conventions.environmentVariableProvider(GRADLE_CACHE_REMOTE_SERVER_ENV_NAME)
                .orElse(conventions.systemPropertyProvider(GRADLE_CACHE_REMOTE_SERVER_PROPERTY_NAME))
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

        private boolean notNullOrEmpty(String value) {
            return value != null && !value.isEmpty();
        }
    }
}
