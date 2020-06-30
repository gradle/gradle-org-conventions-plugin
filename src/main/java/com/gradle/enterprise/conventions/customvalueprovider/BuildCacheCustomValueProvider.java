package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

public class BuildCacheCustomValueProvider implements BuildScanCustomValueProvider {
    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
            buildScan.tag("CACHED");
        }
    }
}
