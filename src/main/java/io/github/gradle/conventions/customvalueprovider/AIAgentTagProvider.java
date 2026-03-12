package io.github.gradle.conventions.customvalueprovider;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.initialization.Settings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds an "AI_AGENT" tag to the build scan when an AI coding agent is detected.
 * Also adds a custom value with the name of the detected agent.
 *
 * <p>Detection is based on environment variables commonly set by AI coding tools:
 * <ul>
 *     <li>{@code CLAUDECODE} - Claude Code</li>
 *     <li>{@code CURSOR_AGENT} - Cursor</li>
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
    }

    public AIAgentTagProvider(DevelocityConventions conventions) {
        super(conventions);
    }

    @Override
    public boolean isEnabled() {
        return detectAgent() != null;
    }

    @Override
    public void accept(Settings settings, BuildScanConfiguration buildScan) {
        String agent = detectAgent();
        if (agent != null) {
            buildScan.tag("AGENT");
            buildScan.value("ai.agent", agent);
        }
    }

    private String detectAgent() {
        // Check for proposed standard AGENT env var first (agents.md#136)
        String agent = getConventions().getEnv("AGENT");
        if (agent != null && !agent.isEmpty()) {
            return agent;
        }

        // Check known agent-specific env vars
        for (Map.Entry<String, String> entry : KNOWN_AGENTS.entrySet()) {
            String value = getConventions().getEnv(entry.getKey());
            if (value != null && !value.isEmpty()) {
                return entry.getValue();
            }
        }

        return null;
    }
}
