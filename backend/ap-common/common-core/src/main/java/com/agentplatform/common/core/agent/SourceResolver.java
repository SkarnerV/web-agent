package com.agentplatform.common.core.agent;

import java.util.UUID;

/**
 * Cross-module interface for resolving tool source metadata (name, URL)
 * from source_id + source_type, used primarily in Agent export.
 */
public interface SourceResolver {

    /**
     * @return display name for the given source, or null if not found
     */
    String resolveSourceName(String sourceType, UUID sourceId);

    /**
     * @return URL for the given source (e.g., MCP endpoint URL), or null if not applicable
     */
    String resolveSourceUrl(String sourceType, UUID sourceId);
}
