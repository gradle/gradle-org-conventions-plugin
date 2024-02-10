package com.gradle.enterprise.conventions.customvalueprovider;


import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.io.File;
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
        if (isRunningInIdea()) {
            buildScan.tag("IDEA");
            String ideaVersion = getConventions().getSystemProperty("idea.paths.selector");
            if (ideaVersion != null) {
                buildScan.value(IDEA_VERSION, ideaVersion);
            }
        }
        captureRevisionInBackground(getConventions(), settings, buildScan);
    }

    private boolean isRunningInIdea() {
        return Stream.of("idea.registered", "idea.active", "idea.paths.selector")
            .anyMatch(it -> getConventions().getSystemProperty(it) != null);
    }

    private static void captureRevisionInBackground(
        GradleEnterpriseConventions conventions, Settings settings, BuildScanExtension buildScan
    ) {
        File rootDir = settings.getRootDir(); // Do not capture the whole settings instance or configuration caching may
        // fail.
        buildScan.background(__ ->
            GradleEnterpriseConventions.execAndGetStdout(rootDir, "git", "rev-parse", "HEAD")
                .ifPresent(commitId -> conventions.setCommitId(rootDir, buildScan, commitId)));
    }
}

