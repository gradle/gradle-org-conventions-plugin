package com.gradle.enterprise.gradleplugin;

import com.gradle.enterprise.fixtures.GradleEnterpriseExtensionForTest;
import groovy.json.JsonOutput;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

/**
 * This is a dummy gradle enterprise plugin for testing. It writes the configuration to json to
 * be verified in integration tests.
 */
public class GradleEnterprisePlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {
        GradleEnterpriseExtensionForTest extension = new GradleEnterpriseExtensionForTest();
        settings.getExtensions().add("gradleEnterpriseForTest", extension);
        settings.getGradle().afterProject(project -> {
            try {
                Files.write(project.file("gradleEnterpriseConfiguration.json").toPath(), JsonOutput.toJson(extension).getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
