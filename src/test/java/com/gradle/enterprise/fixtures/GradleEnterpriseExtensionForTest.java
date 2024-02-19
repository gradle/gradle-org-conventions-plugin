package com.gradle.enterprise.fixtures;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseBuildCache;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;

import javax.annotation.Nullable;

public class GradleEnterpriseExtensionForTest implements GradleEnterpriseExtension {
    private BuildScanExtensionForTest buildScanExtension = new BuildScanExtensionForTest();
    private String server;
    private boolean allowUntrustedServer;

    @Override
    public BuildScanExtensionForTest getBuildScan() {
        return buildScanExtension;
    }

    @Override
    public void buildScan(Action<? super BuildScanExtension> action) {
        action.execute(buildScanExtension);
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public void setProjectId(String projectId) {
        throw new RuntimeException("Not implemented");
    }

    @Nullable
    @Override
    public String getProjectId() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public boolean getAllowUntrustedServer() {
        return allowUntrustedServer;
    }

    @Override
    public void setAccessKey(String accessKey) {
        throw new RuntimeException("Not implemented");
    }

    @Nullable
    @Override
    public String getAccessKey() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Class<? extends GradleEnterpriseBuildCache> getBuildCache() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setAllowUntrustedServer(boolean allowUntrustedServer) {
        this.allowUntrustedServer = allowUntrustedServer;
    }

    public void setBuildScan(BuildScanExtensionForTest buildScanExtensionForTest) {
        this.buildScanExtension = buildScanExtensionForTest;
    }
}
