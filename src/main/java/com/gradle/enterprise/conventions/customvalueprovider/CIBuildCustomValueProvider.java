package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.initialization.Settings;

import java.util.Collections;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.BUILD_ID;
import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_COMMIT_NAME;


public abstract class CIBuildCustomValueProvider extends BuildScanCustomValueProvider {
    private final String markEnvVariableName;

    CIBuildCustomValueProvider(String markEnvVariableName, DevelocityConventions conventions) {
        super(conventions);
        this.markEnvVariableName = markEnvVariableName;
    }

    @Override
    public boolean isEnabled() {
        return getConventions().environmentVariableProvider(markEnvVariableName).isPresent();
    }

    public static class GitHubActionsCustomValueProvider extends CIBuildCustomValueProvider {
        public GitHubActionsCustomValueProvider(DevelocityConventions conventions) {
            super("GITHUB_ACTIONS", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanConfiguration buildScan) {
            String commitId = getConventions().getEnv("GITHUB_SHA");
            String repo = getConventions().getEnv("GITHUB_REPOSITORY");
            String runId = getConventions().getEnv("GITHUB_RUN_ID");
            String buildUrl = String.format("https://github.com/%s/actions/runs/%s", repo, runId);
            String commitUrl = String.format("https://github.com/%s/commit/%s", repo, commitId);

            buildScan.link("GitHub Actions Build", buildUrl);
            buildScan.link("Source", commitUrl);
            buildScan.value(BUILD_ID, String.format("%s %s", getConventions().getEnv("GITHUB_RUN_ID"), getConventions().getEnv("GITHUB_RUN_NUMBER")));
            buildScan.value(GIT_COMMIT_NAME, commitId);
            getConventions().customValueSearchUrl(Collections.singletonMap(GIT_COMMIT_NAME, commitId)).ifPresent(url -> buildScan.link("Git Commit Scans", url));
        }
    }

    public static class JenkinsCustomValueProvider extends CIBuildCustomValueProvider {
        public JenkinsCustomValueProvider(DevelocityConventions conventions) {
            super("JENKINS_HOME", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanConfiguration buildScan) {
            buildScan.link("Jenkins Build", getConventions().getEnv("BUILD_URL"));
            buildScan.value(BUILD_ID, getConventions().getEnv("BUILD_ID"));
            getConventions().setCommitId(settings.getRootDir(), buildScan, getConventions().getEnv("GIT_COMMIT"));
        }
    }

    public static class TeamCityCustomValueProvider extends CIBuildCustomValueProvider {
        public TeamCityCustomValueProvider(DevelocityConventions conventions) {
            super("TEAMCITY_VERSION", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanConfiguration buildScan) {
            buildScan.link("TeamCity Build", getConventions().getEnv("BUILD_URL"));
            buildScan.value(BUILD_ID, getConventions().getEnv("BUILD_ID"));
            getConventions().setCommitId(settings.getRootDir(), buildScan, getConventions().getEnv("BUILD_VCS_NUMBER"));
        }
    }

    public static class TravisCustomValueProvider extends CIBuildCustomValueProvider {
        public TravisCustomValueProvider(DevelocityConventions conventions) {
            super("TRAVIS", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanConfiguration buildScan) {
            buildScan.link("Travis Build", getConventions().getEnv("TRAVIS_BUILD_WEB_URL"));
            buildScan.value(BUILD_ID, getConventions().getEnv("TRAVIS_BUILD_ID"));
            getConventions().setCommitId(settings.getRootDir(), buildScan, getConventions().getEnv("TRAVIS_COMMIT"));
        }
    }
}


