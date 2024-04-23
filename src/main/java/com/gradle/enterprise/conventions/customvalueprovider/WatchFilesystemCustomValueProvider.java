package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.StartParameterInternal;
import org.gradle.internal.watch.registry.WatchMode;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.WATCH_FILE_SYSTEM;

public class WatchFilesystemCustomValueProvider extends BuildScanCustomValueProvider {
    public WatchFilesystemCustomValueProvider(DevelocityConventions conventions) {
        super(conventions);
    }

    @Override
    public void accept(Settings settings, BuildScanConfiguration buildScan) {
        try {
            buildScan.value(WATCH_FILE_SYSTEM, String.valueOf(getWatchFileSystemMode(settings)));
        } catch (Throwable ignore) {
            // older Gradle
        }
    }

    private static WatchMode getWatchFileSystemMode(Settings settings) {
        return ((StartParameterInternal) settings.getGradle().getStartParameter()).getWatchFileSystemMode();
    }
}
