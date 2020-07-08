package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.StartParameterInternal;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.WATCH_FILE_SYSTEM;

public class WatchFilesystemCustomValueProvider extends BuildScanCustomValueProvider {
    public WatchFilesystemCustomValueProvider(GradleEnterpriseConventions gradleEnterpriseConventions) {
        super(gradleEnterpriseConventions);
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        try {
            buildScan.value(WATCH_FILE_SYSTEM, String.valueOf(((StartParameterInternal) settings.getGradle().getStartParameter()).isWatchFileSystem()));
        } catch (Throwable ignore) {
            // older Gradle
        }
    }
}
