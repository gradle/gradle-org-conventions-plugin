package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.Collections;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.BUILD_ID;
import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_COMMIT_NAME;
import static com.gradle.enterprise.conventions.customvalueprovider.Utils.getRemoteGitHubRepository;


public abstract class CIBuildCustomValueProvider extends BuildScanCustomValueProvider {
    private final String markEnvVariableName;

    CIBuildCustomValueProvider(String markEnvVariableName, Utils utils) {
        super(utils);
        this.markEnvVariableName = markEnvVariableName;
    }

    @Override
    public boolean isEnabled() {
        return getUtils().environmentVariableProvider(markEnvVariableName).isPresent();
    }

    public static class GitHubActionsCustomValueProvider extends CIBuildCustomValueProvider {
        public GitHubActionsCustomValueProvider(Utils utils) {
            super("GITHUB_ACTIONS", utils);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            String commitId = getUtils().getEnv("GITHUB_SHA");
            buildScan.value(BUILD_ID, String.format("%s %s", getUtils().getEnv("GITHUB_RUN_ID"), getUtils().getEnv("GITHUB_RUN_NUMBER")));
            buildScan.value(GIT_COMMIT_NAME, commitId);
            buildScan.link("Git Commit Scans", getUtils().customValueSearchUrl(Collections.singletonMap(GIT_COMMIT_NAME, commitId)));
            buildScan.background(__ ->
                getRemoteGitHubRepository(settings.getRootDir()).ifPresent(repoUrl -> {
                    buildScan.link("GitHub Actions Build", String.format("%s/runs/%s", repoUrl, getUtils().getEnv("GITHUB_RUN_ID")));
                    buildScan.link("Source", String.format("%s/commit/%s", repoUrl, commitId));
                })
            );
        }
    }

    public static class JenkinsCustomValueProvider extends CIBuildCustomValueProvider {
        public JenkinsCustomValueProvider(Utils utils) {
            super("JENKINS_HOME", utils);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("Jenkins Build", getUtils().getEnv("BUILD_URL"));
            buildScan.value(BUILD_ID, getUtils().getEnv("BUILD_ID"));
            getUtils().setCommitId(settings.getRootDir(), buildScan, getUtils().getEnv("GIT_COMMIT"));
        }
    }

    public static class TeamCityCustomValueProvider extends CIBuildCustomValueProvider {
        public TeamCityCustomValueProvider(Utils utils) {
            super("TEAMCITY_VERSION", utils);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("TeamCity Build", getUtils().getEnv("BUILD_URL"));
            buildScan.value(BUILD_ID, getUtils().getEnv("BUILD_ID"));
            getUtils().setCommitId(settings.getRootDir(), buildScan, getUtils().getEnv("BUILD_VCS_NUMBER"));
        }
    }

    public static class TravisCustomValueProvider extends CIBuildCustomValueProvider {
        public TravisCustomValueProvider(Utils utils) {
            super("TRAVIS", utils);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("Travis Build", getUtils().getEnv("TRAVIS_BUILD_WEB_URL"));
            buildScan.value(BUILD_ID, getUtils().getEnv("TRAVIS_BUILD_ID"));
            getUtils().setCommitId(settings.getRootDir(), buildScan, getUtils().getEnv("TRAVIS_COMMIT"));
        }
    }
}


