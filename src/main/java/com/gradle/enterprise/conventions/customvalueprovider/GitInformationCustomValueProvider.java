package com.gradle.enterprise.conventions.customvalueprovider;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.initialization.Settings;

import java.io.File;

import static com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions.execAndGetStdout;
import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_BRANCH_NAME;
import static com.gradle.enterprise.conventions.customvalueprovider.ScanCustomValueNames.GIT_STATUS;

public class GitInformationCustomValueProvider extends BuildScanCustomValueProvider {
    public GitInformationCustomValueProvider(GradleEnterpriseConventions conventions) {
        super(conventions);
    }

    @Override
    public void accept(Settings settings, BuildScanExtension buildScan) {
        File rootDir = settings.getRootDir();
        buildScan.background(__ -> {
            execAndGetStdout(rootDir, "git", "status", "--porcelain")
                .ifPresent(output -> {
                    if (!output.isEmpty()) {
                        buildScan.tag("dirty");
                        buildScan.value(GIT_STATUS, output);
                    }
                });

            // https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
            String githubRef = System.getenv("GITHUB_REF");
            String githubHeadRef = System.getenv("GITHUB_HEAD_REF");
            if (githubRef != null) {
                buildScan.value(GIT_BRANCH_NAME, githubRef);
            } else if (githubHeadRef != null) {
                buildScan.value(GIT_BRANCH_NAME, githubHeadRef);
            } else {
                execAndGetStdout(rootDir, "git", "rev-parse", "--abbrev-ref", "HEAD")
                    .ifPresent(output -> buildScan.value(GIT_BRANCH_NAME, output));
            }
        });
    }
}
