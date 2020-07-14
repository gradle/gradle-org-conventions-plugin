package com.gradle.enterprise.conventions.customvalueprovider;


import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.stream.Stream;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.IDEA_VERSION;

public class LocalBuildCustomValueProvider extends BuildScanCustomValueProvider {
    public LocalBuildCustomValueProvider(GradleEnterpriseConventions conventions) {
        super(conventions);
    }

    @Override
    public boolean isEnabled() {
        return !getConventions().isCiServer();
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        buildScan.tag("LOCAL");
        if (Stream.of("idea.registered", "idea.active", "idea.paths.selector").anyMatch(it -> getConventions().systemPropertyProvider(it).isPresent())) {
            buildScan.tag("IDEA");
            String ideaVersion = getConventions().systemPropertyProvider("idea.paths.selector").getOrNull();
            if (ideaVersion != null) {
                buildScan.value(IDEA_VERSION, ideaVersion);
            }
        }

        GradleEnterpriseConventions.execAndGetStdout(settings.getRootDir(), "git", "rev-parse", "HEAD")
            .ifPresent(commitId -> getConventions().setCommitId(settings.getRootDir(), buildScan, commitId));
    }
}

