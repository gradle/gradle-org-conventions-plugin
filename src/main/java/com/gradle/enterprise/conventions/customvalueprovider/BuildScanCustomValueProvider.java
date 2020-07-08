package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.function.BiConsumer;

/**
 * Used to provide custom value for Build Scan.
 */
public abstract class BuildScanCustomValueProvider implements BiConsumer<Settings, BuildScanExtension> {
    private final GradleEnterpriseConventions conventions;

    public BuildScanCustomValueProvider(GradleEnterpriseConventions conventions) {
        this.conventions = conventions;
    }

    public GradleEnterpriseConventions getConventions() {
        return conventions;
    }

    public boolean isEnabled() {
        return true;
    }
}


