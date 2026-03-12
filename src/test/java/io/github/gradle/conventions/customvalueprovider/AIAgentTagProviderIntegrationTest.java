package io.github.gradle.conventions.customvalueprovider;

import io.github.gradle.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIAgentTagProviderIntegrationTest extends AbstractDevelocityPluginIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        // Clear all known agent env vars to isolate tests
        withEnvironmentVariable("AGENT", "");
        withEnvironmentVariable("CLAUDECODE", "");
        withEnvironmentVariable("CURSOR_AGENT", "");
    }

    @Test
    void tagAIAgentWhenClaudeCodeDetected() {
        withEnvironmentVariable("CLAUDECODE", "1");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsTag("AGENT"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "Claude Code"));
    }

    @Test
    void tagAIAgentWhenCursorDetected() {
        withEnvironmentVariable("CURSOR_AGENT", "1");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsTag("AGENT"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "Cursor"));
    }

    @Test
    void tagAIAgentWithGenericEnvVar() {
        withEnvironmentVariable("AGENT", "CustomBot");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsTag("AGENT"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "CustomBot"));
    }

    @Test
    void genericEnvVarTakesPrecedenceOverSpecificAgents() {
        withEnvironmentVariable("AGENT", "MyAgent");
        withEnvironmentVariable("CLAUDECODE", "1");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "MyAgent"));
    }

    @Test
    void noAIAgentTagWhenNoAgentDetected() {
        succeeds("help");

        assertFalse(getConfiguredBuildScan().containsTag("AGENT"));
    }
}
