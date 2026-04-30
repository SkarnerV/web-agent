package com.agentplatform.common.core.agent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cross-module interface for chat module to read Agent configuration
 * without direct dependency on ap-module-agent.
 */
public interface AgentConfigProvider {

    /**
     * @return the configured model_id for the agent, or null if not set
     */
    String getModelId(UUID agentId);

    /**
     * @return the configured max_steps for the agent, or null if not set
     */
    Integer getMaxSteps(UUID agentId);

    /**
     * Returns the tool binding configuration for the given agent.
     * Map key is tool_name, value contains at least: source_type, source_id.
     */
    Map<String, Map<String, Object>> getToolBindings(UUID agentId);
}
