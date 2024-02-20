package com.gradle.enterprise.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseBuildCache;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;

import javax.annotation.Nullable;

public class GradleEnterpriseExtensionForTest implements GradleEnterpriseExtension {
    private BuildScanExtensionForTest buildScanExtension = new BuildScanExtensionForTest();
    private String server;
    private boolean allowUntrustedServer;
    private String projectId;
    private String accessKey;

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
        this.projectId = projectId;
    }

    @Nullable
    @Override
    public String getProjectId() {
        return this.projectId;
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
        this.accessKey = accessKey;
    }

    @Nullable
    @Override
    public String getAccessKey() {
        return this.accessKey;
    }

    @Override
    @JsonIgnore
    public Class<? extends GradleEnterpriseBuildCache> getBuildCache() {
        return GradleEnterpriseBuildCache.class;
    }

    @Override
    public void setAllowUntrustedServer(boolean allowUntrustedServer) {
        this.allowUntrustedServer = allowUntrustedServer;
    }

    public void setBuildScan(BuildScanExtensionForTest buildScanExtensionForTest) {
        this.buildScanExtension = buildScanExtensionForTest;
    }
}
