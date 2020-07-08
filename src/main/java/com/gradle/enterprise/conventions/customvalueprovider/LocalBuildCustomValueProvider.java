package com.gradle.enterprise.conventions.customvalueprovider;


import com.gradle.enterprise.conventions.GradleEnterpriseConventionsPlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.stream.Stream;

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
        if (Stream.of("idea.registered", "idea.active", "idea.paths.selector").anyMatch(it -> System.getProperties().containsKey(it))) {
            buildScan.tag("IDEA");
            String ideaVersion = System.getProperty("idea.paths.selector");
            if (ideaVersion != null) {
                buildScan.value("IDEA version", ideaVersion);
            }
        }

        Utils.execAndGetStdout(settings.getRootDir(), "git", "log", "-1", "--format=%H")
            .ifPresent(commitId -> Utils.setCommitId(settings.getRootDir(), buildScan, commitId));
    }
}

