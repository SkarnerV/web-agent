package com.agentplatform.asset.service;

import com.agentplatform.asset.converter.KnowledgeBaseConverter;
import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.entity.KbDocumentEntity;
import com.agentplatform.asset.entity.KnowledgeBaseEntity;
import com.agentplatform.asset.mapper.KbDocumentMapper;
import com.agentplatform.asset.mapper.KnowledgeBaseMapper;
import com.agentplatform.common.core.embedding.EmbeddingClient;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.knowledge.KbCleanupMessageSender;
import com.agentplatform.common.core.knowledge.KbIndexMessageSender;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.AssetRef;
import com.agentplatform.common.core.security.AssetVisibility;
import com.agentplatform.common.core.security.Permission;
import com.agentplatform.common.core.security.PermissionChecker;
import com.agentplatform.common.core.storage.FileStorageService;
import com.agentplatform.common.core.tool.ToolRegistry;
import com.agentplatform.common.mybatis.entity.AgentToolBindingEntity;
import com.agentplatform.common.mybatis.entity.AssetReferenceEntity;
import com.agentplatform.common.mybatis.mapper.AgentToolBindingMapper;
import com.agentplatform.common.mybatis.mapper.AssetReferenceMapper;
import com.agentplatform.common.mybatis.mapper.FileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseCrudTest {

    @Mock private KnowledgeBaseMapper kbMapper;
    @Mock private KnowledgeBaseConverter kbConverter;
    @Mock private AssetReferenceMapper assetReferenceMapper;
    @Mock private AgentToolBindingMapper agentToolBindingMapper;
    @Mock private PermissionChecker permissionChecker;
    @Mock private KbCleanupMessageSender cleanupMessageSender;
    @Mock private FileMapper fileMapper;
    @Mock private KbDocumentMapper kbDocumentMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private KbIndexMessageSender indexMessageSender;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ToolRegistry toolRegistry;
    @Mock private EmbeddingClient embeddingClient;

    private KnowledgeBaseService kbService;

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID KB_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID DOC_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @BeforeEach
    void setUp() {
        kbService = new KnowledgeBaseService(kbMapper, kbConverter,
                assetReferenceMapper, agentToolBindingMapper, permissionChecker, cleanupMessageSender,
                fileMapper, kbDocumentMapper, fileStorageService, indexMessageSender,
                embeddingClient, jdbcTemplate, toolRegistry);
    }

    @Nested
    @DisplayName("Create Knowledge Base")
    class CreateTests {

        @Test
        @DisplayName("creates KB and registers knowledge tool")
        void create_registersTool() {
            KnowledgeBaseCreateRequest request = new KnowledgeBaseCreateRequest();
            request.setName("My KB");

            KnowledgeBaseEntity entity = buildKbEntity("My KB");
            when(kbConverter.toEntity(request)).thenReturn(entity);
            when(kbMapper.insert(any(KnowledgeBaseEntity.class))).thenAnswer(inv -> {
                KnowledgeBaseEntity e = inv.getArgument(0);
                e.setId(KB_ID); e.setVersion(0L);
                e.setCreatedAt(OffsetDateTime.now()); e.setUpdatedAt(OffsetDateTime.now());
                return 1;
            });
            when(kbMapper.selectById(KB_ID)).thenReturn(entity);
            when(kbConverter.toDetailVO(any())).thenReturn(new KnowledgeBaseDetailVO());

            kbService.create(request, USER_A);

            verify(toolRegistry).registerKnowledgeTool(KB_ID, "My KB");
        }
    }

    @Nested
    @DisplayName("List Knowledge Bases")
    class ListTests {

        @Test
        @DisplayName("returns paginated results")
        void list_paginated() {
            KnowledgeBaseEntity entity = buildKbEntity("My KB");
            when(kbMapper.selectPage(any(com.baomidou.mybatisplus.core.metadata.IPage.class), any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<KnowledgeBaseEntity> page = inv.getArgument(0);
                page.setRecords(List.of(entity)); page.setTotal(1);
                return page;
            });
            when(kbConverter.toSummaryVO(entity)).thenReturn(new KnowledgeBaseSummaryVO());

            PageResult<KnowledgeBaseSummaryVO> result = kbService.list(USER_A, null, 1, 20, "updated_at", "desc");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Knowledge Base Detail")
    class GetDetailTests {

        @Test
        @DisplayName("throws ASSET_NOT_FOUND for missing KB")
        void getDetail_notFound() {
            when(kbMapper.selectById(KB_ID)).thenReturn(null);
            assertThatThrownBy(() -> kbService.getDetail(KB_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Update Knowledge Base")
    class UpdateTests {

        @Test
        @DisplayName("optimistic lock version mismatch")
        void update_optimisticLock() {
            KnowledgeBaseEntity entity = buildKbEntity("Old");
            entity.setVersion(5L);
            when(kbMapper.selectById(KB_ID)).thenReturn(entity);

            KnowledgeBaseUpdateRequest request = new KnowledgeBaseUpdateRequest();
            request.setVersion(3L);

            assertThatThrownBy(() -> kbService.update(KB_ID, request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }

        @Test
        @DisplayName("throws INVALID_REQUEST when version is null")
        void update_nullVersion() {
            KnowledgeBaseEntity entity = buildKbEntity("Old");
            when(kbMapper.selectById(KB_ID)).thenReturn(entity);

            KnowledgeBaseUpdateRequest request = new KnowledgeBaseUpdateRequest();
            assertThatThrownBy(() -> kbService.update(KB_ID, request, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("Delete Knowledge Base")
    class DeleteTests {

        @Test
        @DisplayName("deletes KB, removes tools, sends cleanup")
        void delete_removesToolsAndSendsCleanup() {
            KnowledgeBaseEntity entity = buildKbEntity("To Delete");
            when(kbMapper.selectById(KB_ID)).thenReturn(entity);
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of());
            when(agentToolBindingMapper.selectList(any())).thenReturn(List.of());
            when(kbMapper.deleteById(KB_ID)).thenReturn(1);

            kbService.delete(KB_ID, USER_A, false);

            verify(kbMapper).deleteById(KB_ID);
            verify(toolRegistry).removeKnowledgeTools(KB_ID);
            verify(cleanupMessageSender).sendCleanup(KB_ID);
        }

        @Test
        @DisplayName("throws ASSET_DELETE_CONFLICT when referenced")
        void delete_conflict() {
            KnowledgeBaseEntity entity = buildKbEntity("Referenced KB");
            when(kbMapper.selectById(KB_ID)).thenReturn(entity);
            AssetReferenceEntity ref = new AssetReferenceEntity();
            ref.setReferrerId(UUID.randomUUID());
            ref.setRefereeType("knowledge_base");
            ref.setRefereeId(KB_ID);
            when(assetReferenceMapper.selectList(any())).thenReturn(List.of(ref));
            when(agentToolBindingMapper.selectList(any())).thenReturn(List.of());

            assertThatThrownBy(() -> kbService.delete(KB_ID, USER_A, false))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ASSET_DELETE_CONFLICT);
        }
    }

    @Nested
    @DisplayName("Document Upload")
    class DocumentTests {

        @Test
        @DisplayName("validates file size exceeds limit")
        void upload_fileTooLarge() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(51 * 1024 * 1024L);
            when(file.isEmpty()).thenReturn(false);
            when(kbMapper.selectById(KB_ID)).thenReturn(buildKbEntity("KB"));

            assertThatThrownBy(() -> kbService.uploadDocument(KB_ID, file, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        @Test
        @DisplayName("rejects disallowed file types")
        void upload_badExtension() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(1000L);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("virus.exe");
            when(kbMapper.selectById(KB_ID)).thenReturn(buildKbEntity("KB"));

            assertThatThrownBy(() -> kbService.uploadDocument(KB_ID, file, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_TYPE_REJECTED);
        }

        @Test
        @DisplayName("uploads document and triggers index")
        void upload_success() throws IOException {
            KnowledgeBaseEntity kb = buildKbEntity("KB");
            kb.setDocCount(3);
            kb.setTotalSizeBytes(5000L);
            when(kbMapper.selectById(KB_ID)).thenReturn(kb);

            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(1000L);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("doc.pdf");
            when(file.getContentType()).thenReturn("application/pdf");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
            when(fileStorageService.storeKbDocument(any(), any(), any(), any(), any()))
                    .thenReturn("kb/path/doc.pdf");
            when(kbDocumentMapper.insert(any(KbDocumentEntity.class))).thenReturn(1);
            when(fileMapper.insert(any(com.agentplatform.common.mybatis.entity.FileEntity.class))).thenReturn(1);
            when(kbMapper.updateById(any(KnowledgeBaseEntity.class))).thenReturn(1);

            Map<String, Object> result = kbService.uploadDocument(KB_ID, file, USER_A);

            assertThat(result).containsEntry("scan_status", "clean");
            assertThat(result).containsEntry("index_status", "pending");
            assertThat(result).containsKeys("document_id", "file_id");
            verify(indexMessageSender).sendIndex(any());

            // Verify counter increments
            ArgumentCaptor<KnowledgeBaseEntity> kbCaptor = ArgumentCaptor.forClass(KnowledgeBaseEntity.class);
            verify(kbMapper).updateById(kbCaptor.capture());
            assertThat(kbCaptor.getValue().getDocCount()).isEqualTo(4);   // 3 + 1
            assertThat(kbCaptor.getValue().getTotalSizeBytes()).isEqualTo(6000L); // 5000 + 1000
            verify(fileStorageService).storeKbDocument(eq(USER_A), eq(KB_ID), any(), eq("doc.pdf"), any());
        }

        @Test
        @DisplayName("lists documents for a KB")
        void listDocuments() {
            when(kbMapper.selectById(KB_ID)).thenReturn(buildKbEntity("KB"));
            when(kbDocumentMapper.selectPage(any(com.baomidou.mybatisplus.core.metadata.IPage.class), any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
                com.baomidou.mybatisplus.core.metadata.IPage<KbDocumentEntity> page = inv.getArgument(0);
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            });

            PageResult<KbDocumentVO> result = kbService.listDocuments(KB_ID, USER_A, 1, 20);
            assertThat(result.data()).isEmpty();
        }

        @Test
        @DisplayName("deletes document and updates counters")
        void deleteDocument() {
            KnowledgeBaseEntity kb = buildKbEntity("KB");
            kb.setDocCount(5);
            kb.setTotalSizeBytes(10000L);
            when(kbMapper.selectById(KB_ID)).thenReturn(kb);
            when(kbMapper.updateById(any(KnowledgeBaseEntity.class))).thenReturn(1);

            KbDocumentEntity doc = new KbDocumentEntity()
                    .setId(DOC_ID).setKnowledgeBaseId(KB_ID)
                    .setFileId(UUID.randomUUID()).setFilename("doc.pdf")
                    .setFileSize(2000L);
            when(kbDocumentMapper.selectById(DOC_ID)).thenReturn(doc);
            when(kbDocumentMapper.deleteById(DOC_ID)).thenReturn(1);

            kbService.deleteDocument(KB_ID, DOC_ID, USER_A);

            verify(kbDocumentMapper).deleteById(DOC_ID);

            ArgumentCaptor<KnowledgeBaseEntity> kbCaptor = ArgumentCaptor.forClass(KnowledgeBaseEntity.class);
            verify(kbMapper, atLeastOnce()).updateById(kbCaptor.capture());
            assertThat(kbCaptor.getValue().getDocCount()).isEqualTo(4);  // 5 - 1
            assertThat(kbCaptor.getValue().getTotalSizeBytes()).isEqualTo(8000L); // 10000 - 2000
        }

        @Test
        @DisplayName("reindex document resets status and triggers index")
        void reindexDocument() {
            KnowledgeBaseEntity kb = buildKbEntity("KB");
            when(kbMapper.selectById(KB_ID)).thenReturn(kb);

            KbDocumentEntity doc = new KbDocumentEntity()
                    .setId(DOC_ID).setKnowledgeBaseId(KB_ID)
                    .setScanStatus("clean").setIndexStatus("indexed")
                    .setChunkCount(10);
            when(kbDocumentMapper.selectById(DOC_ID)).thenReturn(doc);

            kbService.reindexDocument(KB_ID, DOC_ID, USER_A);

            assertThat(doc.getIndexStatus()).isEqualTo("pending");
            assertThat(doc.getChunkCount()).isZero();
            verify(indexMessageSender).sendIndex(DOC_ID);
        }

        @Test
        @DisplayName("reindex blocked for non-clean scan status")
        void reindex_blocked() {
            when(kbMapper.selectById(KB_ID)).thenReturn(buildKbEntity("KB"));
            KbDocumentEntity doc = new KbDocumentEntity()
                    .setId(DOC_ID).setKnowledgeBaseId(KB_ID)
                    .setScanStatus("infected").setIndexStatus("failed");
            when(kbDocumentMapper.selectById(DOC_ID)).thenReturn(doc);

            assertThatThrownBy(() -> kbService.reindexDocument(KB_ID, DOC_ID, USER_A))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.KB_DOC_REINDEX_BLOCKED);
        }
    }

    @Nested
    @DisplayName("Index Document & Search")
    class IndexAndSearchTests {

        @Test
        @DisplayName("indexDocument processes pending document")
        void indexDocument_success() throws Exception {
            KbDocumentEntity doc = new KbDocumentEntity()
                    .setId(DOC_ID).setKnowledgeBaseId(KB_ID)
                    .setFileId(UUID.randomUUID()).setIndexStatus("pending");
            when(kbDocumentMapper.selectById(DOC_ID)).thenReturn(doc);
            when(jdbcTemplate.update(anyString(), any(Object.class)))
                    .thenReturn(1);
            com.agentplatform.common.mybatis.entity.FileEntity file =
                    new com.agentplatform.common.mybatis.entity.FileEntity()
                            .setId(doc.getFileId()).setStoragePath("kb/path/doc.txt");
            when(fileMapper.selectById(doc.getFileId())).thenReturn(file);
            when(fileStorageService.read("kb/path/doc.txt"))
                    .thenReturn(new ByteArrayInputStream("hello world".getBytes()));
            when(embeddingClient.embed(anyString()))
                    .thenReturn(List.of(0.1f, 0.2f, 0.3f));

            kbService.indexDocument(DOC_ID);

            verify(embeddingClient, atLeastOnce()).embed(anyString());
        }

        @Test
        @DisplayName("indexDocument skips when already indexed")
        void indexDocument_alreadyIndexed() {
            KbDocumentEntity doc = new KbDocumentEntity()
                    .setId(DOC_ID).setKnowledgeBaseId(KB_ID).setIndexStatus("indexed");
            when(kbDocumentMapper.selectById(DOC_ID)).thenReturn(doc);

            kbService.indexDocument(DOC_ID);

            verify(jdbcTemplate, never()).update(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("indexDocument skips when status is indexing")
        void indexDocument_inProgress() {
            KbDocumentEntity doc = new KbDocumentEntity()
                    .setId(DOC_ID).setKnowledgeBaseId(KB_ID).setIndexStatus("indexing");
            when(kbDocumentMapper.selectById(DOC_ID)).thenReturn(doc);

            kbService.indexDocument(DOC_ID);

            verify(jdbcTemplate, never()).update(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("search performs pgvector query and returns typed results")
        void search_success() {
            KnowledgeBaseEntity kb = buildKbEntity("Search KB");
            when(kbMapper.selectById(KB_ID)).thenReturn(kb);
            when(embeddingClient.embed("test query"))
                    .thenReturn(List.of(0.5f, 0.3f, 0.1f));
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(Map.of(
                    "content", "relevant text",
                    "score", 0.85d,
                    "document_name", "guide.pdf",
                    "chunk_index", 0));
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(rows);

            List<KbSearchResult> results = kbService.search(KB_ID, "test query", 5, USER_A);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getContent()).isEqualTo("relevant text");
            assertThat(results.get(0).getScore()).isEqualTo(0.85d);
            assertThat(results.get(0).getDocumentName()).isEqualTo("guide.pdf");
        }

        @Test
        @DisplayName("search caps topK at 100")
        void search_capsTopK() {
            KnowledgeBaseEntity kb = buildKbEntity("KB");
            when(kbMapper.selectById(KB_ID)).thenReturn(kb);
            when(embeddingClient.embed(anyString())).thenReturn(List.of(0.1f));
            when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                    .thenReturn(List.of());

            kbService.search(KB_ID, "q", 9999, USER_A);

            verify(jdbcTemplate).queryForList(anyString(), any(Object[].class));
        }
    }

    private KnowledgeBaseEntity buildKbEntity(String name) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(KB_ID);
        entity.setOwnerId(USER_A);
        entity.setName(name);
        entity.setDescription(name + " description");
        entity.setVisibility("private");
        entity.setDocCount(0);
        entity.setTotalSizeBytes(0L);
        entity.setVersion(0L);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }
}
