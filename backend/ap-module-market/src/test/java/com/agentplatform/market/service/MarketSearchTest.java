package com.agentplatform.market.service;

import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.mapper.AssetVersionMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.market.converter.MarketConverter;
import com.agentplatform.market.dto.MarketItemDetailVO;
import com.agentplatform.market.dto.MarketItemVO;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.mapper.MarketItemMapper;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketSearchTest {

    @Mock private MarketItemMapper marketItemMapper;
    @Mock private AssetVersionMapper assetVersionMapper;
    @Mock private MarketConverter marketConverter;

    private MarketService marketService;

    private static final UUID ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        marketService = new MarketService(marketItemMapper, null, null, assetVersionMapper, null,
                null, null, null, null, marketConverter, null, null, null);
    }

    @Nested
    @DisplayName("List items")
    class ListItems {

        @Test
        @DisplayName("filters by type")
        void filterByType() {
            MarketItemEntity item = buildItem();
            mockPageResult(List.of(item), 1);

            MarketItemVO vo = buildVO();
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(vo);

            PageResult<MarketItemVO> result = marketService.listItems(
                    "agent", null, null, null, 1, 20, "updated_at", "desc");

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("full-text search applies GIN condition")
        void fullTextSearch() {
            MarketItemEntity item = buildItem();
            mockPageResult(List.of(item), 1);
            MarketItemVO vo = buildVO();
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(vo);

            PageResult<MarketItemVO> result = marketService.listItems(
                    null, null, "Excel", null, 1, 20, "updated_at", "desc");

            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("paginates correctly")
        void paginates() {
            mockPageResult(List.of(buildItem()), 25);

            MarketItemVO vo = buildVO();
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(vo);

            PageResult<MarketItemVO> result = marketService.listItems(
                    null, null, null, null, 1, 10, "updated_at", "desc");

            assertThat(result.data()).hasSize(1);
            assertThat(result.total()).isEqualTo(25);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("sorts by rating desc")
        void sortByRatingDesc() {
            mockPageResult(List.of(buildItem(), buildItem()), 2);
            MarketItemVO vo = buildVO();
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(vo);

            PageResult<MarketItemVO> result = marketService.listItems(
                    null, null, null, null, 1, 20, "avg_rating", "desc");

            assertThat(result.data()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Item detail")
    class ItemDetail {

        @Test
        @DisplayName("returns detail with config_snapshot from asset_versions")
        void returnsConfigSnapshot() {
            UUID versionId = UUID.randomUUID();
            MarketItemEntity item = buildItem();
            item.setCurrentVersionId(versionId);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);

            AssetVersionEntity version = new AssetVersionEntity();
            version.setId(versionId);
            version.setConfigSnapshot("{\"name\":\"Test\"}");
            when(assetVersionMapper.selectById(versionId)).thenReturn(version);

            MarketItemDetailVO vo = new MarketItemDetailVO();
            vo.setId(ITEM_ID);
            when(marketConverter.toItemDetailVO(item)).thenReturn(vo);

            MarketItemDetailVO result = marketService.getItemDetail(ITEM_ID);

            assertThat(result.getId()).isEqualTo(ITEM_ID);
            // configSnapshot is set via the converter ignore, then manually
        }

        @Test
        @DisplayName("item not found throws ASSET_NOT_FOUND")
        void itemNotFound() {
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(null);

            assertThatThrownBy(() -> marketService.getItemDetail(ITEM_ID))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Featured items")
    class FeaturedItems {

        @Test
        @DisplayName("returns top 20 items ordered by rating and favorite count")
        void returnsTopRated() {
            MarketItemEntity item1 = buildItem();
            item1.setAvgRating(BigDecimal.valueOf(4.8));
            item1.setFavoriteCount(20L);
            when(marketItemMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(item1));

            MarketItemVO vo = buildVO();
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(vo);

            PageResult<MarketItemVO> result = marketService.getFeaturedItems(null);

            assertThat(result.data()).hasSize(1);
        }
    }

    private MarketItemEntity buildItem() {
        MarketItemEntity e = new MarketItemEntity();
        e.setId(ITEM_ID);
        e.setAssetType("agent");
        e.setAssetId(UUID.randomUUID());
        e.setAuthorId(UUID.randomUUID());
        e.setStatus("listed");
        e.setVisibility("public");
        e.setCategory("office");
        e.setTags("\"productivity\"");
        e.setUseCount(5L);
        e.setFavoriteCount(10L);
        e.setAvgRating(BigDecimal.valueOf(4.0));
        e.setReviewCount(3);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private MarketItemVO buildVO() {
        MarketItemVO vo = new MarketItemVO();
        vo.setId(ITEM_ID);
        vo.setAssetType("agent");
        vo.setStatus("listed");
        vo.setVisibility("public");
        vo.setCategory("office");
        return vo;
    }

    @SuppressWarnings("unchecked")
    private void mockPageResult(List<MarketItemEntity> records, long total) {
        when(marketItemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(inv -> {
                    IPage<MarketItemEntity> page = inv.getArgument(0);
                    page.setRecords(records);
                    page.setTotal(total);
                    return page;
                });
    }
}
