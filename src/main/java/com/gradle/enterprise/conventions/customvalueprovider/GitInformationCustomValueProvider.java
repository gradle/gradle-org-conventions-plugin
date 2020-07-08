package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_BRANCH_NAME;
import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_STATUS;

public class GitInformationCustomValueProvider extends BuildScanCustomValueProvider {
    public GitInformationCustomValueProvider(GradleEnterpriseConventions gradleEnterpriseConventions) {
        super(gradleEnterpriseConventions);
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        buildScan.background(__ -> {
            GradleEnterpriseConventions.execAndGetStdout(settings.getRootDir(), "git", "status", "--porcelain")
                .ifPresent(output -> {
                    if (!output.isEmpty()) {
                        buildScan.tag("dirty");
                        buildScan.value(GIT_STATUS, output);
                    }
                });
            GradleEnterpriseConventions.execAndGetStdout(settings.getRootDir(), "git", "rev-parse", "--abbrev-ref", "HEAD")
                .ifPresent(output -> buildScan.value(GIT_BRANCH_NAME, output));
        });
    }
}
