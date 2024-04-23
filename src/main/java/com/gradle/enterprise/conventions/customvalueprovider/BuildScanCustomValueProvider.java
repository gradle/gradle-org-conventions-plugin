package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.initialization.Settings;

import java.util.function.BiConsumer;

/**
 * Used to provide custom value for Build Scan.
 */
public abstract class BuildScanCustomValueProvider implements BiConsumer<Settings, BuildScanConfiguration> {
    private final DevelocityConventions conventions;

    public BuildScanCustomValueProvider(DevelocityConventions conventions) {
        this.conventions = conventions;
    }

    public DevelocityConventions getConventions() {
        return conventions;
    }

    public boolean isEnabled() {
        return true;
    }
}


