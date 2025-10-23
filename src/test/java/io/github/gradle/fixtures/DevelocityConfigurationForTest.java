package io.github.gradle.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
import com.gradle.develocity.agent.gradle.integration.DevelocityIntegrationConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import javax.annotation.Nullable;

public class DevelocityConfigurationForTest implements DevelocityConfiguration {
    public DevelocityConfigurationForTest() {
        this(null);
    }

    public DevelocityConfigurationForTest(ObjectFactory objectFactory) {
        this.server = objectFactory == null ? null : objectFactory.property(String.class);
        this.edgeDiscovery = objectFactory == null ? null : objectFactory.property(Boolean.class);
        this.buildScanConfiguration = new BuildScanConfigurationForTest(objectFactory);
    }


    private BuildScanConfigurationForTest buildScanConfiguration;
    private Property<String> server;
    private String serverValue;
    private Property<Boolean> edgeDiscovery;
    private boolean edgeDiscoveryValue;

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

    static class PropertySerializer extends ValueSerializer<Property<?>> {
        @Override
        public void serialize(Property<?> property, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
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
    @JsonSerialize(using = PropertySerializer.class)
    public Property<Boolean> getEdgeDiscovery() {
        return edgeDiscovery;
    }

    @JsonSetter(nulls = Nulls.SKIP)
    public void setEdgeDiscovery(boolean value) {
        this.edgeDiscoveryValue = value;
    }

    @JsonIgnore
    public boolean getEdgeDiscoveryValue() {
        if (edgeDiscovery != null) {
            return edgeDiscovery.getOrElse(false);
        }
        return edgeDiscoveryValue;
    }

    public void setServer(String server) {
        this.serverValue = server;
    }

    @JsonIgnore
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
