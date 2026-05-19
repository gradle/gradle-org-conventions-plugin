package io.github.gradle.conventions;

import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.DevelocityPlugin;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import io.github.gradle.conventions.customvalueprovider.AIAgentCustomValueProvider;
import io.github.gradle.conventions.customvalueprovider.BuildCacheCustomValueProvider;
import io.github.gradle.conventions.customvalueprovider.BuildScanCustomValueProvider;
import io.github.gradle.conventions.customvalueprovider.CIBuildCustomValueProvider.IDESyncCustomValueProvider;
import io.github.gradle.conventions.customvalueprovider.CITagProvider;
import io.github.gradle.conventions.customvalueprovider.DevelocityConventions;
import io.github.gradle.conventions.customvalueprovider.GitInformationCustomValueProvider;
import io.github.gradle.conventions.customvalueprovider.LocalBuildCustomValueProvider;
import io.github.gradle.conventions.customvalueprovider.WatchFilesystemCustomValueProvider;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static io.github.gradle.conventions.PublishingConfigurationAction.CUSTOM;
import static io.github.gradle.conventions.PublishingConfigurationAction.PUBLISH_ALWAYS;
import static io.github.gradle.conventions.PublishingConfigurationAction.PUBLISH_IF_AUTHENTICATED;
import static io.github.gradle.conventions.PublishingConfigurationAction.PUBLISH_ON_FAILURE;
import static io.github.gradle.conventions.customvalueprovider.CIBuildCustomValueProvider.GitHubActionsCustomValueProvider;
import static io.github.gradle.conventions.customvalueprovider.CIBuildCustomValueProvider.TeamCityCustomValueProvider;

public abstract class DevelocityConventionsPlugin implements Plugin<Settings> {
    private List<BuildScanCustomValueProvider> createBuildScanCustomValueProviders(DevelocityConventions conventions) {
        return Arrays.asList(
            new BuildCacheCustomValueProvider(conventions),
            new WatchFilesystemCustomValueProvider(conventions),
            new CITagProvider(conventions),
            new GitHubActionsCustomValueProvider(conventions),
            new TeamCityCustomValueProvider(conventions),
            new LocalBuildCustomValueProvider(conventions),
            new GitInformationCustomValueProvider(conventions),
            new IDESyncCustomValueProvider(conventions),
            new AIAgentCustomValueProvider(conventions)
        );
    }

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Override
    public void apply(Settings settings) {
        settings.getPlugins().withType(DevelocityPlugin.class, p -> {
            DevelocityConventions conventions = new DevelocityConventions(getProviderFactory());
            DevelocityConfiguration dv = settings.getExtensions().getByType(DevelocityConfiguration.class);

            String serverUrl = conventions.getDevelocityServerUrl();
            if (serverUrl != null) {
                dv.getServer().set(serverUrl);
            }

            if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
                settings.buildCache(new BuildCacheConfigureAction(conventions, dv));
                configureEdge(dv, conventions);
            }
            if (!settings.getGradle().getStartParameter().isNoBuildScan() && !containsPropertiesTask(settings)) {
                configureBuildScan(settings, dv, conventions);
            }
        });
    }

    // Disable build scan for security reason
    // https://github.com/gradle/gradle-org-conventions-plugin/issues/9
    private boolean containsPropertiesTask(Settings settings) {
        return settings.getGradle().getStartParameter().getTaskNames().contains("properties")
            || settings.getGradle().getStartParameter().getTaskNames().stream().anyMatch(it -> it.endsWith(":properties"));
    }

    private void configureEdge(DevelocityConfiguration dv, DevelocityConventions conventions) {
        dv.getEdgeDiscovery().set(conventions.getEdgeDiscovery());
    }


    private void configureBuildScan(Settings settings, DevelocityConfiguration dv, DevelocityConventions conventions) {
        BuildScanConfiguration buildScan = dv.getBuildScan();

        if (conventions.getDevelocityServerUrl() == null) {
            // This means `-DagreePublicBuildScanTermOfService=yes` is present
            buildScan.getTermsOfUseAgree().set("yes");
            buildScan.getTermsOfUseUrl().set("https://gradle.com/terms-of-service");
            configurePublishStrategy(conventions, buildScan, PUBLISH_ALWAYS);
        } else {
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
        private static final String DEVELOCITY_ACCESS_KEY = "DEVELOCITY_ACCESS_KEY";
        private static final String GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME = "gradle.cache.remote.push";
        private final DevelocityConventions conventions;
        private final DevelocityConfiguration dv;

        public BuildCacheConfigureAction(DevelocityConventions conventions, DevelocityConfiguration dv) {
            this.conventions = conventions;
            this.dv = dv;
        }

        @Override
        public void execute(BuildCacheConfiguration buildCache) {
            boolean remotePush = Boolean.parseBoolean(conventions.getSystemProperty(GRADLE_CACHE_REMOTE_PUSH_PROPERTY_NAME, "false"));
            String develocityAccessKey = conventions.getEnvVariableThenSystemProperty(DEVELOCITY_ACCESS_KEY, DEVELOCITY_ACCESS_KEY, "");
            boolean disableLocalCache = Boolean.parseBoolean(conventions.getSystemProperty("disableLocalCache", "false"));
            buildCache.remote(dv.getBuildCache(), remoteBuildCache -> {
                remoteBuildCache.setEnabled(true);
                remoteBuildCache.setServer(conventions.getDevelocityServerUrl());
                boolean accessKeySet = notNullOrEmpty(develocityAccessKey);
                boolean push = (conventions.isCiServer() || remotePush) && accessKeySet;
                remoteBuildCache.setPush(push);
            });
            buildCache.local(localBuildCache -> localBuildCache.setEnabled(!disableLocalCache));
        }

        private boolean notNullOrEmpty(String value) {
            return value != null && !value.isEmpty();
        }
    }
}
