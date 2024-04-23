package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.initialization.Settings;

public class CITagProvider extends BuildScanCustomValueProvider {
    public CITagProvider(DevelocityConventions conventions) {
        super(conventions);
    }

    @Override
    public boolean isEnabled() {
        return getConventions().isCiServer();
    }

    @Override
    public void accept(Settings settings, BuildScanConfiguration buildScan) {
        buildScan.tag("CI");
    }
}
