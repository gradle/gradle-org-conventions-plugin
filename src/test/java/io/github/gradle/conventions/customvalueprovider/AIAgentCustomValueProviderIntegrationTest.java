package io.github.gradle.conventions.customvalueprovider;

import io.github.gradle.fixtures.AbstractDevelocityPluginIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIAgentCustomValueProviderIntegrationTest extends AbstractDevelocityPluginIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        // Clear all known agent env vars to isolate tests
        withEnvironmentVariable("AGENT", "");
        withEnvironmentVariable("CLAUDECODE", "");
        withEnvironmentVariable("CURSOR_AGENT", "");
        withEnvironmentVariable("GEMINI_CLI", "");
        withEnvironmentVariable("CODEX_SANDBOX", "");
        withEnvironmentVariable("OPENCODE_CLIENT", "");
        withEnvironmentVariable("CLAUDE_CODE_SESSION_ID", "");
    }

    @Test
    void tagAIAgentWhenClaudeCodeDetected() {
        withEnvironmentVariable("CLAUDECODE", "1");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsTag("AGENT"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "Claude Code"));
    }

    @Test
    void capturesClaudeCodeSessionIdWhenClaudeCodeDetected() {
        withEnvironmentVariable("CLAUDECODE", "1");
        withEnvironmentVariable("CLAUDE_CODE_SESSION_ID", "abc-123");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "Claude Code"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent.session", "abc-123"));
    }

    @Test
    void noClaudeCodeSessionIdValueWhenSessionIdNotSet() {
        withEnvironmentVariable("CLAUDECODE", "1");

        succeeds("help");

        assertFalse(getConfiguredBuildScan().containsValue("ai.agent.session"));
    }

    @Test
    void noClaudeCodeSessionIdValueWhenNotClaudeCode() {
        withEnvironmentVariable("CURSOR_AGENT", "1");
        withEnvironmentVariable("CLAUDE_CODE_SESSION_ID", "abc-123");

        succeeds("help");

        assertFalse(getConfiguredBuildScan().containsValue("ai.agent.session"));
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
    void tagAIAgentWhenGeminiDetected() {
        withEnvironmentVariable("GEMINI_CLI", "1");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsTag("AGENT"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "Gemini CLI"));
    }

    @Test
    void tagAIAgentWhenCodexDetected() {
        withEnvironmentVariable("CODEX_SANDBOX", "seatbelt");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsTag("AGENT"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "Codex CLI"));
    }

    @Test
    void tagAIAgentWhenOpenCodeDetected() {
        withEnvironmentVariable("OPENCODE_CLIENT", "1");

        succeeds("help");

        assertTrue(getConfiguredBuildScan().containsTag("AGENT"));
        assertTrue(getConfiguredBuildScan().containsValue("ai.agent", "OpenCode"));
    }

    @Test
    void noAIAgentTagWhenNoAgentDetected() {
        succeeds("help");

        assertFalse(getConfiguredBuildScan().containsTag("AGENT"));
    }
}
