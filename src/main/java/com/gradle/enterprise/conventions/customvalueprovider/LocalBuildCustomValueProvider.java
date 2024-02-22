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
        GradleEnterpriseConventions conventions = getConventions();
        File rootDir = settings.getRootDir();
        buildScan.background(new BackgroundTagsAction(conventions, rootDir));
    }

    private static class BackgroundTagsAction implements Action<BuildScanExtension> {
        private final GradleEnterpriseConventions conventions;
        private final File rootDir;

        BackgroundTagsAction(GradleEnterpriseConventions conventions, File rootDir) {
            this.conventions = conventions;
            this.rootDir = rootDir;
        }

        @Override
        public void execute(BuildScanExtension buildScan) {
            addIdeaTags(buildScan);
            addCommitId(buildScan);
        }

        private void addIdeaTags(BuildScanExtension buildScan) {
            if (isRunningInIdea()) {
                buildScan.tag("IDEA");
                String ideaVersion = conventions.getSystemProperty("idea.paths.selector");
                if (ideaVersion != null) {
                    buildScan.value(IDEA_VERSION, ideaVersion);
                }
            }
        }

        private boolean isRunningInIdea() {
            return Stream.of("idea.registered", "idea.active", "idea.paths.selector")
                .anyMatch(it -> conventions.getSystemProperty(it) != null);
        }

        private void addCommitId(BuildScanExtension buildScan) {
            GradleEnterpriseConventions.execAndGetStdout(rootDir, "git", "rev-parse", "HEAD")
                .ifPresent(commitId -> conventions.setCommitId(rootDir, buildScan, commitId));
        }
    }
}

