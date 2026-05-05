package com.agentplatform.market.service;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.market.converter.MarketConverter;
import com.agentplatform.market.dto.ReviewCreateRequest;
import com.agentplatform.market.dto.ReviewVO;
import com.agentplatform.market.entity.FavoriteEntity;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.entity.ReviewEntity;
import com.agentplatform.market.mapper.FavoriteMapper;
import com.agentplatform.market.mapper.MarketItemMapper;
import com.agentplatform.market.mapper.ReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteReviewTest {

    @Mock private MarketItemMapper marketItemMapper;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private ReviewMapper reviewMapper;
    @Mock private MarketConverter marketConverter;

    private MarketService marketService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        marketService = new MarketService(marketItemMapper, favoriteMapper, reviewMapper, null,
                null, null, null, null, null, marketConverter, null, null, null);
    }

    @Nested
    @DisplayName("Favorites")
    class Favorites {

        @Test
        @DisplayName("add favorite increments count atomically")
        void addFavoriteIncrementsCount() {
            MarketItemEntity item = buildItem(10L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(favoriteMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(favoriteMapper.insert(any(FavoriteEntity.class))).thenReturn(1);
            when(marketItemMapper.incrementFavoriteCount(ITEM_ID)).thenReturn(1);

            marketService.addFavorite(ITEM_ID, USER_A);

            verify(marketItemMapper).incrementFavoriteCount(ITEM_ID);
        }

        @Test
        @DisplayName("add favorite twice is idempotent")
        void addFavoriteTwiceIsIdempotent() {
            MarketItemEntity item = buildItem(10L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(favoriteMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

            marketService.addFavorite(ITEM_ID, USER_A);

            verify(favoriteMapper, never()).insert(any(FavoriteEntity.class));
            verify(marketItemMapper, never()).incrementFavoriteCount(any());
        }

        @Test
        @DisplayName("remove favorite decrements count atomically")
        void removeFavoriteDecrementsCount() {
            MarketItemEntity item = buildItem(10L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(new FavoriteEntity());
            when(favoriteMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(marketItemMapper.decrementFavoriteCount(ITEM_ID)).thenReturn(1);

            marketService.removeFavorite(ITEM_ID, USER_A);

            verify(marketItemMapper).decrementFavoriteCount(ITEM_ID);
        }

        @Test
        @DisplayName("remove when not favorited is idempotent")
        void removeWhenNotFavoritedIsIdempotent() {
            MarketItemEntity item = buildItem(5L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(favoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            marketService.removeFavorite(ITEM_ID, USER_A);

            verify(marketItemMapper, never()).decrementFavoriteCount(any());
            verify(favoriteMapper, never()).delete(any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("Reviews")
    class Reviews {

        @Test
        @DisplayName("create review recalculates average rating via atomic SQL")
        void createReviewRecalculatesAverage() {
            MarketItemEntity item = buildItem(5L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(reviewMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(reviewMapper.insert(any(ReviewEntity.class))).thenReturn(1);
            when(marketItemMapper.recalcRatingAndCount(ITEM_ID)).thenReturn(1);

            ReviewCreateRequest req = new ReviewCreateRequest();
            req.setRating((short) 3);
            req.setComment("Good");

            ReviewVO mockVo = new ReviewVO();
            mockVo.setRating((short) 3);
            when(marketConverter.toReviewVO(any(ReviewEntity.class))).thenReturn(mockVo);

            ReviewVO result = marketService.createReview(ITEM_ID, req, USER_A);

            assertThat(result.getRating()).isEqualTo((short) 3);
            verify(marketItemMapper).recalcRatingAndCount(ITEM_ID);
        }

        @Test
        @DisplayName("duplicate review by same user throws INVALID_REQUEST")
        void duplicateReviewThrows() {
            MarketItemEntity item = buildItem(0L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(reviewMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

            ReviewCreateRequest req = new ReviewCreateRequest();
            req.setRating((short) 4);

            assertThatThrownBy(() -> marketService.createReview(ITEM_ID, req, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("list reviews returns paginated results")
        void listReviewsPaginated() {
            ReviewEntity review = buildReview((short) 5);
            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        IPage<ReviewEntity> page = inv.getArgument(0);
                        page.setRecords(List.of(review));
                        page.setTotal(1);
                        return page;
                    });
            ReviewVO vo = new ReviewVO();
            vo.setRating((short) 5);
            when(marketConverter.toReviewVO(any(ReviewEntity.class))).thenReturn(vo);

            List<ReviewVO> result = marketService.listReviews(ITEM_ID, 1, 20);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRating()).isEqualTo((short) 5);
        }

        @Test
        @DisplayName("review for non-existent item throws ASSET_NOT_FOUND")
        void reviewItemNotFound() {
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(null);

            ReviewCreateRequest req = new ReviewCreateRequest();
            req.setRating((short) 4);

            assertThatThrownBy(() -> marketService.createReview(ITEM_ID, req, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    private MarketItemEntity buildItem(long favoriteCount) {
        MarketItemEntity e = new MarketItemEntity();
        e.setId(ITEM_ID);
        e.setAssetType("agent");
        e.setAssetId(UUID.randomUUID());
        e.setAuthorId(UUID.randomUUID());
        e.setStatus("listed");
        e.setVisibility("public");
        e.setFavoriteCount(favoriteCount);
        e.setAvgRating(BigDecimal.valueOf(4.5).setScale(1, RoundingMode.HALF_UP));
        e.setReviewCount(3);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private ReviewEntity buildReview(short rating) {
        ReviewEntity e = new ReviewEntity();
        e.setId(UUID.randomUUID());
        e.setMarketItemId(ITEM_ID);
        e.setUserId(UUID.randomUUID());
        e.setRating(rating);
        e.setComment("Test comment");
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }
}
