package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.function.BiConsumer;

/**
 * Used to provide custom value for Build Scan.
 */
public interface BuildScanCustomValueProvider extends BiConsumer<Settings, BuildScanExtension> {
    default boolean isEnabled() {
        return true;
    }
}


