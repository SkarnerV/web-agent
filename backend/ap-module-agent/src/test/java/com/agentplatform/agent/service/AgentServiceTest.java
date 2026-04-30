package com.agentplatform.agent.service;

import com.agentplatform.agent.converter.AgentConverter;
import com.agentplatform.agent.dto.*;
import com.agentplatform.agent.entity.AgentEntity;
import com.agentplatform.agent.entity.AgentToolBindingEntity;
import com.agentplatform.agent.entity.AssetReferenceEntity;
import com.agentplatform.agent.entity.AssetVersionEntity;
import com.agentplatform.agent.mapper.AgentMapper;
import com.agentplatform.agent.mapper.AgentToolBindingMapper;
import com.agentplatform.agent.mapper.AssetReferenceMapper;
import com.agentplatform.agent.mapper.AssetVersionMapper;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.security.AssetRef;
import com.agentplatform.common.core.security.Permission;
import com.agentplatform.common.core.agent.SourceResolver;
import com.agentplatform.common.core.security.PermissionChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock private AgentMapper agentMapper;
    @Mock private AgentToolBindingMapper toolBindingMapper;
    @Mock private AssetReferenceMapper assetReferenceMapper;
    @Mock private AssetVersionMapper assetVersionMapper;
    @Mock private PermissionChecker permissionChecker;
    @Mock private AgentConverter agentConverter;
    @Mock private SourceResolver sourceResolver;

    private ObjectMapper objectMapper;
    private AgentService agentService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID AGENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        agentService = new AgentService(
                agentMapper, toolBindingMapper, assetReferenceMapper,
                assetVersionMapper, agentConverter, permissionChecker, objectMapper,
                sourceResolver);
    }

    // ───────── 3.1 CRUD Tests ─────────

    @Nested
    @DisplayName("3.1 Agent CRUD")
    class CrudTests {

        @Test
        @DisplayName("创建 Agent — 正常流程，保存 tool_bindings 和 asset_references")
        void createAgent_success() {
            AgentCreateRequest request = new AgentCreateRequest();
            request.setName("测试Agent");
            request.setDescription("描述");
            request.setMaxSteps(10);
            request.setToolBindings(List.of(
                    toolBinding("mcp", UUID.randomUUID(), "list_buckets"),
                    toolBinding("builtin", null, "web_search")
            ));
            request.setSkillIds(List.of(UUID.randomUUID()));

            AgentEntity mockEntity = new AgentEntity();
            mockEntity.setName("测试Agent");
            when(agentConverter.toEntity(any(AgentCreateRequest.class))).thenReturn(mockEntity);

            when(agentMapper.insert(any(AgentEntity.class))).thenAnswer(inv -> {
                AgentEntity e = inv.getArgument(0);
                e.setId(AGENT_ID);
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(agentMapper.selectById(AGENT_ID)).thenAnswer(inv -> buildAgentEntity("测试Agent", USER_A));
            when(toolBindingMapper.selectList(any())).thenReturn(List.of());
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());

            AgentDetailVO mockVo = new AgentDetailVO();
            mockVo.setName("测试Agent");
            when(agentConverter.toDetailVO(any())).thenReturn(mockVo);

            AgentDetailVO result = agentService.create(request, USER_A);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("测试Agent");

            verify(agentMapper).insert(any(AgentEntity.class));
            verify(toolBindingMapper, times(2)).insert(any(AgentToolBindingEntity.class));
            verify(assetReferenceMapper, times(1)).insert(any(AssetReferenceEntity.class));
        }

        @Test
        @DisplayName("查询 Agent 详情 — 权限校验通过")
        void getDetail_ownerAccess() {
            AgentEntity entity = buildAgentEntity("My Agent", USER_A);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);
            when(toolBindingMapper.selectList(any())).thenReturn(List.of());
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());

            AgentDetailVO mockVo = new AgentDetailVO();
            mockVo.setName("My Agent");
            when(agentConverter.toDetailVO(any())).thenReturn(mockVo);

            AgentDetailVO result = agentService.getDetail(AGENT_ID, USER_A);

            assertThat(result.getName()).isEqualTo("My Agent");
            verify(permissionChecker).checkAccess(eq(USER_A), any(AssetRef.class), eq(Permission.READ));
        }

        @Test
        @DisplayName("查询不存在的 Agent — 抛出 ASSET_NOT_FOUND")
        void getDetail_notFound() {
            when(agentMapper.selectById(AGENT_ID)).thenReturn(null);

            assertThatThrownBy(() -> agentService.getDetail(AGENT_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }

        @Test
        @DisplayName("更新 Agent — 乐观锁版本不匹配返回 ASSET_OPTIMISTIC_LOCK")
        void update_optimisticLock() {
            AgentEntity entity = buildAgentEntity("Old Name", USER_A);
            entity.setVersion(5L);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            AgentUpdateRequest request = new AgentUpdateRequest();
            request.setName("New Name");
            request.setVersion(3L); // stale version

            assertThatThrownBy(() -> agentService.update(AGENT_ID, request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        @Test
        @DisplayName("更新已发布 Agent — 设置 hasUnpublishedChanges=true")
        void update_publishedAgent_marksUnpublishedChanges() {
            AgentEntity entity = buildAgentEntity("Published Agent", USER_A);
            entity.setStatus("published");
            entity.setVersion(1L);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);
            when(agentMapper.updateById(any(AgentEntity.class))).thenReturn(1);
            when(toolBindingMapper.selectList(any())).thenReturn(List.of());
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());

            AgentDetailVO mockVo = new AgentDetailVO();
            mockVo.setName("Updated");
            when(agentConverter.toDetailVO(any())).thenReturn(mockVo);

            AgentUpdateRequest request = new AgentUpdateRequest();
            request.setName("Updated");
            request.setVersion(1L);

            agentService.update(AGENT_ID, request, USER_A);

            ArgumentCaptor<AgentEntity> captor = ArgumentCaptor.forClass(AgentEntity.class);
            verify(agentMapper).updateById(captor.capture());
            assertThat(captor.getValue().getHasUnpublishedChanges()).isTrue();
        }

        @Test
        @DisplayName("删除 Agent — 有依赖引用时返回 ASSET_DELETE_CONFLICT")
        void delete_conflict() {
            AgentEntity entity = buildAgentEntity("Referenced Agent", USER_A);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            AssetReferenceEntity dep = new AssetReferenceEntity();
            dep.setReferrerId(UUID.randomUUID());
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of(dep));

            assertThatThrownBy(() -> agentService.delete(AGENT_ID, USER_A, false))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> {
                        BizException biz = (BizException) e;
                        assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.ASSET_DELETE_CONFLICT);
                        assertThat(biz.getDetails()).containsKey("referrer_ids");
                    });
        }

        @Test
        @DisplayName("强制删除 Agent — 解除绑定后软删除")
        void delete_force() {
            AgentEntity entity = buildAgentEntity("Referenced Agent", USER_A);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            AssetReferenceEntity dep = new AssetReferenceEntity();
            dep.setReferrerId(UUID.randomUUID());
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of(dep));
            when(assetReferenceMapper.delete(any())).thenReturn(1);
            when(toolBindingMapper.delete(any())).thenReturn(0);
            when(agentMapper.deleteById(AGENT_ID)).thenReturn(1);

            assertThatCode(() -> agentService.delete(AGENT_ID, USER_A, true))
                    .doesNotThrowAnyException();

            verify(agentMapper).deleteById(AGENT_ID);
        }
    }

    // ───────── 3.2 Duplicate Tests ─────────

    @Nested
    @DisplayName("3.2 Agent 复制")
    class DuplicateTests {

        @Test
        @DisplayName("复制 Agent — 名称加-副本后缀，status=draft")
        void duplicate_copiesNameAndStatus() {
            AgentEntity source = buildAgentEntity("Excel助手", USER_A);

            UUID copyId = UUID.randomUUID();
            when(agentMapper.insert(any(AgentEntity.class))).thenAnswer(inv -> {
                AgentEntity e = inv.getArgument(0);
                e.setId(copyId);
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });

            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setAgentId(AGENT_ID);
            binding.setSourceType("mcp");
            binding.setSourceId(UUID.randomUUID());
            binding.setToolName("list_buckets");
            binding.setEnabled(true);
            binding.setSortOrder(0);

            AssetReferenceEntity ref = new AssetReferenceEntity();
            ref.setReferrerType("agent");
            ref.setReferrerId(AGENT_ID);
            ref.setRefereeType("skill");
            ref.setRefereeId(UUID.randomUUID());
            ref.setRefKind("skill");

            // First selectById is for the source, second is for getDetail after copy insert
            AgentEntity copyEntity = buildAgentEntity("Excel助手-副本", USER_A);
            copyEntity.setId(copyId);
            copyEntity.setStatus("draft");
            when(agentMapper.selectById(AGENT_ID)).thenReturn(source);
            when(agentMapper.selectById(copyId)).thenReturn(copyEntity);

            // toolBindingMapper.selectList is called: once for source copy, once for getDetail
            when(toolBindingMapper.selectList(any())).thenReturn(List.of(binding)).thenReturn(List.of());
            // assetReferenceMapper.selectList is called: once for source copy, once for getDetail
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of(ref)).thenReturn(List.of());

            AgentDetailVO mockVo = new AgentDetailVO();
            mockVo.setName("Excel助手-副本");
            when(agentConverter.toDetailVO(any())).thenReturn(mockVo);

            AgentDetailVO result = agentService.duplicate(AGENT_ID, USER_A);

            assertThat(result.getName()).isEqualTo("Excel助手-副本");

            ArgumentCaptor<AgentEntity> agentCaptor = ArgumentCaptor.forClass(AgentEntity.class);
            verify(agentMapper).insert(agentCaptor.capture());
            AgentEntity inserted = agentCaptor.getValue();
            assertThat(inserted.getName()).isEqualTo("Excel助手-副本");
            assertThat(inserted.getStatus()).isEqualTo("draft");

            verify(toolBindingMapper).insert(any(AgentToolBindingEntity.class));
            verify(assetReferenceMapper).insert(any(AssetReferenceEntity.class));
        }
    }

    // ───────── 3.3 Export / Import Tests ─────────

    @Nested
    @DisplayName("3.3 Agent 导出/导入")
    class ExportImportTests {

        @Test
        @DisplayName("导出 Agent — JSON 包含完整 tool_bindings")
        void export_containsToolBindings() {
            AgentEntity entity = buildAgentEntity("Export Agent", USER_A);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setSourceType("mcp");
            binding.setSourceId(UUID.randomUUID());
            binding.setToolName("list_buckets");
            binding.setToolSchemaSnapshot("{\"type\":\"object\"}");
            binding.setEnabled(true);
            binding.setSortOrder(0);
            when(toolBindingMapper.selectList(any())).thenReturn(List.of(binding));
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());

            Map<String, Object> result = agentService.export(AGENT_ID, USER_A);

            assertThat(result).containsKey("name");
            assertThat(result.get("name")).isEqualTo("Export Agent");
            assertThat(result).containsKey("tool_bindings");
            List<?> bindings = (List<?>) result.get("tool_bindings");
            assertThat(bindings).hasSize(1);
            Map<?, ?> tb = (Map<?, ?>) bindings.get(0);
            assertThat(tb.get("source_type")).isEqualTo("mcp");
            assertThat(tb.get("tool_name")).isEqualTo("list_buckets");
            assertThat(tb.get("tool_schema_snapshot")).isNotNull();
        }

        @Test
        @DisplayName("导入无效 JSON — 返回 AGENT_IMPORT_INVALID")
        void import_invalidJson() {
            assertThatThrownBy(() -> agentService.importAgent("not valid json{{{", USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.AGENT_IMPORT_INVALID);
        }

        @Test
        @DisplayName("导入缺少 name 字段 — 返回 AGENT_IMPORT_INVALID")
        void import_missingName() {
            assertThatThrownBy(() -> agentService.importAgent("{\"description\":\"test\"}", USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.AGENT_IMPORT_INVALID);
        }

        @Test
        @DisplayName("导入有效 JSON — 无法匹配的工具标记 unresolved")
        void import_unresolvedToolBindings() {
            String json = """
                    {
                      "name": "Imported Agent",
                      "max_steps": 15,
                      "tool_bindings": [
                        {
                          "source_type": "mcp",
                          "source_id": null,
                          "tool_name": "unknown_tool",
                          "enabled": true
                        },
                        {
                          "source_type": "builtin",
                          "tool_name": "web_search",
                          "enabled": true
                        }
                      ]
                    }
                    """;

            when(agentMapper.insert(any(AgentEntity.class))).thenAnswer(inv -> {
                AgentEntity e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(agentMapper.selectById(any(UUID.class))).thenAnswer(inv -> {
                AgentEntity e = buildAgentEntity("Imported Agent", USER_A);
                e.setId(inv.getArgument(0));
                return e;
            });
            when(toolBindingMapper.selectList(any())).thenReturn(List.of());
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());

            AgentDetailVO mockVo = new AgentDetailVO();
            mockVo.setName("Imported Agent");
            when(agentConverter.toDetailVO(any())).thenReturn(mockVo);

            AgentImportResult result = agentService.importAgent(json, USER_A);

            assertThat(result.getAgent()).isNotNull();
            assertThat(result.getAgent().getName()).isEqualTo("Imported Agent");
            assertThat(result.getUnresolvedRefs()).hasSize(1);
            assertThat(result.getUnresolvedRefs().get(0).get("tool_name")).isEqualTo("unknown_tool");
            assertThat(result.getUnresolvedRefs().get(0).get("source_type")).isEqualTo("mcp");

            // builtin tool should have been inserted (source_id=null is acceptable)
            verify(toolBindingMapper, times(1)).insert(any(AgentToolBindingEntity.class));
        }

        @Test
        @DisplayName("导出 → 导入往返一致")
        void export_import_roundtrip() throws Exception {
            AgentEntity entity = buildAgentEntity("Roundtrip Agent", USER_A);
            entity.setMaxSteps(20);
            entity.setSystemPrompt("You are helpful");
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setSourceType("builtin");
            binding.setToolName("web_search");
            binding.setEnabled(true);
            binding.setSortOrder(0);
            when(toolBindingMapper.selectList(any())).thenReturn(List.of(binding));
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());

            Map<String, Object> exported = agentService.export(AGENT_ID, USER_A);
            String json = objectMapper.writeValueAsString(exported);

            when(agentMapper.insert(any(AgentEntity.class))).thenAnswer(inv -> {
                AgentEntity e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(agentMapper.selectById(any(UUID.class))).thenAnswer(inv -> {
                AgentEntity e = buildAgentEntity("Roundtrip Agent", USER_A);
                e.setId(inv.getArgument(0));
                e.setMaxSteps(20);
                e.setSystemPrompt("You are helpful");
                return e;
            });

            AgentDetailVO mockVo = new AgentDetailVO();
            mockVo.setName("Roundtrip Agent");
            when(agentConverter.toDetailVO(any())).thenReturn(mockVo);

            AgentImportResult result = agentService.importAgent(json, USER_A);

            assertThat(result.getAgent().getName()).isEqualTo("Roundtrip Agent");
            assertThat(result.getUnresolvedRefs()).isEmpty();

            ArgumentCaptor<AgentEntity> captor = ArgumentCaptor.forClass(AgentEntity.class);
            verify(agentMapper, atLeastOnce()).insert(captor.capture());
            AgentEntity inserted = captor.getAllValues().stream()
                    .filter(e -> "Roundtrip Agent".equals(e.getName()))
                    .findFirst().orElseThrow();
            assertThat(inserted.getMaxSteps()).isEqualTo(20);
            assertThat(inserted.getSystemPrompt()).isEqualTo("You are helpful");
        }
    }

    // ───────── 3.4 Version Management Tests ─────────

    @Nested
    @DisplayName("3.4 Agent 版本管理")
    class VersionTests {

        @Test
        @DisplayName("版本列表 — 按 published_at 倒序返回")
        void listVersions_orderedByPublishedAtDesc() {
            AgentEntity entity = buildAgentEntity("Versioned Agent", USER_A);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            AssetVersionEntity v1 = buildVersion("v1.0.0", OffsetDateTime.now().minusDays(2));
            AssetVersionEntity v2 = buildVersion("v1.0.1", OffsetDateTime.now().minusDays(1));
            AssetVersionEntity v3 = buildVersion("v1.0.2", OffsetDateTime.now());
            when(assetVersionMapper.selectList(any())).thenReturn(List.of(v3, v2, v1));

            when(agentConverter.toAssetVersionVO(any())).thenAnswer(inv -> {
                AssetVersionEntity e = inv.getArgument(0);
                AssetVersionVO vo = new AssetVersionVO();
                vo.setVersion(e.getVersion());
                vo.setPublishedAt(e.getPublishedAt());
                return vo;
            });

            List<AssetVersionVO> result = agentService.listVersions(AGENT_ID, USER_A);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getVersion()).isEqualTo("v1.0.2");
            assertThat(result.get(1).getVersion()).isEqualTo("v1.0.1");
            assertThat(result.get(2).getVersion()).isEqualTo("v1.0.0");
        }

        @Test
        @DisplayName("回滚 — 恢复快照并设置 hasUnpublishedChanges=true")
        void rollback_restoresSnapshotAndMarksUnpublished() throws Exception {
            AgentEntity entity = buildAgentEntity("Current Name", USER_A);
            entity.setStatus("published");
            entity.setVersion(3L);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);
            when(agentMapper.updateById(any(AgentEntity.class))).thenReturn(1);
            when(toolBindingMapper.delete(any())).thenReturn(1);
            when(assetReferenceMapper.delete(any())).thenReturn(1);
            when(toolBindingMapper.selectList(any())).thenReturn(List.of());
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());

            AgentDetailVO mockVo = new AgentDetailVO();
            mockVo.setName("Old Name");
            when(agentConverter.toDetailVO(any())).thenReturn(mockVo);

            UUID versionId = UUID.randomUUID();
            String snapshot = objectMapper.writeValueAsString(Map.of(
                    "name", "Old Name",
                    "description", "Old Desc",
                    "max_steps", 5,
                    "system_prompt", "Old prompt",
                    "tool_bindings", List.of(
                            Map.of("source_type", "builtin", "tool_name", "web_search", "enabled", true)
                    ),
                    "skill_ids", List.of()
            ));
            AssetVersionEntity version = new AssetVersionEntity();
            version.setId(versionId);
            version.setAssetType("agent");
            version.setAssetId(AGENT_ID);
            version.setVersion("v1.0.0");
            version.setConfigSnapshot(snapshot);
            when(assetVersionMapper.selectById(versionId)).thenReturn(version);

            AgentDetailVO result = agentService.rollback(AGENT_ID, versionId, USER_A);

            ArgumentCaptor<AgentEntity> captor = ArgumentCaptor.forClass(AgentEntity.class);
            verify(agentMapper).updateById(captor.capture());
            AgentEntity updated = captor.getValue();
            assertThat(updated.getName()).isEqualTo("Old Name");
            assertThat(updated.getMaxSteps()).isEqualTo(5);
            assertThat(updated.getHasUnpublishedChanges()).isTrue();

            verify(toolBindingMapper).insert(any(AgentToolBindingEntity.class));
        }

        @Test
        @DisplayName("回滚不存在的版本 — 返回 ASSET_NOT_FOUND")
        void rollback_versionNotFound() {
            AgentEntity entity = buildAgentEntity("Agent", USER_A);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            UUID versionId = UUID.randomUUID();
            when(assetVersionMapper.selectById(versionId)).thenReturn(null);

            assertThatThrownBy(() -> agentService.rollback(AGENT_ID, versionId, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }

        @Test
        @DisplayName("回滚不属于该 Agent 的版本 — 返回 ASSET_NOT_FOUND")
        void rollback_versionBelongsToDifferentAgent() {
            AgentEntity entity = buildAgentEntity("Agent", USER_A);
            when(agentMapper.selectById(AGENT_ID)).thenReturn(entity);

            UUID versionId = UUID.randomUUID();
            AssetVersionEntity version = new AssetVersionEntity();
            version.setId(versionId);
            version.setAssetType("agent");
            version.setAssetId(UUID.randomUUID()); // different agent
            when(assetVersionMapper.selectById(versionId)).thenReturn(version);

            assertThatThrownBy(() -> agentService.rollback(AGENT_ID, versionId, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    // ───────── Test helpers ─────────

    private AgentEntity buildAgentEntity(String name, UUID ownerId) {
        AgentEntity entity = new AgentEntity();
        entity.setId(AGENT_ID);
        entity.setOwnerId(ownerId);
        entity.setName(name);
        entity.setDescription("test description");
        entity.setMaxSteps(10);
        entity.setStatus("draft");
        entity.setVisibility("private");
        entity.setHasUnpublishedChanges(false);
        entity.setVersion(0L);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }

    private AssetVersionEntity buildVersion(String ver, OffsetDateTime publishedAt) {
        AssetVersionEntity v = new AssetVersionEntity();
        v.setId(UUID.randomUUID());
        v.setAssetType("agent");
        v.setAssetId(AGENT_ID);
        v.setVersion(ver);
        v.setPublishedAt(publishedAt);
        return v;
    }

    private ToolBindingRequest toolBinding(String sourceType, UUID sourceId, String toolName) {
        ToolBindingRequest tb = new ToolBindingRequest();
        tb.setSourceType(sourceType);
        tb.setSourceId(sourceId);
        tb.setToolName(toolName);
        tb.setEnabled(true);
        return tb;
    }
}
