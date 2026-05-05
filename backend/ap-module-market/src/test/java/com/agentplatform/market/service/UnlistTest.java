package com.agentplatform.market.service;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.market.converter.MarketConverter;
import com.agentplatform.market.dto.MarketItemVO;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.mapper.MarketItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnlistTest {

    @Mock private MarketItemMapper marketItemMapper;
    @Mock private MarketConverter marketConverter;

    private MarketService marketService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        marketService = new MarketService(marketItemMapper, null, null, null, null, null,
                null, null, null, marketConverter, null, null, null);
    }

    @Nested
    @DisplayName("Unlist item")
    class Unlist {

        @Test
        @DisplayName("setting visibility=private sets status=unlisted")
        void unlistMakesItemUnlisted() {
            MarketItemEntity item = buildItem(USER_A, "listed", "public");
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(marketItemMapper.updateById(any(MarketItemEntity.class))).thenReturn(1);

            MarketItemVO vo = new MarketItemVO();
            vo.setStatus("unlisted");
            vo.setVisibility("private");
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(vo);

            MarketItemVO result = marketService.updateVisibility(ITEM_ID, "private", USER_A);

            assertThat(result.getStatus()).isEqualTo("unlisted");
            assertThat(result.getVisibility()).isEqualTo("private");

            ArgumentCaptor<MarketItemEntity> captor = ArgumentCaptor.forClass(MarketItemEntity.class);
            verify(marketItemMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("unlisted");
            assertThat(captor.getValue().getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("setting visibility=public re-lists the item")
        void republishMakesItemListed() {
            MarketItemEntity item = buildItem(USER_A, "unlisted", "private");
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(marketItemMapper.updateById(any(MarketItemEntity.class))).thenReturn(1);

            MarketItemVO vo = new MarketItemVO();
            vo.setStatus("listed");
            vo.setVisibility("public");
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(vo);

            MarketItemVO result = marketService.updateVisibility(ITEM_ID, "public", USER_A);

            assertThat(result.getStatus()).isEqualTo("listed");
            assertThat(result.getVisibility()).isEqualTo("public");
        }
    }

    @Nested
    @DisplayName("Permission errors")
    class PermissionErrors {

        @Test
        @DisplayName("non-author cannot change visibility")
        void nonAuthorDenied() {
            MarketItemEntity item = buildItem(USER_A, "listed", "public");
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);

            assertThatThrownBy(() -> marketService.updateVisibility(ITEM_ID, "private", OTHER_USER))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_PERMISSION_DENIED);
        }

        @Test
        @DisplayName("item not found throws ASSET_NOT_FOUND")
        void itemNotFound() {
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(null);

            assertThatThrownBy(() -> marketService.updateVisibility(ITEM_ID, "private", USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    private MarketItemEntity buildItem(UUID authorId, String status, String visibility) {
        MarketItemEntity e = new MarketItemEntity();
        e.setId(ITEM_ID);
        e.setAssetType("agent");
        e.setAssetId(UUID.randomUUID());
        e.setAuthorId(authorId);
        e.setStatus(status);
        e.setVisibility(visibility);
        e.setUseCount(5L);
        e.setFavoriteCount(10L);
        e.setAvgRating(BigDecimal.valueOf(4.0));
        e.setReviewCount(2);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }
}
