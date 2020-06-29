package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import static com.gradle.enterprise.conventions.customvalueprovider.Utils.GIT_COMMIT_NAME;
import static com.gradle.enterprise.conventions.customvalueprovider.Utils.customValueSearchUrl;
import static com.gradle.enterprise.conventions.customvalueprovider.Utils.getRemoteGitHubRepository;
import static com.gradle.enterprise.conventions.customvalueprovider.Utils.mapOf;
import static com.gradle.enterprise.conventions.customvalueprovider.Utils.setCommitId;


public abstract class CIBuildCustomValueProvider implements BuildScanCustomValueProvider {
    private final String markEnvVariableName;

    CIBuildCustomValueProvider(String markEnvVariableName) {
        this.markEnvVariableName = markEnvVariableName;
    }

    @Override
    public boolean isEnabled() {
        return System.getenv().containsKey(markEnvVariableName);
    }

    public static class GitHubActionsCustomValueProvider extends CIBuildCustomValueProvider {
        public GitHubActionsCustomValueProvider() {
            super("GITHUB_ACTIONS");
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            String commitId = System.getenv("GITHUB_SHA");
            buildScan.value("Build ID", String.format("%s %s", System.getenv("GITHUB_RUN_ID"), System.getenv("GITHUB_RUN_NUMBER")));
            buildScan.value(GIT_COMMIT_NAME, commitId);
            buildScan.link("Git Commit Scans", customValueSearchUrl(mapOf(GIT_COMMIT_NAME, commitId)));
            buildScan.background(__ ->
                getRemoteGitHubRepository(settings.getRootDir()).ifPresent(repoUrl -> {
                    buildScan.link("GitHub Actions Build", String.format("%s/runs/%s", repoUrl, System.getenv("GITHUB_RUN_ID")));
                    buildScan.link("Source", String.format("%s/commit/%s", repoUrl, commitId));
                })
            );
        }
    }

    public static class JenkinsCustomValueProvider extends CIBuildCustomValueProvider {
        public JenkinsCustomValueProvider() {
            super("JENKINS_HOME");
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("Jenkins Build", System.getenv("BUILD_URL"));
            buildScan.value("Build ID", System.getenv("BUILD_ID"));
            setCommitId(settings.getRootDir(), buildScan, System.getenv("GIT_COMMIT"));
        }
    }

    public static class TeamCityCustomValueProvider extends CIBuildCustomValueProvider {
        public TeamCityCustomValueProvider() {
            super("TEAMCITY_VERSION");
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("TeamCity Build", System.getenv("BUILD_URL"));
            buildScan.value("Build ID", System.getenv("BUILD_ID"));
            setCommitId(settings.getRootDir(), buildScan, System.getenv("BUILD_VCS_NUMBER"));
        }
    }

    public static class TravisCustomValueProvider extends CIBuildCustomValueProvider {
        public TravisCustomValueProvider() {
            super("TRAVIS");
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("Travis Build", System.getenv("TRAVIS_BUILD_WEB_URL"));
            buildScan.value("Build ID", System.getenv("TRAVIS_BUILD_ID"));
            setCommitId(settings.getRootDir(), buildScan, System.getenv("TRAVIS_COMMIT"));
        }
    }
}


