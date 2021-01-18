package com.gradle.enterprise.conventions;

import com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GradleEnterpriseConventionsTest {
    @Mock
    ProviderFactory providerFactory;

    @Mock
    BuildScanExtension buildScanExtension;

    @Mock
    File projectDir;

    @Mock
    Provider<String> provider;

    GradleEnterpriseConventions gradleEnterpriseConventions;

    @BeforeEach
    public void setUp() {
        when(provider.get()).thenReturn("");
        when(providerFactory.environmentVariable(anyString())).thenReturn(provider);
        when(providerFactory.systemProperty(anyString())).thenReturn(provider);
        when(provider.forUseAtConfigurationTime()).thenReturn(provider);
        when(provider.orElse(anyString())).thenReturn(provider);

        gradleEnterpriseConventions = new GradleEnterpriseConventions(providerFactory);
    }

    @Test
    public void dontSetCommitIdWhenInvalid() {
        gradleEnterpriseConventions.setCommitId(projectDir, buildScanExtension, "Invalid commit id");

        verifyNoInteractions(buildScanExtension);
    }
}
