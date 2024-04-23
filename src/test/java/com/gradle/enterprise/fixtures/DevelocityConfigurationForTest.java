package com.gradle.enterprise.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.Action;
import org.gradle.api.provider.Property;

import javax.annotation.Nullable;

public class DevelocityConfigurationForTest implements DevelocityConfiguration {
    private BuildScanConfigurationForTest buildScanConfiguration = new BuildScanConfigurationForTest();
    private String server;
    private boolean allowUntrustedServer;
    private String projectId;
    private String accessKey;

    @Override
    public BuildScanConfigurationForTest getBuildScan() {
        return buildScanConfiguration;
    }

    public void setBuildScan(BuildScanConfigurationForTest buildScanConfiguration) {
        this.buildScanConfiguration = buildScanConfiguration;
    }

    @Override
    public void buildScan(Action<? super BuildScanConfiguration> action) {
        action.execute(buildScanConfiguration);
    }

    @Override
    @JsonSerialize(using = ProxyProperty.ProxyPropertySerializer.class)
    public Property<String> getServer() {
        return new ProxyProperty<String>() {
            @Override
            public void set(@Nullable String value) {
                server = value;
            }

            @Override
            public String getOrNull() {
                return server;
            }
        };
    }

    public void setServer(String server) {
        this.server = server;
    }

    @Nullable
    @Override
    @JsonIgnore
    public Property<String> getProjectId() {
        return new ProxyProperty<String>() {
            @Override
            public void set(@Nullable String value) {
                projectId = value;
            }

            @Override
            public String get() {
                return projectId;
            }
        };
    }

    @Override
    @JsonIgnore
    public Property<Boolean> getAllowUntrustedServer() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    @JsonIgnore
    public Property<String> getAccessKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    @JsonIgnore
    public Class<? extends DevelocityBuildCache> getBuildCache() {
        return DevelocityBuildCache.class;
    }
}
