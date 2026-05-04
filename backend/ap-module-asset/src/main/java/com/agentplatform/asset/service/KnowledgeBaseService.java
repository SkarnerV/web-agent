package com.agentplatform.asset.service;

import com.agentplatform.asset.converter.KnowledgeBaseConverter;
import com.agentplatform.asset.dto.*;
import com.agentplatform.asset.entity.KbDocumentEntity;
import com.agentplatform.asset.entity.KnowledgeBaseEntity;
import com.agentplatform.asset.mapper.KbDocumentMapper;
import com.agentplatform.asset.mapper.KnowledgeBaseMapper;
import com.agentplatform.common.core.embedding.EmbeddingClient;
import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.tool.ToolRegistry;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.knowledge.KbCleanupMessageSender;
import com.agentplatform.common.core.knowledge.KbIndexMessageSender;
import com.agentplatform.common.core.response.PageResult;
import com.agentplatform.common.core.security.*;
import com.agentplatform.common.core.storage.FileStorageService;
import com.agentplatform.common.mybatis.entity.AgentToolBindingEntity;
import com.agentplatform.common.mybatis.entity.AssetReferenceEntity;
import com.agentplatform.common.mybatis.entity.FileEntity;
import com.agentplatform.common.mybatis.mapper.AgentToolBindingMapper;
import com.agentplatform.common.mybatis.mapper.AssetReferenceMapper;
import com.agentplatform.common.mybatis.mapper.FileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md");

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseConverter knowledgeBaseConverter;
    private final AssetReferenceMapper assetReferenceMapper;
    private final AgentToolBindingMapper agentToolBindingMapper;
    private final PermissionChecker permissionChecker;
    private final KbCleanupMessageSender cleanupMessageSender;
    private final FileMapper fileMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final FileStorageService fileStorageService;
    private final KbIndexMessageSender indexMessageSender;
    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;
    private final ToolRegistry toolRegistry;

    public KnowledgeBaseService(KnowledgeBaseMapper knowledgeBaseMapper,
                                KnowledgeBaseConverter knowledgeBaseConverter,
                                AssetReferenceMapper assetReferenceMapper,
                                AgentToolBindingMapper agentToolBindingMapper,
                                PermissionChecker permissionChecker,
                                KbCleanupMessageSender cleanupMessageSender,
                                FileMapper fileMapper,
                                KbDocumentMapper kbDocumentMapper,
                                FileStorageService fileStorageService,
                                KbIndexMessageSender indexMessageSender,
                                EmbeddingClient embeddingClient,
                                JdbcTemplate jdbcTemplate,
                                ToolRegistry toolRegistry) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeBaseConverter = knowledgeBaseConverter;
        this.assetReferenceMapper = assetReferenceMapper;
        this.agentToolBindingMapper = agentToolBindingMapper;
        this.permissionChecker = permissionChecker;
        this.cleanupMessageSender = cleanupMessageSender;
        this.fileMapper = fileMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.fileStorageService = fileStorageService;
        this.indexMessageSender = indexMessageSender;
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate = jdbcTemplate;
        this.toolRegistry = toolRegistry;
    }

    // ───────── Knowledge Base CRUD ─────────

    @Transactional
    public KnowledgeBaseDetailVO create(KnowledgeBaseCreateRequest request, UUID currentUserId) {
        KnowledgeBaseEntity entity = knowledgeBaseConverter.toEntity(request);
        entity.setOwnerId(currentUserId);
        entity.setVisibility(AssetVisibility.PRIVATE.name().toLowerCase());
        entity.setDocCount(0);
        entity.setTotalSizeBytes(0L);
        knowledgeBaseMapper.insert(entity);
        toolRegistry.registerKnowledgeTool(entity.getId(), entity.getName());
        return getDetail(entity.getId(), currentUserId);
    }

    public PageResult<KnowledgeBaseSummaryVO> list(UUID currentUserId, String search,
                                                    int page, int pageSize, String sortBy, String sortOrder) {
        LambdaQueryWrapper<KnowledgeBaseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseEntity::getOwnerId, currentUserId);
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(KnowledgeBaseEntity::getName, search)
                    .or().like(KnowledgeBaseEntity::getDescription, search));
        }
        String column = "created_at".equals(sortBy) ? "created_at" : "updated_at";
        if ("asc".equalsIgnoreCase(sortOrder)) {
            wrapper.orderByAsc(column.equals("created_at") ? KnowledgeBaseEntity::getCreatedAt : KnowledgeBaseEntity::getUpdatedAt);
        } else {
            wrapper.orderByDesc(column.equals("created_at") ? KnowledgeBaseEntity::getCreatedAt : KnowledgeBaseEntity::getUpdatedAt);
        }
        IPage<KnowledgeBaseEntity> pageResult = knowledgeBaseMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<KnowledgeBaseSummaryVO> voList = pageResult.getRecords().stream()
                .map(knowledgeBaseConverter::toSummaryVO)
                .toList();
        return new PageResult<>(voList, pageResult.getTotal(), page, pageSize);
    }

    public KnowledgeBaseDetailVO getDetail(UUID kbId, UUID currentUserId) {
        KnowledgeBaseEntity entity = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.READ);
        return knowledgeBaseConverter.toDetailVO(entity);
    }

    @Transactional
    public KnowledgeBaseDetailVO update(UUID kbId, KnowledgeBaseUpdateRequest request, UUID currentUserId) {
        KnowledgeBaseEntity entity = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.WRITE);
        if (request.getVersion() == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, Map.of("reason", "version is required for update"));
        }
        if (!request.getVersion().equals(entity.getVersion())) {
            throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getIndexConfig() != null) entity.setIndexConfig(request.getIndexConfig());
        int rows = knowledgeBaseMapper.updateById(entity);
        if (rows == 0) throw new BizException(ErrorCode.ASSET_OPTIMISTIC_LOCK);
        return getDetail(kbId, currentUserId);
    }

    @Transactional
    public void delete(UUID kbId, UUID currentUserId, boolean force) {
        KnowledgeBaseEntity entity = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.DELETE);

        List<AssetReferenceEntity> refs = assetReferenceMapper.selectList(
                new LambdaQueryWrapper<AssetReferenceEntity>()
                        .eq(AssetReferenceEntity::getRefereeType, "knowledge_base")
                        .eq(AssetReferenceEntity::getRefereeId, kbId));
        List<AgentToolBindingEntity> bindings = agentToolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBindingEntity>()
                        .eq(AgentToolBindingEntity::getSourceType, "knowledge")
                        .eq(AgentToolBindingEntity::getSourceId, kbId));

        if ((!refs.isEmpty() || !bindings.isEmpty()) && !force) {
            List<UUID> referrerIds = new ArrayList<>();
            refs.stream().map(AssetReferenceEntity::getReferrerId).distinct().forEach(referrerIds::add);
            bindings.stream().map(AgentToolBindingEntity::getAgentId).distinct().forEach(referrerIds::add);
            throw new BizException(ErrorCode.ASSET_DELETE_CONFLICT,
                    Map.of("referrer_ids", referrerIds.stream().distinct().toList(), "count", referrerIds.size()));
        }
        if (!refs.isEmpty()) {
            assetReferenceMapper.delete(new LambdaQueryWrapper<AssetReferenceEntity>()
                    .eq(AssetReferenceEntity::getRefereeType, "knowledge_base").eq(AssetReferenceEntity::getRefereeId, kbId));
        }
        if (!bindings.isEmpty()) {
            agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBindingEntity>()
                    .eq(AgentToolBindingEntity::getSourceType, "knowledge").eq(AgentToolBindingEntity::getSourceId, kbId));
        }
        knowledgeBaseMapper.deleteById(kbId);
        toolRegistry.removeKnowledgeTools(kbId);
        cleanupMessageSender.sendCleanup(kbId);
        List<UUID> allBindings = new ArrayList<>();
        refs.stream().map(AssetReferenceEntity::getReferrerId).distinct().forEach(allBindings::add);
        bindings.stream().map(AgentToolBindingEntity::getAgentId).distinct().forEach(allBindings::add);
        log.info("Deleted knowledge base {} (force={}, unbindRefs={})", kbId, force,
                allBindings.stream().distinct().toList());
    }

    public Map<String, Object> export(UUID kbId, UUID currentUserId) {
        KnowledgeBaseEntity entity = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(entity), Permission.READ);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", entity.getName());
        data.put("description", entity.getDescription());
        data.put("index_config", entity.getIndexConfig());
        data.put("visibility", entity.getVisibility());
        data.put("doc_count", entity.getDocCount());
        data.put("total_size_bytes", entity.getTotalSizeBytes());
        return data;
    }

    // ───────── Document Upload (8.2) ─────────

    @Transactional
    public Map<String, Object> uploadDocument(UUID kbId, MultipartFile file, UUID currentUserId) {
        KnowledgeBaseEntity kb = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(kb), Permission.WRITE);

        validateFile(file);

        UUID docId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        // Store file
        String storagePath;
        try {
            storagePath = fileStorageService.storeKbDocument(currentUserId, kbId, docId,
                    file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, Map.of("reason", "Failed to read uploaded file"));
        }

        // Insert files record
        FileEntity fileEntity = new FileEntity()
                .setId(fileId)
                .setOwnerId(currentUserId)
                .setSource("knowledge")
                .setFilename(file.getOriginalFilename())
                .setFileSize(file.getSize())
                .setMimeType(file.getContentType())
                .setStoragePath(storagePath)
                .setStorageType("minio")
                .setScanStatus("clean")
                .setStatus("active")
                .setCreatedAt(OffsetDateTime.now());
        fileMapper.insert(fileEntity);

        // Insert kb_documents record
        KbDocumentEntity doc = new KbDocumentEntity()
                .setId(docId)
                .setKnowledgeBaseId(kbId)
                .setFileId(fileId)
                .setFilename(file.getOriginalFilename())
                .setFileSize(file.getSize())
                .setMimeType(file.getContentType())
                .setScanStatus("clean")
                .setIndexStatus("pending")
                .setChunkCount(0)
                .setCreatedAt(OffsetDateTime.now())
                .setUpdatedAt(OffsetDateTime.now());
        kbDocumentMapper.insert(doc);

        // Update KB counters
        kb.setDocCount((kb.getDocCount() != null ? kb.getDocCount() : 0) + 1);
        kb.setTotalSizeBytes((kb.getTotalSizeBytes() != null ? kb.getTotalSizeBytes() : 0) + file.getSize());
        knowledgeBaseMapper.updateById(kb);

        // Trigger async indexing
        indexMessageSender.sendIndex(docId);

        log.info("Uploaded document {} to KB {}", docId, kbId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("document_id", docId);
        result.put("file_id", fileId);
        result.put("scan_status", "clean");
        result.put("index_status", "pending");
        return result;
    }

    public PageResult<KbDocumentVO> listDocuments(UUID kbId, UUID currentUserId, int page, int pageSize) {
        KnowledgeBaseEntity kb = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(kb), Permission.READ);

        IPage<KbDocumentEntity> pageResult = kbDocumentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<KbDocumentEntity>()
                        .eq(KbDocumentEntity::getKnowledgeBaseId, kbId)
                        .orderByDesc(KbDocumentEntity::getCreatedAt));
        List<KbDocumentVO> voList = pageResult.getRecords().stream()
                .map(knowledgeBaseConverter::toDocumentVO)
                .toList();
        return new PageResult<>(voList, pageResult.getTotal(), page, pageSize);
    }

    // ───────── Document Delete (8.5) ─────────

    @Transactional
    public void deleteDocument(UUID kbId, UUID docId, UUID currentUserId) {
        KnowledgeBaseEntity kb = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(kb), Permission.WRITE);

        KbDocumentEntity doc = kbDocumentMapper.selectById(docId);
        if (doc == null || !doc.getKnowledgeBaseId().equals(kbId)) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }

        // Delete chunks
        kbDocumentMapper.deleteById(docId); // CASCADE deletes kb_chunks

        // Update file status
        FileEntity file = fileMapper.selectById(doc.getFileId());
        if (file != null) {
            file.setStatus("deleted");
            fileMapper.updateById(file);
            fileStorageService.delete(file.getStoragePath());
        }

        // Update KB counters
        kb.setDocCount(Math.max(0, (kb.getDocCount() != null ? kb.getDocCount() : 1) - 1));
        kb.setTotalSizeBytes(Math.max(0, (kb.getTotalSizeBytes() != null ? kb.getTotalSizeBytes() : 0) - (doc.getFileSize() != null ? doc.getFileSize() : 0)));
        knowledgeBaseMapper.updateById(kb);

        log.info("Deleted document {} from KB {}", docId, kbId);
    }

    // ───────── Document Reindex (8.3) ─────────

    @Transactional
    public void reindexDocument(UUID kbId, UUID docId, UUID currentUserId) {
        KnowledgeBaseEntity kb = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(kb), Permission.WRITE);

        KbDocumentEntity doc = kbDocumentMapper.selectById(docId);
        if (doc == null || !doc.getKnowledgeBaseId().equals(kbId)) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        if (!"clean".equals(doc.getScanStatus())) {
            throw new BizException(ErrorCode.KB_DOC_REINDEX_BLOCKED,
                    Map.of("scan_status", doc.getScanStatus()));
        }

        // Reset and re-trigger indexing
        doc.setIndexStatus("pending");
        doc.setIndexError(null);
        doc.setChunkCount(0);
        doc.setUpdatedAt(OffsetDateTime.now());
        kbDocumentMapper.updateById(doc);

        // Clear old chunks
        jdbcTemplate.update("DELETE FROM kb_chunks WHERE document_id = ?::uuid", docId);

        indexMessageSender.sendIndex(docId);
        log.info("Reindex triggered for document {}", docId);
    }

    // ───────── Semantic Search (8.4) ─────────

    public List<KbSearchResult> search(UUID kbId, String query, int topK, UUID currentUserId) {
        KnowledgeBaseEntity kb = getEntityOrThrow(kbId);
        permissionChecker.checkAccess(currentUserId, toAssetRef(kb), Permission.READ);

        List<Float> queryEmbedding = embeddingClient.embed(query);
        String vectorStr = formatPgVector(queryEmbedding);

        String sql = """
                SELECT c.content, c.chunk_index,
                       d.filename AS document_name,
                       1 - (c.embedding <=> ?::vector) AS score
                FROM kb_chunks c
                JOIN kb_documents d ON c.document_id = d.id
                WHERE c.knowledge_base_id = ?::uuid
                  AND c.embedding IS NOT NULL
                ORDER BY c.embedding <=> ?::vector
                LIMIT ?
                """;

        int cappedTopK = Math.min(topK, 100);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorStr, kbId, vectorStr, cappedTopK);

        return rows.stream().map(row -> {
            KbSearchResult r = new KbSearchResult();
            r.setContent((String) row.get("content"));
            r.setScore(((Number) row.get("score")).doubleValue());
            r.setDocumentName((String) row.get("document_name"));
            r.setChunkIndex(((Number) row.get("chunk_index")).intValue());
            return r;
        }).toList();
    }

    // ───────── Asynchronous Index (8.3) ─────────

    @Transactional
    public void indexDocument(UUID docId) {
        KbDocumentEntity doc = kbDocumentMapper.selectById(docId);
        if (doc == null) {
            log.warn("Index requested for nonexistent document {}", docId);
            return;
        }
        if (!"pending".equals(doc.getIndexStatus()) && !"failed".equals(doc.getIndexStatus())) {
            return;
        }

        int retryCount = "failed".equals(doc.getIndexStatus()) ? 3 : 0;
        runIndexing(doc, retryCount);
    }

    private void runIndexing(KbDocumentEntity doc, int previousRetries) {
        UUID docId = doc.getId();
        // Atomic status transition — only one consumer wins
        int locked = jdbcTemplate.update(
                "UPDATE kb_documents SET index_status='indexing', updated_at=now() " +
                "WHERE id=?::uuid AND index_status IN ('pending','failed')", docId);
        if (locked == 0) {
            log.info("Indexing already started for document {}, skipping", docId);
            return;
        }
        doc.setIndexStatus("indexing");

        // Clean orphaned chunks from any previous failed attempt before inserting new ones
        jdbcTemplate.update("DELETE FROM kb_chunks WHERE document_id = ?::uuid", docId);

        try {
            FileEntity file = fileMapper.selectById(doc.getFileId());
            if (file == null) {
                throw new RuntimeException("File not found: " + doc.getFileId());
            }

            String content = extractText(file);
            if (content == null || content.isBlank()) {
                throw new RuntimeException("No extractable text content");
            }

            // MVP: character-based chunking (512 chars / 128 overlap). TODO: token-aware chunking per spec.
            List<String> chunks = chunkText(content, 512, 128);

            int indexed = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                List<Float> embedding = embeddingClient.embed(chunk);
                String vectorStr = formatPgVector(embedding);

                UUID chunkId = UUID.randomUUID();
                jdbcTemplate.update(
                        "INSERT INTO kb_chunks (id, document_id, knowledge_base_id, chunk_index, content, embedding, created_at) " +
                        "VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?::vector, now())",
                        chunkId, docId, doc.getKnowledgeBaseId(), i, chunk, vectorStr);
                indexed++;
            }

            jdbcTemplate.update(
                    "UPDATE kb_documents SET index_status='indexed', chunk_count=?, index_error=NULL, updated_at=now() " +
                    "WHERE id=?::uuid", indexed, docId);
            log.info("Indexed document {}: {} chunks", docId, indexed);
        } catch (Exception e) {
            log.error("Indexing failed for document {} (attempt {}): {}", docId, previousRetries + 1, e.getMessage());
            if (previousRetries < 2) {
                jdbcTemplate.update(
                        "UPDATE kb_documents SET index_status='failed', updated_at=now() " +
                        "WHERE id=?::uuid", docId);
                // Retry
                KbDocumentEntity refreshed = kbDocumentMapper.selectById(docId);
                if (refreshed != null) {
                    runIndexing(refreshed, previousRetries + 1);
                }
            } else {
                jdbcTemplate.update(
                        "UPDATE kb_documents SET index_status='failed', index_error=?, updated_at=now() " +
                        "WHERE id=?::uuid", e.getMessage(), docId);
            }
        }
    }

    // ─── helpers ───

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, Map.of("reason", "File is empty"));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new BizException(ErrorCode.FILE_TYPE_REJECTED, Map.of("reason", "Unknown file type"));
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(ErrorCode.FILE_TYPE_REJECTED,
                    Map.of("reason", "File type not allowed: ." + ext));
        }
    }

    private String extractText(FileEntity file) {
        try (InputStream is = fileStorageService.read(file.getStoragePath())) {
            Tika tika = new Tika();
            return tika.parseToString(is);
        } catch (IOException | TikaException e) {
            throw new RuntimeException("Tika extraction failed: " + e.getMessage(), e);
        }
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
            if (start >= text.length()) break;
        }
        return chunks;
    }

    private String formatPgVector(List<Float> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private KnowledgeBaseEntity getEntityOrThrow(UUID kbId) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(kbId);
        if (entity == null) throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        return entity;
    }

    private AssetRef toAssetRef(KnowledgeBaseEntity entity) {
        AssetVisibility visibility;
        try {
            visibility = AssetVisibility.valueOf(entity.getVisibility().toUpperCase());
        } catch (IllegalArgumentException e) {
            visibility = AssetVisibility.PRIVATE;
        }
        return new AssetRef(entity.getId(), entity.getOwnerId(), visibility, entity.getDeletedAt());
    }
}
