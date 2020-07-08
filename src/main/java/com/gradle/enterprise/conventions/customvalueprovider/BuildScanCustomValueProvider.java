package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.function.BiConsumer;

/**
 * Used to provide custom value for Build Scan.
 */
public abstract class BuildScanCustomValueProvider implements BiConsumer<Settings, BuildScanExtension> {
    private final Utils utils;

    public BuildScanCustomValueProvider(Utils utils) {
        this.utils = utils;
    }

    public Utils getUtils() {
        return utils;
    }

    public boolean isEnabled() {
        return true;
    }
}


