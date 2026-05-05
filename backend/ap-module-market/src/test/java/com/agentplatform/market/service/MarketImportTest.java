package com.agentplatform.market.service;

import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.AssetVersionMapper;
import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.mybatis.mapper.AgentToolBindingMapper;
import com.agentplatform.common.mybatis.mapper.AssetReferenceMapper;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.mapper.MarketItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketImportTest {

    @Mock private MarketItemMapper marketItemMapper;
    @Mock private AssetVersionMapper assetVersionMapper;
    @Mock private AgentMapper agentMapper;
    @Mock private AgentToolBindingMapper toolBindingMapper;
    @Mock private AssetReferenceMapper assetReferenceMapper;
    @Mock private McpMapper mcpMapper;

    private MarketService marketService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ITEM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        marketService = new MarketService(marketItemMapper, null, null, assetVersionMapper,
                agentMapper, toolBindingMapper, assetReferenceMapper, null, mcpMapper,
                null, null, null, objectMapper);
    }

    @Nested
    @DisplayName("Import agent")
    class ImportAgent {

        @Test
        @DisplayName("creates agent copy and increments use_count")
        void createsAgent() {
            MarketItemEntity item = buildItem("agent", 5L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);

            String config = "{\"name\":\"Test Agent\",\"max_steps\":10,\"tool_bindings\":[]," +
                    "\"skill_ids\":[],\"knowledge_base_ids\":[],\"collaborator_agent_ids\":[]}";
            AssetVersionEntity version = buildVersion(config);
            when(assetVersionMapper.selectById(VERSION_ID)).thenReturn(version);

            when(agentMapper.insert(any(AgentEntity.class))).thenAnswer(inv -> {
                AgentEntity e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return 1;
            });
            lenient().when(toolBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(marketItemMapper.incrementUseCount(ITEM_ID)).thenReturn(1);

            Map<String, Object> result = marketService.importItem(ITEM_ID, USER_A);

            assertThat(result.get("asset_id")).isNotNull();
            assertThat(result.get("asset_type")).isEqualTo("agent");

            verify(marketItemMapper).incrementUseCount(ITEM_ID);
        }
    }

    @Nested
    @DisplayName("Import MCP")
    class ImportMcp {

        @Test
        @DisplayName("MCP import does NOT copy auth_headers")
        void mcpWithoutCredentials() {
            MarketItemEntity item = buildItem("mcp", 5L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);

            String config = "{\"name\":\"Test MCP\",\"url\":\"https://mcp.example.com\"," +
                    "\"protocol\":\"sse\",\"tools_discovered\":[]}";
            AssetVersionEntity version = buildVersion(config);
            when(assetVersionMapper.selectById(VERSION_ID)).thenReturn(version);

            when(mcpMapper.insert(any(McpEntity.class))).thenAnswer(inv -> {
                McpEntity e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return 1;
            });
            when(marketItemMapper.incrementUseCount(ITEM_ID)).thenReturn(1);

            Map<String, Object> result = marketService.importItem(ITEM_ID, USER_A);

            assertThat(result.get("asset_type")).isEqualTo("mcp");
            // Verify MCP entity created without auth headers
            verify(mcpMapper).insert(any(McpEntity.class));
        }
    }

    @Nested
    @DisplayName("Import errors")
    class ImportErrors {

        @Test
        @DisplayName("item without version throws INVALID_REQUEST")
        void noVersionThrows() {
            MarketItemEntity item = buildItem("agent", 5L);
            item.setCurrentVersionId(null);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);

            assertThatThrownBy(() -> marketService.importItem(ITEM_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("version not found throws ASSET_NOT_FOUND")
        void versionNotFound() {
            MarketItemEntity item = buildItem("agent", 5L);
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(item);
            when(assetVersionMapper.selectById(VERSION_ID)).thenReturn(null);

            assertThatThrownBy(() -> marketService.importItem(ITEM_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }

        @Test
        @DisplayName("item not found throws ASSET_NOT_FOUND")
        void itemNotFound() {
            when(marketItemMapper.selectById(ITEM_ID)).thenReturn(null);

            assertThatThrownBy(() -> marketService.importItem(ITEM_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    private MarketItemEntity buildItem(String assetType, long useCount) {
        MarketItemEntity e = new MarketItemEntity();
        e.setId(ITEM_ID);
        e.setAssetType(assetType);
        e.setAssetId(UUID.randomUUID());
        e.setAuthorId(UUID.randomUUID());
        e.setStatus("listed");
        e.setVisibility("public");
        e.setCurrentVersionId(VERSION_ID);
        e.setUseCount(useCount);
        e.setFavoriteCount(5L);
        e.setAvgRating(BigDecimal.valueOf(4.0));
        e.setReviewCount(2);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private AssetVersionEntity buildVersion(String configSnapshot) {
        AssetVersionEntity v = new AssetVersionEntity();
        v.setId(VERSION_ID);
        v.setAssetType("agent");
        v.setAssetId(UUID.randomUUID());
        v.setVersion("v1.0.0");
        v.setConfigSnapshot(configSnapshot);
        v.setPublishedBy(USER_A);
        v.setPublishedAt(OffsetDateTime.now());
        return v;
    }
}
