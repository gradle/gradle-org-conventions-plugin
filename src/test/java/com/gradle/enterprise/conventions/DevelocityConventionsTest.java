package com.gradle.enterprise.conventions;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import com.gradle.enterprise.conventions.customvalueprovider.DevelocityConventions;
import org.gradle.api.provider.ProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class DevelocityConventionsTest {
    @Mock
    ProviderFactory providerFactory;

    @Mock
    BuildScanConfiguration buildScanConfiguration;

    @Mock
    File projectDir;

    DevelocityConventions develocityConventions;

    @BeforeEach
    public void setUp() {
        develocityConventions = new DevelocityConventions(providerFactory);
    }

    @Test
    public void dontSetCommitIdWhenInvalid() {
        develocityConventions.setCommitId(projectDir, buildScanConfiguration, "Invalid commit id");

        verifyNoInteractions(buildScanConfiguration);
    }
}
