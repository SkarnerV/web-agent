package com.agentplatform.asset.converter;

import com.agentplatform.asset.dto.McpCreateRequest;
import com.agentplatform.asset.dto.McpDetailVO;
import com.agentplatform.asset.dto.McpSummaryVO;
import com.agentplatform.asset.entity.McpEntity;
public interface McpConverter {

    McpDetailVO toDetailVO(McpEntity entity);

    McpSummaryVO toSummaryVO(McpEntity entity);

    McpEntity toEntity(McpCreateRequest request);
}
