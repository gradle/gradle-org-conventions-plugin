package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.initialization.Settings;

public class BuildCacheCustomValueProvider extends BuildScanCustomValueProvider {
    public BuildCacheCustomValueProvider(DevelocityConventions conventions) {
        super(conventions);
    }

    @Override
    public void accept(Settings settings, BuildScanConfiguration buildScan) {
        if (settings.getGradle().getStartParameter().isBuildCacheEnabled()) {
            buildScan.tag("CACHED");
        }
    }
}
