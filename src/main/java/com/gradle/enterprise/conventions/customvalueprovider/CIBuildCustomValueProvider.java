package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.util.Collections;

import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.BUILD_ID;
import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_COMMIT_NAME;
import static com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions.getRemoteGitHubRepository;


public abstract class CIBuildCustomValueProvider extends BuildScanCustomValueProvider {
    private final String markEnvVariableName;

    CIBuildCustomValueProvider(String markEnvVariableName, GradleEnterpriseConventions conventions) {
        super(conventions);
        this.markEnvVariableName = markEnvVariableName;
    }

    @Override
    public boolean isEnabled() {
        return getConventions().environmentVariableProvider(markEnvVariableName).isPresent();
    }

    public static class GitHubActionsCustomValueProvider extends CIBuildCustomValueProvider {
        public GitHubActionsCustomValueProvider(GradleEnterpriseConventions conventions) {
            super("GITHUB_ACTIONS", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            String commitId = getConventions().getEnv("GITHUB_SHA");
            buildScan.value(BUILD_ID, String.format("%s %s", getConventions().getEnv("GITHUB_RUN_ID"), getConventions().getEnv("GITHUB_RUN_NUMBER")));
            buildScan.value(GIT_COMMIT_NAME, commitId);
            getConventions().customValueSearchUrl(Collections.singletonMap(GIT_COMMIT_NAME, commitId)).ifPresent(url -> buildScan.link("Git Commit Scans", url));
            buildScan.background(__ ->
                getRemoteGitHubRepository(settings.getRootDir()).ifPresent(repoUrl -> {
                    buildScan.link("GitHub Actions Build", String.format("%s/runs/%s", repoUrl, getConventions().getEnv("GITHUB_RUN_ID")));
                    buildScan.link("Source", String.format("%s/commit/%s", repoUrl, commitId));
                })
            );
        }
    }

    public static class JenkinsCustomValueProvider extends CIBuildCustomValueProvider {
        public JenkinsCustomValueProvider(GradleEnterpriseConventions conventions) {
            super("JENKINS_HOME", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("Jenkins Build", getConventions().getEnv("BUILD_URL"));
            buildScan.value(BUILD_ID, getConventions().getEnv("BUILD_ID"));
            getConventions().setCommitId(settings.getRootDir(), buildScan, getConventions().getEnv("GIT_COMMIT"));
        }
    }

    public static class TeamCityCustomValueProvider extends CIBuildCustomValueProvider {
        public TeamCityCustomValueProvider(GradleEnterpriseConventions conventions) {
            super("TEAMCITY_VERSION", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("TeamCity Build", getConventions().getEnv("BUILD_URL"));
            buildScan.value(BUILD_ID, getConventions().getEnv("BUILD_ID"));
            getConventions().setCommitId(settings.getRootDir(), buildScan, getConventions().getEnv("BUILD_VCS_NUMBER"));
        }
    }

    public static class TravisCustomValueProvider extends CIBuildCustomValueProvider {
        public TravisCustomValueProvider(GradleEnterpriseConventions conventions) {
            super("TRAVIS", conventions);
        }

        @Override
        public void accept(Settings settings, BuildScanExtension buildScan) {
            buildScan.link("Travis Build", getConventions().getEnv("TRAVIS_BUILD_WEB_URL"));
            buildScan.value(BUILD_ID, getConventions().getEnv("TRAVIS_BUILD_ID"));
            getConventions().setCommitId(settings.getRootDir(), buildScan, getConventions().getEnv("TRAVIS_COMMIT"));
        }
    }
}


