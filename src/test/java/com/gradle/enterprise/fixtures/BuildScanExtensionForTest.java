package com.gradle.enterprise.fixtures;

import com.gradle.scan.plugin.BuildResult;
import com.gradle.scan.plugin.BuildScanCaptureSettings;
import com.gradle.scan.plugin.BuildScanDataObfuscation;
import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.PublishedBuildScan;
import org.gradle.api.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuildScanExtensionForTest implements BuildScanExtension {
    private List<String> tags = new ArrayList<>();
    private List<List<String>> values = new ArrayList<>();
    private List<List<String>> links = new ArrayList<>();
    private List<String> backgroundTags = new ArrayList<>();
    private List<List<String>> backgroundValues = new ArrayList<>();
    private List<List<String>> backgroundLinks = new ArrayList<>();
    private String termsOfServiceUrl;
    private String termsOfServiceAgree;
    private String server;
    private boolean allowUntrustedServer;
    private boolean publishAlways;
    private boolean publishOnFailure;
    private boolean uploadInBackground;
    private boolean captureTaskInputFiles;
    private boolean publishIfAuthenticated;

    private boolean inBackground;

    public boolean containsTag(String tag) {
        return tags.contains(tag);
    }

    public boolean containsValue(String key) {
        return values.stream().anyMatch(it -> it.get(0).equals(key));
    }

    public boolean containsValue(String key, String value) {
        return values.contains(Arrays.asList(key, value));
    }

    public boolean containsLink(String name, String link) {
        return links.contains(Arrays.asList(name, link));
    }

    public boolean containsLink(String name) {
        return links.stream().anyMatch(it -> it.get(0).equals(name));
    }

    public boolean containsBackgroundTag(String tag) {
        return backgroundTags.contains(tag);
    }

    public boolean containsBackgroundValue(String key) {
        return backgroundValues.stream().anyMatch(it -> it.get(0).equals(key));
    }

    public boolean containsBackgroundValue(String key, String value) {
        return backgroundValues.contains(Arrays.asList(key, value));
    }

    public boolean containsBackgroundLink(String name, String link) {
        return backgroundLinks.contains(Arrays.asList(name, link));
    }

    public boolean containsBackgroundLink(String name) {
        return backgroundLinks.stream().anyMatch(it -> it.get(0).equals(name));
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<List<String>> getValues() {
        return values;
    }

    public void setValues(List<List<String>> values) {
        this.values = values;
    }

    public List<List<String>> getLinks() {
        return links;
    }

    public void setLinks(List<List<String>> links) {
        this.links = links;
    }

    public List<String> getBackgroundTags() {
        return backgroundTags;
    }

    public void setBackgroundTags(List<String> backgroundTags) {
        this.backgroundTags = backgroundTags;
    }

    public List<List<String>> getBackgroundValues() {
        return backgroundValues;
    }

    public void setBackgroundValues(List<List<String>> backgroundValues) {
        this.backgroundValues = backgroundValues;
    }

    public List<List<String>> getBackgroundLinks() {
        return backgroundLinks;
    }

    public void setBackgroundLinks(List<List<String>> backgroundLinks) {
        this.backgroundLinks = backgroundLinks;
    }

    @Override
    public void background(Action<? super BuildScanExtension> action) {
        inBackground = true;
        action.execute(this);
        inBackground = false;
    }

    @Override
    public void tag(String tag) {
        if (inBackground) {
            backgroundTags.add(tag);
        } else {
            tags.add(tag);
        }
    }

    @Override
    public void value(String name, String value) {
        if (inBackground) {
            backgroundValues.add(Arrays.asList(name, value));
        } else {
            values.add(Arrays.asList(name, value));
        }
    }

    @Override
    public void link(String name, String url) {
        if (inBackground) {
            backgroundLinks.add(Arrays.asList(name, url));
        } else {
            links.add(Arrays.asList(name, url));
        }
    }

    @Override
    public void buildFinished(Action<? super BuildResult> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void buildScanPublished(Action<? super PublishedBuildScan> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        this.termsOfServiceUrl = termsOfServiceUrl;
    }

    @Override
    public String getTermsOfServiceUrl() {
        return termsOfServiceUrl;
    }

    @Override
    public void setTermsOfServiceAgree(String agree) {
        this.termsOfServiceAgree = agree;
    }

    @Override
    public String getTermsOfServiceAgree() {
        return termsOfServiceAgree;
    }

    @Override
    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public void setAllowUntrustedServer(boolean allow) {
        this.allowUntrustedServer = allow;
    }

    @Override
    public boolean getAllowUntrustedServer() {
        return allowUntrustedServer;
    }

    @Override
    public void publishAlways() {
        this.publishAlways = true;
    }

    @Override
    public void publishAlwaysIf(boolean condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void publishOnFailure() {
        this.publishOnFailure = true;
    }

    public boolean isPublishOnFailure() {
        return publishOnFailure;
    }

    @Override
    public void publishOnFailureIf(boolean condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUploadInBackground(boolean uploadInBackground) {
        this.uploadInBackground = uploadInBackground;
    }

    @Override
    public boolean isUploadInBackground() {
        return uploadInBackground;
    }

    @Override
    public void setCaptureTaskInputFiles(boolean capture) {
        this.captureTaskInputFiles = capture;
    }

    @Override
    public boolean isCaptureTaskInputFiles() {
        return captureTaskInputFiles;
    }

    @Override
    public BuildScanDataObfuscation getObfuscation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void obfuscation(Action<? super BuildScanDataObfuscation> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BuildScanCaptureSettings getCapture() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void capture(Action<? super BuildScanCaptureSettings> action) {
        throw new RuntimeException("Not implemented");
    }

    // On internal interface
    public void publishIfAuthenticated() {
        publishIfAuthenticated = true;
    }

    public boolean isPublishIfAuthenticated() {
        return publishIfAuthenticated;
    }

    public boolean isPublishAlways() {
        return publishAlways;
    }
}
