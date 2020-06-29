package com.gradle.enterprise.fixtures;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;

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
    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public boolean getAllowUntrustedServer() {
        return allowUntrustedServer;
    }

    @Override
    public void setAllowUntrustedServer(boolean allowUntrustedServer) {
        this.allowUntrustedServer = allowUntrustedServer;
    }

    public void setBuildScan(BuildScanExtensionForTest buildScanExtensionForTest) {
        this.buildScanExtension = buildScanExtensionForTest;
    }
}
