package com.agentplatform.market.converter;

import com.agentplatform.market.dto.MarketItemDetailVO;
import com.agentplatform.market.dto.MarketItemVO;
import com.agentplatform.market.dto.ReviewVO;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.entity.ReviewEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class DefaultMarketConverter implements MarketConverter {

    @Override
    public MarketItemVO toItemVO(MarketItemEntity entity) {
        if (entity == null) {
            return null;
        }
        MarketItemVO vo = new MarketItemVO();
        copyItem(entity, vo);
        return vo;
    }

    @Override
    public MarketItemDetailVO toItemDetailVO(MarketItemEntity entity) {
        if (entity == null) {
            return null;
        }
        MarketItemDetailVO vo = new MarketItemDetailVO();
        copyItem(entity, vo);
        return vo;
    }

    @Override
    public ReviewVO toReviewVO(ReviewEntity entity) {
        if (entity == null) {
            return null;
        }
        ReviewVO vo = new ReviewVO();
        vo.setId(entity.getId());
        vo.setMarketItemId(entity.getMarketItemId());
        vo.setUserId(entity.getUserId());
        vo.setRating(entity.getRating());
        vo.setComment(entity.getComment());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private static void copyItem(MarketItemEntity entity, MarketItemVO vo) {
        vo.setId(entity.getId());
        vo.setAssetType(entity.getAssetType());
        vo.setAssetId(entity.getAssetId());
        vo.setCurrentVersionId(entity.getCurrentVersionId());
        vo.setAuthorId(entity.getAuthorId());
        vo.setStatus(entity.getStatus());
        vo.setVisibility(entity.getVisibility());
        vo.setCategory(entity.getCategory());
        vo.setTags(entity.getTags());
        vo.setUseCount(entity.getUseCount());
        vo.setFavoriteCount(entity.getFavoriteCount());
        vo.setAvgRating(entity.getAvgRating());
        vo.setReviewCount(entity.getReviewCount());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
    }
}
