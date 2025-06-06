package com.gradle.enterprise.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
import com.gradle.develocity.agent.gradle.integration.DevelocityIntegrationConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.annotation.Nullable;
import java.io.IOException;

public class DevelocityConfigurationForTest implements DevelocityConfiguration {
    public DevelocityConfigurationForTest() {
        this(null);
    }

    public DevelocityConfigurationForTest(ObjectFactory objectFactory) {
        this.server = objectFactory == null ? null : objectFactory.property(String.class);
        this.buildScanConfiguration = new BuildScanConfigurationForTest(objectFactory);
    }

    private BuildScanConfigurationForTest buildScanConfiguration;
    private Property<String> server;
    private String serverValue;

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

    static class PropertySerializer extends JsonSerializer<Property<?>> {
        @Override
        public void serialize(Property property, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            Object value = property.getOrNull();
            if (value == null) {
                gen.writeNull();
            } else if (value instanceof Boolean) {
                gen.writeBoolean((Boolean) value);
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    @Override
    @JsonSerialize(using = PropertySerializer.class)
    public Property<String> getServer() {
        return server;
    }

    @Override
    @JsonIgnore
    public Property<Boolean> getEdgeDiscovery() {
        throw new UnsupportedOperationException();
    }

    public void setServer(String server) {
        this.serverValue = server;
    }

    public String getServerValue() {
        if (server != null) {
            return server.getOrNull();
        }
        return serverValue;
    }

    @Nullable
    @Override
    @JsonIgnore
    public Property<String> getProjectId() {
        throw new UnsupportedOperationException();
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

    @Override
    @JsonIgnore
    public DevelocityIntegrationConfiguration getIntegration() {
        throw new UnsupportedOperationException();
    }
}
