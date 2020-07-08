package com.gradle.enterprise.conventions.customvalueprovider;


import com.gradle.enterprise.conventions.GradleEnterpriseConventionsPlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.stream.Stream;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.IDEA_VERSION;

public class LocalBuildCustomValueProvider extends BuildScanCustomValueProvider {
    public LocalBuildCustomValueProvider(Utils utils) {
        super(utils);
    }

    @Override
    public boolean isEnabled() {
        return !GradleEnterpriseConventionsPlugin.isCiServer;
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        buildScan.tag("LOCAL");
        if (Stream.of("idea.registered", "idea.active", "idea.paths.selector").anyMatch(it -> getUtils().systemPropertyProvider(it).isPresent())) {
            buildScan.tag("IDEA");
            String ideaVersion = getUtils().systemPropertyProvider("idea.paths.selector").getOrNull();
            if (ideaVersion != null) {
                buildScan.value(IDEA_VERSION, ideaVersion);
            }
        }

        Utils.execAndGetStdout(settings.getRootDir(), "git", "log", "-1", "--format=%H")
            .ifPresent(commitId -> getUtils().setCommitId(settings.getRootDir(), buildScan, commitId));
    }
}

