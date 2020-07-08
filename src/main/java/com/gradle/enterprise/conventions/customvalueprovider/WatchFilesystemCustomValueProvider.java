package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.StartParameterInternal;

public class WatchFilesystemCustomValueProvider extends BuildScanCustomValueProvider {
    public WatchFilesystemCustomValueProvider(Utils utils) {
        super(utils);
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        try {
            buildScan.value("watchFileSystem", String.valueOf(((StartParameterInternal) settings.getGradle().getStartParameter()).isWatchFileSystem()));
        } catch (Throwable ignore) {
            // older Gradle
        }
    }
}
