package io.github.gradle.conventions;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import io.github.gradle.conventions.customvalueprovider.DevelocityConventions;
import org.gradle.api.provider.ProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DevelocityConventionsTest {
    @Mock
    ProviderFactory providerFactory;

    @Mock
    BuildScanConfiguration buildScanConfiguration;

    @Mock
    Path projectDir;

    DevelocityConventions develocityConventions;

    @BeforeEach
    void setUp() {
        develocityConventions = new DevelocityConventions(providerFactory);
    }

    @Test
    void dontSetCommitIdWhenInvalid() {
        develocityConventions.setCommitId(projectDir, buildScanConfiguration, "Invalid commit id");

        verifyNoInteractions(buildScanConfiguration);
    }
}
