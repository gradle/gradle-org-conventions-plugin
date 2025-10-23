package io.github.gradle.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.Action;
import org.gradle.api.initialization.Settings;

import java.nio.file.Path;
import java.util.stream.Stream;

import static io.github.gradle.conventions.customvalueprovider.ScanCustomValueNames.IDEA_VERSION;

public class LocalBuildCustomValueProvider extends BuildScanCustomValueProvider {
    public LocalBuildCustomValueProvider(DevelocityConventions conventions) {
        super(conventions);
    }

    @Override
    public boolean isEnabled() {
        return !getConventions().isCiServer();
    }

    @Override
    public void accept(Settings settings, BuildScanConfiguration buildScan) {
        buildScan.tag("LOCAL");
        DevelocityConventions conventions = getConventions();
        buildScan.background(new BackgroundTagsAction(conventions, settings.getRootDir().toPath()));
    }

    private static class BackgroundTagsAction implements Action<BuildScanConfiguration> {
        private final DevelocityConventions conventions;
        private final Path rootDir;

        BackgroundTagsAction(DevelocityConventions conventions, Path rootDir) {
            this.conventions = conventions;
            this.rootDir = rootDir;
        }

        @Override
        public void execute(BuildScanConfiguration buildScan) {
            addIdeaTags(buildScan);
            addCommitId(buildScan);
        }

        private void addIdeaTags(BuildScanConfiguration buildScan) {
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

        private void addCommitId(BuildScanConfiguration buildScan) {
            DevelocityConventions.execAndGetStdout(rootDir, "git", "rev-parse", "HEAD")
                .ifPresent(commitId -> conventions.setCommitId(rootDir, buildScan, commitId));
        }
    }
}

