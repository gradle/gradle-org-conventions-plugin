package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.enterprise.conventions.GradleEnterpriseConventionsPlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

public class CITagProvider implements BuildScanCustomValueProvider {
    @Override
    public boolean isEnabled() {
        return GradleEnterpriseConventionsPlugin.isCiServer;
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        buildScan.tag("CI");
    }
}
