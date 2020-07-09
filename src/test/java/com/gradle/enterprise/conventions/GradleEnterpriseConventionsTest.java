package com.gradle.enterprise.conventions;

import com.gradle.enterprise.conventions.customvalueprovider.GradleEnterpriseConventions;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.provider.ProviderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.Answers.RETURNS_MOCKS;

@ExtendWith(MockitoExtension.class)
public class GradleEnterpriseConventionsTest {
    @Mock(answer = RETURNS_MOCKS)
    ProviderFactory providerFactory;

    @Mock
    BuildScanExtension buildScanExtension;

    @Mock
    File projectDir;

    @InjectMocks
    GradleEnterpriseConventions gradleEnterpriseConventions;

    @Test
    public void dontSetCommitIdWhenInvalid() {
        gradleEnterpriseConventions.setCommitId(projectDir, buildScanExtension, "Invalid commid id");

        Mockito.verifyNoInteractions(buildScanExtension);
    }
}
