package com.agentplatform.agent.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentImportResult {

    private AgentDetailVO agent;

    private List<Map<String, Object>> unresolvedRefs;
}
