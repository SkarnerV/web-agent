package com.agentplatform.market.converter;

import com.agentplatform.market.dto.MarketItemVO;
import com.agentplatform.market.dto.ReviewVO;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.entity.ReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MarketConverter {

    @Mapping(target = "authorName", ignore = true)
    MarketItemVO toItemVO(MarketItemEntity entity);

    @Mapping(target = "userName", ignore = true)
    ReviewVO toReviewVO(ReviewEntity entity);
}
