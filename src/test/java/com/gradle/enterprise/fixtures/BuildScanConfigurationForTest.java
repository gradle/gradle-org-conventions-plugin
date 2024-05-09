package com.gradle.enterprise.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gradle.develocity.agent.gradle.internal.scan.BuildScanCaptureConfigurationInternal;
import com.gradle.develocity.agent.gradle.internal.scan.BuildScanConfigurationInternal;
import com.gradle.develocity.agent.gradle.scan.BuildScanCaptureConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanDataObfuscationConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration;
import com.gradle.enterprise.conventions.PublishingConfigurationAction;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuildScanConfigurationForTest implements BuildScanConfigurationInternal {
    public BuildScanConfigurationForTest() {
        this(null);
    }

    public BuildScanConfigurationForTest(ObjectFactory objectFactory) {
        if (objectFactory != null) {
            this.termsOfUseUrlProperty = objectFactory.property(String.class);
            this.termsOfUseAgreeProperty = objectFactory.property(String.class);
            this.uploadInBackgroundProperty = objectFactory.property(Boolean.class);
            this.captureFileFingerprintsProperty = objectFactory.property(Boolean.class);
        }
    }

    private List<String> tags = new ArrayList<>();
    private List<List<String>> values = new ArrayList<>();
    private List<List<String>> links = new ArrayList<>();
    private List<String> backgroundTags = new ArrayList<>();
    private List<List<String>> backgroundValues = new ArrayList<>();
    private List<List<String>> backgroundLinks = new ArrayList<>();

    private Property<String> termsOfUseUrlProperty;
    private String termsOfUseUrlValue;
    private Property<String> termsOfUseAgreeProperty;
    private String termsOfUseAgreeValue;
    private String server;
    private boolean allowUntrustedServer;
    private List<String> publishConfigurationActions = new ArrayList<>();
    private Property<Boolean> uploadInBackgroundProperty;
    private boolean uploadInBackgroundValue;
    private Property<Boolean> captureFileFingerprintsProperty;
    private boolean captureFileFingerprintsValue;
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

    public boolean isCaptureFileFingerprints() {
        if (captureFileFingerprintsProperty != null && captureFileFingerprintsProperty.isPresent()) {
            return captureFileFingerprintsProperty.get();
        }
        return captureFileFingerprintsValue;
    }

    public void setCaptureFileFingerprints(boolean captureFileFingerprints) {
        this.captureFileFingerprintsValue = captureFileFingerprints;
    }

    public List<String> getPublishConfigurationAction() {
        return publishConfigurationActions;
    }

    public void setPublishConfigurationAction(List<String> publishConfigurationActions) {
        this.publishConfigurationActions = publishConfigurationActions;
    }

    public boolean isUploadInBackground() {
        if (uploadInBackgroundProperty != null && uploadInBackgroundProperty.isPresent()) {
            return uploadInBackgroundProperty.get();
        }
        return uploadInBackgroundValue;
    }

    public void setUploadInBackground(boolean uploadInBackground) {
        this.uploadInBackgroundValue = uploadInBackground;
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
    public void buildFinished(org.gradle.api.Action<? super com.gradle.develocity.agent.gradle.scan.BuildResult> action) {

    }

    @Override
    public void buildScanPublished(org.gradle.api.Action<? super com.gradle.develocity.agent.gradle.scan.PublishedBuildScan> action) {

    }

    @Override
    @JsonIgnore
    public Property<String> getTermsOfUseUrl() {
        return termsOfUseUrlProperty;
    }

    @Override
    @JsonIgnore
    public Property<String> getTermsOfUseAgree() {
        return termsOfUseAgreeProperty;
    }

    @Override
    @JsonSerialize(using = DevelocityConfigurationForTest.PropertySerializer.class)
    public Property<Boolean> getUploadInBackground() {
        return uploadInBackgroundProperty;
    }

    @Override
    @JsonIgnore
    public BuildScanPublishingConfiguration getPublishing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void publishing(Action<? super BuildScanPublishingConfiguration> action) {
        if (action instanceof PublishingConfigurationAction) {
            publishConfigurationActions.add(((PublishingConfigurationAction) action).name);
        }
    }

    @JsonIgnore
    public boolean isPublishOnFailure() {
        return publishConfigurationActions.contains(PublishingConfigurationAction.PUBLISH_ON_FAILURE.name);
    }

    @Override
    @JsonIgnore
    public BuildScanDataObfuscationConfiguration getObfuscation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void obfuscation(Action<? super BuildScanDataObfuscationConfiguration> action) {
        throw new UnsupportedOperationException();
    }

    private class BuildScanCaptureAdapter implements BuildScanCaptureConfiguration {
        @Override
        @JsonIgnore
        public Property<Boolean> getFileFingerprints() {
            return captureFileFingerprintsProperty;
        }

        @Override
        @JsonIgnore
        public Property<Boolean> getBuildLogging() {
            return null;
        }

        @Override
        @JsonIgnore
        public Property<Boolean> getTestLogging() {
            return null;
        }
    }

    @Override
    public void background(Runnable action) {
        inBackground = true;
        action.run();
        inBackground = false;
    }

    @Override
    @JsonIgnore
    public BuildScanCaptureConfigurationInternal getCapture() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void capture(Action<? super BuildScanCaptureConfiguration> action) {
        action.execute(buildScanCaptureAdapter);
    }

    private final BuildScanCaptureAdapter buildScanCaptureAdapter = new BuildScanCaptureAdapter();

    @Override
    public void onError(Action<String> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    @JsonIgnore
    public DualPublishConfiguration getDualPublish() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void dualPublish(Action<? super DualPublishConfiguration> action) {
        throw new UnsupportedOperationException();
    }

    @JsonIgnore
    public boolean isPublishIfAuthenticated() {
        return publishConfigurationActions.contains(PublishingConfigurationAction.PUBLISH_IF_AUTHENTICATED.name);
    }

    @JsonIgnore
    public boolean isPublishAlways() {
        return publishConfigurationActions.contains(PublishingConfigurationAction.PUBLISH_ALWAYS.name);
    }

    @Override
    public void onErrorInternal(Action<BuildScanError> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    @JsonIgnore
    public Property<Boolean> getDevelocityPluginApplied() {
        throw new UnsupportedOperationException();
    }
}
