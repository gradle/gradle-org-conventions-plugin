package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

public class CITagProvider extends BuildScanCustomValueProvider {
    public CITagProvider(GradleEnterpriseConventions gradleEnterpriseConventions) {
        super(gradleEnterpriseConventions);
    }

    @Override
    public boolean isEnabled() {
        return getUtils().isCiServer();
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        buildScan.tag("CI");
    }
}
