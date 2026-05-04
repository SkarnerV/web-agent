package com.agentplatform.asset.service;

import com.agentplatform.asset.client.McpClient;
import com.agentplatform.asset.converter.McpConverter;
import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.entity.McpEntity;
import com.agentplatform.asset.mapper.McpMapper;
import com.agentplatform.common.core.enums.ConnectionStatus;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.AssetRef;
import com.agentplatform.common.core.security.CredentialStore;
import com.agentplatform.common.core.security.Permission;
import com.agentplatform.common.core.security.PermissionChecker;
import com.agentplatform.common.core.tool.ToolRegistry;
import com.agentplatform.common.mybatis.entity.AgentToolBindingEntity;
import com.agentplatform.common.mybatis.mapper.AgentToolBindingMapper;
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
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpCrudTest {

    @Mock private McpMapper mcpMapper;
    @Mock private McpConverter mcpConverter;
    @Mock private AgentToolBindingMapper agentToolBindingMapper;
    @Mock private CredentialStore credentialStore;
    @Mock private ToolRegistry toolRegistry;
    @Mock private PermissionChecker permissionChecker;
    @Mock private McpClient mcpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private McpService mcpService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MCP_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        mcpService = new McpService(mcpMapper, mcpConverter, agentToolBindingMapper,
                credentialStore, toolRegistry, permissionChecker, objectMapper, mcpClient);
    }

    @Nested
    @DisplayName("Create MCP")
    class CreateTests {

        @Test
        @DisplayName("creates MCP with valid SSE protocol and encrypted auth")
        void create_validSse() {
            McpCreateRequest request = new McpCreateRequest();
            request.setName("My MCP");
            request.setUrl("https://mcp.example.com");
            request.setProtocol("sse");
            request.setAuthHeaders("Authorization: Bearer secret");

            McpEntity entity = buildMcpEntity("My MCP", "sse");
            when(mcpConverter.toEntity(request)).thenReturn(entity);
            when(credentialStore.encrypt("Authorization: Bearer secret")).thenReturn("encrypted-secret");
            when(mcpMapper.insert(any(McpEntity.class))).thenAnswer(inv -> {
                McpEntity e = inv.getArgument(0);
                e.setId(MCP_ID);
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("My MCP");
            when(mcpConverter.toDetailVO(any(McpEntity.class))).thenReturn(mockVo);

            McpDetailVO result = mcpService.create(request, USER_A);

            assertThat(result.getName()).isEqualTo("My MCP");

            ArgumentCaptor<McpEntity> captor = ArgumentCaptor.forClass(McpEntity.class);
            verify(mcpMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("draft");
            assertThat(captor.getValue().getEnabled()).isTrue();
            assertThat(new String(captor.getValue().getAuthHeadersEnc())).isEqualTo("encrypted-secret");
        }

        @Test
        @DisplayName("rejects invalid protocol with MCP_PROTOCOL_INVALID")
        void create_invalidProtocol() {
            McpCreateRequest request = new McpCreateRequest();
            request.setName("Bad MCP");
            request.setUrl("https://mcp.example.com");
            request.setProtocol("grpc");

            assertThatThrownBy(() -> mcpService.create(request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MCP_PROTOCOL_INVALID);
        }

        @Test
        @DisplayName("creates MCP without auth headers")
        void create_noAuth() {
            McpCreateRequest request = new McpCreateRequest();
            request.setName("No Auth MCP");
            request.setUrl("https://mcp.example.com");
            request.setProtocol("streamable_http");

            McpEntity entity = buildMcpEntity("No Auth MCP", "streamable_http");
            when(mcpConverter.toEntity(request)).thenReturn(entity);
            when(mcpMapper.insert(any(McpEntity.class))).thenAnswer(inv -> {
                McpEntity e = inv.getArgument(0);
                e.setId(MCP_ID);
                e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now());
                e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("No Auth MCP");
            when(mcpConverter.toDetailVO(any(McpEntity.class))).thenReturn(mockVo);

            McpDetailVO result = mcpService.create(request, USER_A);

            assertThat(result.getName()).isEqualTo("No Auth MCP");
            verify(credentialStore, never()).encrypt(any());
        }
    }

    @Nested
    @DisplayName("List MCPs")
    class ListTests {

        @Test
        @DisplayName("returns paginated results with masked auth")
        void list_paginated() {
            McpEntity entity = buildMcpEntity("My MCP", "sse");
            entity.setToolsDiscovered("[{\"name\":\"tool1\"}]");
            when(mcpMapper.selectPage(any(), any())).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<McpEntity> page = inv.getArgument(0);
                page.setRecords(List.of(entity));
                page.setTotal(1);
                return page;
            });

            McpSummaryVO mockVo = new McpSummaryVO();
            mockVo.setName("My MCP");
            when(mcpConverter.toSummaryVO(entity)).thenReturn(mockVo);

            PageResult<McpSummaryVO> result = mcpService.list(USER_A, null, 1, 20, "updated_at", "desc");

            assertThat(result.data()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
            assertThat(result.data().get(0).getToolsDiscoveredCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get MCP Detail")
    class GetDetailTests {

        @Test
        @DisplayName("returns detail with masked auth headers")
        void getDetail_masksAuth() {
            McpEntity entity = buildMcpEntity("Detail MCP", "sse");
            entity.setAuthHeadersEnc("encrypted-secret".getBytes());
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(credentialStore.mask("encrypted-secret")).thenReturn("****ret");

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("Detail MCP");
            when(mcpConverter.toDetailVO(entity)).thenReturn(mockVo);

            McpDetailVO result = mcpService.getDetail(MCP_ID, USER_A);

            assertThat(result.getName()).isEqualTo("Detail MCP");
            assertThat(result.getAuthHeadersMasked()).isEqualTo("****ret");
            verify(permissionChecker).checkAccess(eq(USER_A), any(AssetRef.class), eq(Permission.READ));
        }

        @Test
        @DisplayName("throws ASSET_NOT_FOUND for missing MCP")
        void getDetail_notFound() {
            when(mcpMapper.selectById(MCP_ID)).thenReturn(null);

            assertThatThrownBy(() -> mcpService.getDetail(MCP_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Update MCP")
    class UpdateTests {

        @Test
        @DisplayName("partial update with auth re-encryption")
        void update_reEncryptsAuth() {
            McpEntity entity = buildMcpEntity("Old Name", "sse");
            entity.setVersion(1L);
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(credentialStore.encrypt("new-secret")).thenReturn("new-encrypted");
            when(mcpMapper.updateById(any(McpEntity.class))).thenReturn(1);

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("New Name");
            when(mcpConverter.toDetailVO(any(McpEntity.class))).thenReturn(mockVo);

            McpUpdateRequest request = new McpUpdateRequest();
            request.setName("New Name");
            request.setAuthHeaders("new-secret");
            request.setVersion(1L);

            McpDetailVO result = mcpService.update(MCP_ID, request, USER_A);

            assertThat(result.getName()).isEqualTo("New Name");
            ArgumentCaptor<McpEntity> captor = ArgumentCaptor.forClass(McpEntity.class);
            verify(mcpMapper).updateById(captor.capture());
            assertThat(new String(captor.getValue().getAuthHeadersEnc())).isEqualTo("new-encrypted");
            verify(toolRegistry).refreshMcpTools(MCP_ID);
        }

        @Test
        @DisplayName("optimistic lock conflict")
        void update_optimisticLock() {
            McpEntity entity = buildMcpEntity("Old", "sse");
            entity.setVersion(5L);
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);

            McpUpdateRequest request = new McpUpdateRequest();
            request.setName("New");
            request.setVersion(3L);

            assertThatThrownBy(() -> mcpService.update(MCP_ID, request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }
    }

    @Nested
    @DisplayName("Delete MCP")
    class DeleteTests {

        @Test
        @DisplayName("deletes MCP without dependencies")
        void delete_noDependencies() {
            McpEntity entity = buildMcpEntity("To Delete", "sse");
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(agentToolBindingMapper.selectList(any())).thenReturn(List.of());
            when(mcpMapper.deleteById(MCP_ID)).thenReturn(1);

            assertThatCode(() -> mcpService.delete(MCP_ID, USER_A, false))
                    .doesNotThrowAnyException();

            verify(toolRegistry).refreshMcpTools(MCP_ID);
            verify(mcpMapper).deleteById(MCP_ID);
        }

        @Test
        @DisplayName("throws ASSET_DELETE_CONFLICT when MCP is referenced by agent tool bindings")
        void delete_conflict() {
            McpEntity entity = buildMcpEntity("Referenced MCP", "published");
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);

            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setAgentId(UUID.randomUUID());
            binding.setSourceType("mcp");
            binding.setSourceId(MCP_ID);
            binding.setToolName("some_tool");
            when(agentToolBindingMapper.selectList(any())).thenReturn(List.of(binding));

            assertThatThrownBy(() -> mcpService.delete(MCP_ID, USER_A, false))
                    .isInstanceOf(BizException.class)
                    .satisfies(e -> {
                        BizException biz = (BizException) e;
                        assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.ASSET_DELETE_CONFLICT);
                        assertThat(biz.getDetails()).containsKey("referrer_agent_ids");
                    });
        }

        @Test
        @DisplayName("force delete unbinds tool bindings and removes MCP")
        void delete_force() {
            McpEntity entity = buildMcpEntity("Force Delete", "draft");
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);

            AgentToolBindingEntity binding = new AgentToolBindingEntity();
            binding.setAgentId(UUID.randomUUID());
            binding.setSourceType("mcp");
            binding.setSourceId(MCP_ID);
            binding.setToolName("some_tool");
            when(agentToolBindingMapper.selectList(any())).thenReturn(List.of(binding));
            when(agentToolBindingMapper.delete(any())).thenReturn(1);
            when(mcpMapper.deleteById(MCP_ID)).thenReturn(1);

            assertThatCode(() -> mcpService.delete(MCP_ID, USER_A, true))
                    .doesNotThrowAnyException();

            verify(agentToolBindingMapper).delete(any());
            verify(mcpMapper).deleteById(MCP_ID);
        }
    }

    @Nested
    @DisplayName("Toggle MCP")
    class ToggleTests {

        @Test
        @DisplayName("disables MCP and refreshes tool registry")
        void toggle_disable() {
            McpEntity entity = buildMcpEntity("Toggle MCP", "sse");
            entity.setEnabled(true);
            entity.setConnectionStatus(ConnectionStatus.CONNECTED.getValue());
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(mcpMapper.updateById(any(McpEntity.class))).thenReturn(1);

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("Toggle MCP");
            when(mcpConverter.toDetailVO(any(McpEntity.class))).thenReturn(mockVo);

            McpDetailVO result = mcpService.toggle(MCP_ID, USER_A, false);

            assertThat(result.getName()).isEqualTo("Toggle MCP");
            ArgumentCaptor<McpEntity> captor = ArgumentCaptor.forClass(McpEntity.class);
            verify(mcpMapper).updateById(captor.capture());
            assertThat(captor.getValue().getEnabled()).isFalse();
            assertThat(captor.getValue().getConnectionStatus()).isEqualTo(ConnectionStatus.OFFLINE.getValue());
            verify(toolRegistry).refreshMcpTools(MCP_ID);
        }

        @Test
        @DisplayName("enables MCP")
        void toggle_enable() {
            McpEntity entity = buildMcpEntity("Toggle MCP", "sse");
            entity.setEnabled(false);
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(mcpMapper.updateById(any(McpEntity.class))).thenReturn(1);

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("Toggle MCP");
            when(mcpConverter.toDetailVO(any(McpEntity.class))).thenReturn(mockVo);

            mcpService.toggle(MCP_ID, USER_A, true);

            ArgumentCaptor<McpEntity> captor = ArgumentCaptor.forClass(McpEntity.class);
            verify(mcpMapper).updateById(captor.capture());
            assertThat(captor.getValue().getEnabled()).isTrue();
            verify(toolRegistry).refreshMcpTools(MCP_ID);
        }
    }

    @Nested
    @DisplayName("Export MCP")
    class ExportTests {

        @Test
        @DisplayName("exports MCP as JSON map")
        void export_success() {
            McpEntity entity = buildMcpEntity("Export MCP", "published");
            entity.setDescription("MCP for export");
            entity.setUrl("https://mcp.example.com");
            entity.setProtocol("sse");
            entity.setJsonConfig("{\"timeout\":30}");
            entity.setToolsDiscovered("[{\"name\":\"tool1\"},{\"name\":\"tool2\"}]");
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);

            Map<String, Object> result = mcpService.export(MCP_ID, USER_A);

            assertThat(result).containsEntry("name", "Export MCP");
            assertThat(result).containsEntry("url", "https://mcp.example.com");
            assertThat(result).containsEntry("protocol", "sse");
            assertThat(result.get("tools_discovered")).asList().hasSize(2);
            verify(permissionChecker).checkAccess(eq(USER_A), any(AssetRef.class), eq(Permission.READ));
        }
    }

    @Nested
    @DisplayName("Test Connection & Discover Tools")
    class McpClientTests {

        @Test
        @DisplayName("testConnection updates status to CONNECTED on success")
        void testConnection_success() {
            McpEntity entity = buildMcpEntity("Test MCP", "sse");
            entity.setAuthHeadersEnc("encrypted-auth".getBytes());
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(credentialStore.decrypt("encrypted-auth")).thenReturn("Bearer token");
            when(mcpClient.testConnection("https://mcp.example.com", "sse", "Bearer token"))
                    .thenReturn(Map.of("status", "ok"));
            when(mcpMapper.updateById(any(McpEntity.class))).thenReturn(1);

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("Test MCP");
            when(mcpConverter.toDetailVO(any(McpEntity.class))).thenReturn(mockVo);

            McpDetailVO result = mcpService.testConnection(MCP_ID, USER_A);

            assertThat(result.getName()).isEqualTo("Test MCP");
            ArgumentCaptor<McpEntity> captor = ArgumentCaptor.forClass(McpEntity.class);
            verify(mcpMapper).updateById(captor.capture());
            assertThat(captor.getValue().getConnectionStatus()).isEqualTo(ConnectionStatus.CONNECTED.getValue());
        }

        @Test
        @DisplayName("testConnection throws MCP_CONNECTION_FAILED on failure")
        void testConnection_failure() {
            McpEntity entity = buildMcpEntity("Test MCP", "sse");
            entity.setAuthHeadersEnc("encrypted-auth".getBytes());
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(credentialStore.decrypt("encrypted-auth")).thenReturn("Bearer token");
            when(mcpClient.testConnection(any(), any(), any()))
                    .thenThrow(new RuntimeException("Connection refused"));
            when(mcpMapper.updateById(any(McpEntity.class))).thenReturn(1);

            assertThatThrownBy(() -> mcpService.testConnection(MCP_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MCP_CONNECTION_FAILED);

            ArgumentCaptor<McpEntity> captor = ArgumentCaptor.forClass(McpEntity.class);
            verify(mcpMapper).updateById(captor.capture());
            assertThat(captor.getValue().getConnectionStatus()).isEqualTo(ConnectionStatus.FAILED.getValue());
        }

        @Test
        @DisplayName("discoverTools updates toolsDiscovered and refreshes registry")
        void discoverTools_success() {
            McpEntity entity = buildMcpEntity("Discover MCP", "sse");
            entity.setEnabled(true);
            entity.setAuthHeadersEnc("encrypted-auth".getBytes());
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);
            when(credentialStore.decrypt("encrypted-auth")).thenReturn("Bearer token");
            List<Map<String, Object>> tools = List.of(
                    Map.of("name", "tool1", "description", "desc1", "inputSchema", Map.of("type", "object")),
                    Map.of("name", "tool2", "description", "desc2", "inputSchema", Map.of("type", "object")));
            when(mcpClient.discoverTools("https://mcp.example.com", "sse", "Bearer token"))
                    .thenReturn(tools);
            when(mcpMapper.updateById(any(McpEntity.class))).thenReturn(1);

            McpDetailVO mockVo = new McpDetailVO();
            mockVo.setName("Discover MCP");
            when(mcpConverter.toDetailVO(any(McpEntity.class))).thenReturn(mockVo);

            McpDetailVO result = mcpService.discoverTools(MCP_ID, USER_A);

            assertThat(result.getName()).isEqualTo("Discover MCP");
            ArgumentCaptor<McpEntity> captor = ArgumentCaptor.forClass(McpEntity.class);
            verify(mcpMapper).updateById(captor.capture());
            assertThat(captor.getValue().getConnectionStatus()).isEqualTo(ConnectionStatus.CONNECTED.getValue());
            assertThat(captor.getValue().getToolsDiscovered()).contains("tool1").contains("tool2");
            verify(toolRegistry).refreshMcpTools(MCP_ID);
        }

        @Test
        @DisplayName("discoverTools throws MCP_CONNECTION_FAILED when MCP is disabled")
        void discoverTools_disabled() {
            McpEntity entity = buildMcpEntity("Disabled MCP", "sse");
            entity.setEnabled(false);
            when(mcpMapper.selectById(MCP_ID)).thenReturn(entity);

            assertThatThrownBy(() -> mcpService.discoverTools(MCP_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MCP_CONNECTION_FAILED);
        }
    }

    private McpEntity buildMcpEntity(String name, String protocol) {
        McpEntity entity = new McpEntity();
        entity.setId(MCP_ID);
        entity.setOwnerId(USER_A);
        entity.setName(name);
        entity.setDescription(name + " description");
        entity.setUrl("https://mcp.example.com");
        entity.setProtocol(protocol);
        entity.setEnabled(true);
        entity.setConnectionStatus(ConnectionStatus.OFFLINE.getValue());
        entity.setStatus("draft");
        entity.setVisibility("private");
        entity.setCurrentVersion("v1.0.0");
        entity.setHasUnpublishedChanges(false);
        entity.setVersion(0L);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }
}
