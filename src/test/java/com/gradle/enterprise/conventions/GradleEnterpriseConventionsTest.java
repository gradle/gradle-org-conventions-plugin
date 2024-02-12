package com.gradle.enterprise.conventions;

import com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.provider.ProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class GradleEnterpriseConventionsTest {
    @Mock
    ProviderFactory providerFactory;

    @Mock
    BuildScanExtension buildScanExtension;

    @Mock
    File projectDir;

    GradleEnterpriseConventions gradleEnterpriseConventions;

    @BeforeEach
    public void setUp() {
        gradleEnterpriseConventions = new GradleEnterpriseConventions(providerFactory);
    }

    @Test
    public void dontSetCommitIdWhenInvalid() {
        gradleEnterpriseConventions.setCommitId(projectDir, buildScanExtension, "Invalid commit id");

        verifyNoInteractions(buildScanExtension);
    }
}
