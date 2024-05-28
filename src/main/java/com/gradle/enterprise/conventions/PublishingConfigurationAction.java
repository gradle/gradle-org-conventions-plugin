package com.gradle.enterprise.conventions;

import com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration;
import org.gradle.api.Action;

public abstract class PublishingConfigurationAction implements Action<BuildScanPublishingConfiguration> {

    public static final PublishingConfigurationAction PUBLISH_IF_AUTHENTICATED = new PublishingConfigurationAction("publishIfAuthenticated") {
        @Override
        public void execute(BuildScanPublishingConfiguration publishing) {
            publishing.onlyIf(BuildScanPublishingConfiguration.PublishingContext::isAuthenticated);
        }
    };

    public static final PublishingConfigurationAction PUBLISH_ON_FAILURE = new PublishingConfigurationAction("publishOnFailure") {
        @Override
        public void execute(BuildScanPublishingConfiguration publishing) {
            publishing.onlyIf(spec -> !spec.getBuildResult().getFailures().isEmpty());
        }
    };
    public static final PublishingConfigurationAction PUBLISH_ALWAYS = new PublishingConfigurationAction("publishOnFailure") {
        @Override
        public void execute(BuildScanPublishingConfiguration publishing) {
            publishing.onlyIf(__ -> true);
        }
    };

    /**
     * Don't apply any publishing strategy, the user will configure in their own init script.
     */
    public static final PublishingConfigurationAction CUSTOM = new PublishingConfigurationAction("custom") {
        @Override
        public void execute(BuildScanPublishingConfiguration publishing) {
        }
    };
    public final String name;

    public PublishingConfigurationAction(String name) {
        this.name = name;
    }
}
