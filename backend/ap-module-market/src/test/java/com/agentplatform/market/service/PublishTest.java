package com.agentplatform.market.service;

import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.AssetVersionMapper;
import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.entity.SkillEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.asset.mapper.SkillMapper;
import com.agentplatform.common.core.agent.SourceResolver;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.security.PermissionChecker;
import com.agentplatform.common.mybatis.entity.AgentToolBindingEntity;
import com.agentplatform.common.mybatis.entity.AssetReferenceEntity;
import com.agentplatform.common.mybatis.mapper.AgentToolBindingMapper;
import com.agentplatform.common.mybatis.mapper.AssetReferenceMapper;
import com.agentplatform.market.converter.MarketConverter;
import com.agentplatform.market.dto.*;
import com.agentplatform.market.entity.MarketItemEntity;
import com.agentplatform.market.mapper.FavoriteMapper;
import com.agentplatform.market.mapper.MarketItemMapper;
import com.agentplatform.market.mapper.ReviewMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishTest {

    @Mock private MarketItemMapper marketItemMapper;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private ReviewMapper reviewMapper;
    @Mock private AssetVersionMapper assetVersionMapper;
    @Mock private AgentMapper agentMapper;
    @Mock private AgentToolBindingMapper toolBindingMapper;
    @Mock private AssetReferenceMapper assetReferenceMapper;
    @Mock private SkillMapper skillMapper;
    @Mock private McpMapper mcpMapper;
    @Mock private MarketConverter marketConverter;
    @Mock private SourceResolver sourceResolver;
    @Mock private PermissionChecker permissionChecker;

    private MarketService marketService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AGENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MCP_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SKILL_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        marketService = new MarketService(marketItemMapper, favoriteMapper, reviewMapper,
                assetVersionMapper, agentMapper, toolBindingMapper, assetReferenceMapper,
                skillMapper, mcpMapper, marketConverter, sourceResolver, permissionChecker,
                objectMapper);
    }

    @Nested
    @DisplayName("Publish Agent")
    class PublishAgent {

        @Test
        @DisplayName("first publish creates asset_version and market_item with listed status")
        void firstPublishAgent() {
            PublishRequest req = new PublishRequest();
            req.setAssetType("agent");
            req.setAssetId(AGENT_ID);
            req.setVisibility("public");
            req.setVersion("v1.0.0");
            req.setReleaseNotes("Initial release");

            AgentEntity agent = buildAgent("Test Agent", "draft");
            when(agentMapper.selectById(AGENT_ID)).thenReturn(agent);
            when(toolBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(assetReferenceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(assetVersionMapper.insert(any(AssetVersionEntity.class))).thenReturn(1);
            when(agentMapper.updateById(any(AgentEntity.class))).thenReturn(1);

            // No existing market item
            when(marketItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(marketItemMapper.insert(any(MarketItemEntity.class))).thenAnswer(inv -> {
                MarketItemEntity e = inv.getArgument(0);
                return 1;
            });


            MarketItemVO mockVo = new MarketItemVO();
            mockVo.setId(UUID.randomUUID());
            mockVo.setAssetType("agent");
            mockVo.setStatus("listed");
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(mockVo);

            MarketItemVO result = marketService.publish(req, USER_A);

            assertThat(result.getStatus()).isEqualTo("listed");

            ArgumentCaptor<AssetVersionEntity> versionCaptor = ArgumentCaptor.forClass(AssetVersionEntity.class);
            verify(assetVersionMapper).insert(versionCaptor.capture());
            AssetVersionEntity version = versionCaptor.getValue();
            assertThat(version.getAssetType()).isEqualTo("agent");
            assertThat(version.getAssetId()).isEqualTo(AGENT_ID);
            assertThat(version.getVersion()).isEqualTo("v1.0.0");
            assertThat(version.getConfigSnapshot()).isNotNull();

            // Market item should be inserted (not updated)
            verify(marketItemMapper).insert(any(MarketItemEntity.class));
            verify(marketItemMapper, never()).updateById(any(MarketItemEntity.class));

            // Agent status should be set to published
            verify(agentMapper).updateById(any(AgentEntity.class));
            verify(permissionChecker).checkAccess(eq(USER_A), any(), any());
        }

        @Test
        @DisplayName("publish with tool_bindings includes tool_schema_snapshot in config")
        void publishWithToolBindings() {
            PublishRequest req = new PublishRequest();
            req.setAssetType("agent");
            req.setAssetId(AGENT_ID);
            req.setVersion("v1.0.0");

            AgentEntity agent = buildAgent("Tooled Agent", "draft");
            when(agentMapper.selectById(AGENT_ID)).thenReturn(agent);

            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setSourceType("mcp");
            binding.setSourceId(UUID.randomUUID());
            binding.setToolName("fetch_data");
            binding.setToolSchemaSnapshot("{\"type\":\"object\"}");
            binding.setEnabled(true);
            binding.setSortOrder(0);
            when(toolBindingMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(binding));
            when(sourceResolver.resolveSourceName(any(), any())).thenReturn("Test MCP");
            when(sourceResolver.resolveSourceUrl(any(), any())).thenReturn("https://mcp.test");
            when(assetReferenceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(assetVersionMapper.insert(any(AssetVersionEntity.class))).thenReturn(1);
            when(agentMapper.updateById(any(AgentEntity.class))).thenReturn(1);
            when(marketItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(marketItemMapper.insert(any(MarketItemEntity.class))).thenReturn(1);

            MarketItemVO mockVo = new MarketItemVO();
            mockVo.setStatus("listed");
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(mockVo);

            marketService.publish(req, USER_A);

            ArgumentCaptor<AssetVersionEntity> captor = ArgumentCaptor.forClass(AssetVersionEntity.class);
            verify(assetVersionMapper).insert(captor.capture());
            String snapshot = captor.getValue().getConfigSnapshot();
            assertThat(snapshot).contains("tool_bindings");
            assertThat(snapshot).contains("fetch_data");
            assertThat(snapshot).contains("tool_schema_snapshot");
        }

        @Test
        @DisplayName("re-publish updates existing market item")
        void republishAgent() {
            PublishRequest req = new PublishRequest();
            req.setAssetType("agent");
            req.setAssetId(AGENT_ID);
            req.setVisibility("public");
            req.setVersion("v1.1.0");

            AgentEntity agent = buildAgent("Test Agent", "published");
            when(agentMapper.selectById(AGENT_ID)).thenReturn(agent);
            when(toolBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(assetReferenceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(assetVersionMapper.insert(any(AssetVersionEntity.class))).thenReturn(1);
            when(agentMapper.updateById(any(AgentEntity.class))).thenReturn(1);

            MarketItemEntity existingItem = buildMarketItem(AGENT_ID, "listed", "public");
            when(marketItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingItem);
            when(marketItemMapper.updateById(any(MarketItemEntity.class))).thenReturn(1);

            MarketItemVO mockVo = new MarketItemVO();
            mockVo.setId(existingItem.getId());
            mockVo.setStatus("listed");
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(mockVo);

            MarketItemVO result = marketService.publish(req, USER_A);

            assertThat(result.getStatus()).isEqualTo("listed");
            verify(marketItemMapper).updateById(any(MarketItemEntity.class));
            verify(marketItemMapper, never()).insert(any(MarketItemEntity.class));
        }
    }

    @Nested
    @DisplayName("Publish Skill")
    class PublishSkill {

        @Test
        @DisplayName("publishes skill with correct config snapshot")
        void publishSkill() {
            PublishRequest req = new PublishRequest();
            req.setAssetType("skill");
            req.setAssetId(SKILL_ID);
            req.setVisibility("public");
            req.setVersion("v1.0.0");

            SkillEntity skill = buildSkillEntity("Test Skill", "draft");
            when(skillMapper.selectById(SKILL_ID)).thenReturn(skill);
            when(assetVersionMapper.insert(any(AssetVersionEntity.class))).thenReturn(1);
            when(skillMapper.updateById(any(SkillEntity.class))).thenReturn(1);
            when(marketItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(marketItemMapper.insert(any(MarketItemEntity.class))).thenReturn(1);

            MarketItemVO mockVo = new MarketItemVO();
            mockVo.setStatus("listed");
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(mockVo);

            MarketItemVO result = marketService.publish(req, USER_A);

            assertThat(result.getStatus()).isEqualTo("listed");

            ArgumentCaptor<AssetVersionEntity> captor = ArgumentCaptor.forClass(AssetVersionEntity.class);
            verify(assetVersionMapper).insert(captor.capture());
            String snapshot = captor.getValue().getConfigSnapshot();
            assertThat(captor.getValue().getAssetType()).isEqualTo("skill");
            assertThat(snapshot).contains("Test Skill");
            assertThat(snapshot).contains("yaml");
            assertThat(snapshot).contains("format");
        }
    }

    @Nested
    @DisplayName("Publish MCP")
    class PublishMcp {

        @Test
        @DisplayName("MCP snapshot excludes auth_headers_enc")
        void mcpSnapshotExcludesAuthHeaders() {
            PublishRequest req = new PublishRequest();
            req.setAssetType("mcp");
            req.setAssetId(MCP_ID);
            req.setVersion("v1.0.0");

            McpEntity mcp = buildMcpEntity("Test MCP", "draft");
            when(mcpMapper.selectById(MCP_ID)).thenReturn(mcp);
            when(assetVersionMapper.insert(any(AssetVersionEntity.class))).thenReturn(1);
            when(mcpMapper.updateById(any(McpEntity.class))).thenReturn(1);
            when(marketItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(marketItemMapper.insert(any(MarketItemEntity.class))).thenReturn(1);

            MarketItemVO mockVo = new MarketItemVO();
            mockVo.setId(UUID.randomUUID());
            mockVo.setStatus("listed");
            when(marketConverter.toItemVO(any(MarketItemEntity.class))).thenReturn(mockVo);

            marketService.publish(req, USER_A);

            ArgumentCaptor<AssetVersionEntity> captor = ArgumentCaptor.forClass(AssetVersionEntity.class);
            verify(assetVersionMapper).insert(captor.capture());
            String snapshot = captor.getValue().getConfigSnapshot();
            assertThat(snapshot).doesNotContain("auth_headers_enc");
            assertThat(snapshot).doesNotContain("auth_headers");
        }
    }

    @Nested
    @DisplayName("Publish errors")
    class PublishErrors {

        @Test
        @DisplayName("unsupported asset type throws INVALID_REQUEST")
        void unsupportedType() {
            PublishRequest req = new PublishRequest();
            req.setAssetType("invalid_type");
            req.setAssetId(AGENT_ID);

            assertThatThrownBy(() -> marketService.publish(req, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("asset not found throws ASSET_NOT_FOUND")
        void assetNotFound() {
            PublishRequest req = new PublishRequest();
            req.setAssetType("agent");
            req.setAssetId(UUID.randomUUID());
            when(agentMapper.selectById(any())).thenReturn(null);

            assertThatThrownBy(() -> marketService.publish(req, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(ex -> ((BizException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    private AgentEntity buildAgent(String name, String status) {
        AgentEntity e = new AgentEntity();
        e.setId(AGENT_ID);
        e.setOwnerId(USER_A);
        e.setName(name);
        e.setDescription("Test description");
        e.setStatus(status);
        e.setVisibility("private");
        e.setMaxSteps(10);
        return e;
    }

    private McpEntity buildMcpEntity(String name, String status) {
        McpEntity e = new McpEntity();
        e.setId(MCP_ID);
        e.setOwnerId(USER_A);
        e.setName(name);
        e.setUrl("https://mcp.example.com");
        e.setProtocol("sse");
        e.setStatus(status);
        e.setVisibility("private");
        e.setAuthHeadersEnc("secret-token".getBytes());
        return e;
    }

    private MarketItemEntity buildMarketItem(UUID assetId, String status, String visibility) {
        MarketItemEntity e = new MarketItemEntity();
        e.setId(UUID.randomUUID());
        e.setAssetType("agent");
        e.setAssetId(assetId);
        e.setAuthorId(USER_A);
        e.setStatus(status);
        e.setVisibility(visibility);
        e.setUseCount(5L);
        e.setFavoriteCount(10L);
        e.setAvgRating(BigDecimal.valueOf(4.5));
        e.setReviewCount(3);
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }

    private SkillEntity buildSkillEntity(String name, String status) {
        SkillEntity e = new SkillEntity();
        e.setId(SKILL_ID);
        e.setOwnerId(USER_A);
        e.setName(name);
        e.setDescription("Test description");
        e.setFormat("yaml");
        e.setContent("name: test");
        e.setStatus(status);
        e.setVisibility("private");
        return e;
    }
}
