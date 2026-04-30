package com.agentplatform.asset.provider;

import com.agentplatform.asset.entity.KnowledgeBaseEntity;
import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.mapper.KnowledgeBaseMapper;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.common.core.agent.SourceResolver;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SourceResolverImpl implements SourceResolver {

    private final McpMapper mcpMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public SourceResolverImpl(McpMapper mcpMapper, KnowledgeBaseMapper knowledgeBaseMapper) {
        this.mcpMapper = mcpMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    @Override
    public String resolveSourceName(String sourceType, UUID sourceId) {
        if (sourceId == null) return null;
        return switch (sourceType) {
            case "mcp" -> {
                McpEntity mcp = mcpMapper.selectById(sourceId);
                yield mcp != null ? mcp.getName() : null;
            }
            case "knowledge" -> {
                KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(sourceId);
                yield kb != null ? kb.getName() : null;
            }
            default -> null;
        };
    }

    @Override
    public String resolveSourceUrl(String sourceType, UUID sourceId) {
        if (sourceId == null || !"mcp".equals(sourceType)) return null;
        McpEntity mcp = mcpMapper.selectById(sourceId);
        return mcp != null ? mcp.getUrl() : null;
    }
}
