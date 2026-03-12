package io.github.gradle.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.initialization.Settings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adds an "AGENT" tag to the build scan when an AI coding agent is detected.
 * Also adds a custom value with the name of the detected agent.
 *
 * <p>Detection is based on environment variables commonly set by AI coding tools:
 * <ul>
 *     <li>{@code CLAUDECODE} - Claude Code</li>
 *     <li>{@code CURSOR_AGENT} - Cursor</li>
 *     <li>{@code GEMINI_CLI} - Gemini CLI</li>
 *     <li>{@code CODEX_SANDBOX} - Codex CLI</li>
 *     <li>{@code OPENCODE_CLIENT} - OpenCode</li>
 * </ul>
 *
 * <p>Any tool can also be detected by setting the proposed standard {@code AGENT} environment variable
 * (see <a href="https://github.com/agentsmd/agents.md/issues/136">agents.md#136</a>)
 * variable.
 */
public class AIAgentTagProvider extends BuildScanCustomValueProvider {
    // Ordered map of env var -> agent name
    private static final Map<String, String> KNOWN_AGENTS = new LinkedHashMap<>();

    static {
        KNOWN_AGENTS.put("CLAUDECODE", "Claude Code");
        KNOWN_AGENTS.put("CURSOR_AGENT", "Cursor");
        KNOWN_AGENTS.put("GEMINI_CLI", "Gemini CLI");
        KNOWN_AGENTS.put("CODEX_SANDBOX", "Codex CLI");
        KNOWN_AGENTS.put("OPENCODE_CLIENT", "OpenCode");
    }

    public AIAgentTagProvider(DevelocityConventions conventions) {
        super(conventions);
    }

    @Override
    public boolean isEnabled() {
        return detectAgent().isPresent();
    }

    @Override
    public void accept(Settings settings, BuildScanConfiguration buildScan) {
        detectAgent().ifPresent(agent -> {
            buildScan.tag("AGENT");
            buildScan.value("ai.agent", agent);
        });
    }

    private Optional<String> detectAgent() {
        // Check for proposed standard AGENT env var first (agents.md#136)
        Optional<String> agent = getNonEmptyEnv("AGENT");
        if (agent.isPresent()) {
            return agent;
        }

        // Check known agent-specific env vars
        for (Map.Entry<String, String> entry : KNOWN_AGENTS.entrySet()) {
            Optional<String> value = getNonEmptyEnv(entry.getKey());
            if (value.isPresent()) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    private Optional<String> getNonEmptyEnv(String name) {
        String value = getConventions().getEnv(name);
        return value != null && !value.isEmpty() ? Optional.of(value) : Optional.empty();
    }
}
