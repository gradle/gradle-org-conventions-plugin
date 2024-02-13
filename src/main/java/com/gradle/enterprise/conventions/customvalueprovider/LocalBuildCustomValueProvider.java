package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
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
        buildScan.background(addTagsInBackground(getConventions(), settings.getRootDir()));
    }

    private static Action<BuildScanExtension> addTagsInBackground(
        GradleEnterpriseConventions conventions, File rootDir
    ) {
        return buildScan -> {
            addIdeaTags(buildScan, conventions);
            addCommitId(buildScan, conventions, rootDir);
        };
    }

    private static void addIdeaTags(BuildScanExtension buildScan, GradleEnterpriseConventions conventions) {
        if (isRunningInIdea(conventions)) {
            buildScan.tag("IDEA");
            String ideaVersion = conventions.getSystemProperty("idea.paths.selector");
            if (ideaVersion != null) {
                buildScan.value(IDEA_VERSION, ideaVersion);
            }
        }
    }

    private static boolean isRunningInIdea(GradleEnterpriseConventions conventions) {
        return Stream.of("idea.registered", "idea.active", "idea.paths.selector")
            .anyMatch(it -> conventions.getSystemProperty(it) != null);
    }

    private static void addCommitId(
        BuildScanExtension buildScan, GradleEnterpriseConventions conventions, File rootDir
    ) {
        GradleEnterpriseConventions.execAndGetStdout(rootDir, "git", "rev-parse", "HEAD")
            .ifPresent(commitId -> conventions.setCommitId(rootDir, buildScan, commitId));
    }
}

