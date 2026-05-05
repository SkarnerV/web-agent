package com.agentplatform.market.mapper;

import com.agentplatform.market.entity.MarketItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.UUID;

@Mapper
public interface MarketItemMapper extends BaseMapper<MarketItemEntity> {

    @Update("UPDATE market_items SET favorite_count = favorite_count + 1 WHERE id = #{id}")
    int incrementFavoriteCount(UUID id);

    @Update("UPDATE market_items SET favorite_count = GREATEST(0, favorite_count - 1) WHERE id = #{id}")
    int decrementFavoriteCount(UUID id);

    @Update("UPDATE market_items SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(UUID id);

    @Update("""
        UPDATE market_items SET
            avg_rating = COALESCE((SELECT ROUND(AVG(rating)::numeric, 1) FROM reviews WHERE market_item_id = #{id}), 0),
            review_count = (SELECT COUNT(*) FROM reviews WHERE market_item_id = #{id})
        WHERE id = #{id}
        """)
    int recalcRatingAndCount(UUID id);
}
