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
import com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionInternal;
import com.gradle.scan.plugin.BuildScanCaptureSettings;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;

import javax.inject.Inject;
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
                GradleEnterpriseExtension ge = settings.getExtensions().getByType(GradleEnterpriseExtension.class);
                settings.buildCache(new BuildCacheConfigureAction(conventions, ge));
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

    private void configureBuildScan(Settings settings, GradleEnterpriseConventions conventions) {
        BuildScanExtension buildScan = settings.getExtensions().getByType(GradleEnterpriseExtension.class).getBuildScan();

        // This means `-DagreePublicBuildScanTermOfService=yes` is present
        if (conventions.getGradleEnterpriseServerUrl() == null) {
            buildScan.setTermsOfServiceUrl("https://gradle.com/terms-of-service");
            buildScan.setTermsOfServiceAgree("yes");
        } else {
            buildScan.setServer(conventions.getGradleEnterpriseServerUrl());
            publishIfAuthenticated(buildScan);
        }
        buildScan.capture(new Action<BuildScanCaptureSettings>() {
            @Override
            public void execute(BuildScanCaptureSettings buildScanCaptureSettings) {
                buildScanCaptureSettings.setTaskInputFiles(true);
            }
        });
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

    private void publishIfAuthenticated(BuildScanExtension buildScan) {
        try {
            ((BuildScanExtensionInternal)buildScan).publishIfAuthenticated();
        } catch (ClassCastException e) {
            throw new IllegalStateException("Could not call publishIfAuthenticated()", e);
        }
    }

    private void configurePublishStrategy(GradleEnterpriseConventions conventions, BuildScanExtension buildScan) {
        String strategy = conventions.getSystemProperty("publishStrategy", "publishAlways");
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
    }

    private static class BuildCacheConfigureAction implements Action<BuildCacheConfiguration> {
        private static final String EU_CACHE_NODE = "https://eu-build-cache.gradle.org/cache/";
        private static final String US_CACHE_NODE = "https://us-build-cache.gradle.org/cache/";
        private static final String AU_CACHE_NODE = "https://au-build-cache.gradle.org/cache/";

        private static final String GRADLE_CACHE_REMOTE_URL_PROPERTY_NAME = "gradle.cache.remote.url";
        private static final String GRADLE_CACHE_REMOTE_URL_ENV_NAME = "GRADLE_CACHE_REMOTE_URL";
        private static final String DEVELOCITY_ACCESS_KEY = "DEVELOCITY_ACCESS_KEY";
        private static final String GRADLE_ENTERPRISE_ACCESS_KEY = "GRADLE_ENTERPRISE_ACCESS_KEY";
        private static final String GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME = "gradle.cache.remote.push";
        private static final String GRADLE_CACHE_NODE_PROPERTY_NAME = "cacheNode";
        private final GradleEnterpriseConventions conventions;
        private final GradleEnterpriseExtension ge;

        public BuildCacheConfigureAction(GradleEnterpriseConventions conventions, GradleEnterpriseExtension ge) {
            this.conventions = conventions;
            this.ge = ge;
        }

        @Override
        public void execute(BuildCacheConfiguration buildCache) {
            String remoteCacheUrl = determineRemoteCacheUrl();
            boolean remotePush = Boolean.parseBoolean(conventions.getSystemProperty(GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME, "false"));
            String develocityAccessKey = conventions.getEnvVariableThenSystemProperty(DEVELOCITY_ACCESS_KEY, DEVELOCITY_ACCESS_KEY, "");
            String geAccessKey = conventions.getEnvVariableThenSystemProperty(GRADLE_ENTERPRISE_ACCESS_KEY, GRADLE_ENTERPRISE_ACCESS_KEY, "");
            boolean disableLocalCache = Boolean.parseBoolean(conventions.getSystemProperty("disableLocalCache", "false"));
            buildCache.remote(ge.getBuildCache(), remoteBuildCache -> {
                remoteBuildCache.setEnabled(true);
                remoteBuildCache.setServer(remoteCacheUrl);
                boolean accessKeySet = notNullOrEmpty(develocityAccessKey) || notNullOrEmpty(geAccessKey);
                boolean push = (conventions.isCiServer() || remotePush) && accessKeySet;
                remoteBuildCache.setPush(push);
            });

            buildCache.local(localBuildCache -> localBuildCache.setEnabled(!disableLocalCache));
        }

        private String determineRemoteCacheUrl() {
            return conventions.environmentVariableProvider(GRADLE_CACHE_REMOTE_URL_ENV_NAME)
                .orElse(conventions.systemPropertyProvider(GRADLE_CACHE_REMOTE_URL_PROPERTY_NAME))
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
