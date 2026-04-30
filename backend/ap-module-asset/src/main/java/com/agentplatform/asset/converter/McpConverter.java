package com.agentplatform.asset.converter;

import com.agentplatform.asset.dto.McpCreateRequest;
import com.agentplatform.asset.dto.McpDetailVO;
import com.agentplatform.asset.dto.McpSummaryVO;
import com.agentplatform.asset.entity.McpEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface McpConverter {

    @Mapping(target = "authHeadersMasked", ignore = true)
    McpDetailVO toDetailVO(McpEntity entity);

    McpSummaryVO toSummaryVO(McpEntity entity);

    @Mapping(target = "authHeadersEnc", ignore = true)
    McpEntity toEntity(McpCreateRequest request);
}
