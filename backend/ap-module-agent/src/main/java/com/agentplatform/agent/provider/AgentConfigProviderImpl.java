package com.agentplatform.agent.provider;

import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AgentToolBindingEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.AgentToolBindingMapper;
import com.agentplatform.common.core.agent.AgentConfigProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AgentConfigProviderImpl implements AgentConfigProvider {

    private final AgentMapper agentMapper;
    private final AgentToolBindingMapper toolBindingMapper;

    public AgentConfigProviderImpl(AgentMapper agentMapper,
                                   AgentToolBindingMapper toolBindingMapper) {
        this.agentMapper = agentMapper;
        this.toolBindingMapper = toolBindingMapper;
    }

    @Override
    public String getModelId(UUID agentId) {
        AgentEntity agent = agentMapper.selectById(agentId);
        return agent != null ? agent.getModelId() : null;
    }

    @Override
    public Integer getMaxSteps(UUID agentId) {
        AgentEntity agent = agentMapper.selectById(agentId);
        return agent != null ? agent.getMaxSteps() : null;
    }

    @Override
    public Map<String, Map<String, Object>> getToolBindings(UUID agentId) {
        List<AgentToolBindingEntity> bindings = toolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getAgentId, agentId)
                        .eq(AgentToolBindingEntity::getEnabled, true));

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (AgentToolBindingEntity b : bindings) {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("source_type", b.getSourceType());
            config.put("source_id", b.getSourceId());
            config.put("tool_name", b.getToolName());
            config.put("tool_schema_snapshot", b.getToolSchemaSnapshot());
            result.put(b.getToolName(), config);
        }
        return result;
    }
}
