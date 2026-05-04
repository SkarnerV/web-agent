package com.agentplatform.file.service;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.storage.FileStorageService;
import com.agentplatform.common.mybatis.entity.FileEntity;
import com.agentplatform.common.mybatis.mapper.FileMapper;
import com.agentplatform.file.dto.FileDownloadTokenVO;
import com.agentplatform.file.entity.FileDownloadTokenEntity;
import com.agentplatform.file.mapper.FileDownloadTokenMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "jpg", "png", "gif", "webp");
    private static final int TOKEN_BYTES = 64;
    private static final Set<String> TEXT_MIME_TYPES = Set.of(
            "text/plain", "text/markdown", "text/csv", "application/json", "application/xml");

    private final FileMapper fileMapper;
    private final FileDownloadTokenMapper tokenMapper;
    private final FileStorageService storageService;
    private final Tika tika = new Tika();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public FileService(FileMapper fileMapper, FileDownloadTokenMapper tokenMapper,
                       FileStorageService storageService) {
        this.fileMapper = fileMapper;
        this.tokenMapper = tokenMapper;
        this.storageService = storageService;
    }

    // ───────── 9.1 Upload ─────────

    @Transactional
    public Map<String, Object> upload(MultipartFile file, UUID currentUserId) {
        if (file.isEmpty()) throw new BizException(ErrorCode.INVALID_REQUEST, Map.of("reason", "File is empty"));
        if (file.getSize() > MAX_FILE_SIZE) throw new BizException(ErrorCode.FILE_SIZE_EXCEEDED);

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains("."))
            throw new BizException(ErrorCode.FILE_TYPE_REJECTED, Map.of("reason", "Unknown file type"));
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext))
            throw new BizException(ErrorCode.FILE_TYPE_REJECTED, Map.of("reason", "File type not allowed: ." + ext));

        // MIME validation with Tika
        String declaredType = file.getContentType();
        try (InputStream is = file.getInputStream()) {
            String detectedType = tika.detect(is, filename);
            if (declaredType == null) declaredType = detectedType;
            boolean consistent = isMimeConsistent(declaredType, detectedType, ext);
            if (!consistent)
                throw new BizException(ErrorCode.FILE_TYPE_REJECTED,
                        Map.of("reason", "MIME mismatch: declared=" + declaredType + " detected=" + detectedType));
        } catch (BizException e) {
            throw e;
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, Map.of("reason", "Failed to read file for validation"));
        }

        UUID fileId = UUID.randomUUID();
        String storagePath;
        try {
            storagePath = storageService.storeKbDocument(currentUserId, null, fileId, filename, file.getInputStream());
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, Map.of("reason", "Failed to store file"));
        }

        FileEntity entity = new FileEntity()
                .setId(fileId)
                .setOwnerId(currentUserId)
                .setSource("chat_upload")
                .setFilename(filename)
                .setFileSize(file.getSize())
                .setMimeType(declaredType)
                .setStoragePath(storagePath)
                .setStorageType("minio")
                .setScanStatus("clean")
                .setStatus("active")
                .setExpiresAt(OffsetDateTime.now().plusDays(30))
                .setCreatedAt(OffsetDateTime.now());
        fileMapper.insert(entity);

        log.info("File uploaded: {} ({} bytes)", fileId, file.getSize());
        return Map.of("file_id", fileId, "filename", filename, "size", file.getSize(),
                "mime_type", declaredType, "scan_status", "clean");
    }

    // ───────── 9.2 Token Generation ─────────

    @Transactional
    public FileDownloadTokenVO generateDownloadToken(UUID fileId, UUID currentUserId, UUID sessionId) {
        return generateToken(fileId, currentUserId, sessionId, "download", "d");
    }

    @Transactional
    public FileDownloadTokenVO generatePreviewToken(UUID fileId, UUID currentUserId, UUID sessionId) {
        return generateToken(fileId, currentUserId, sessionId, "preview", "p");
    }

    private FileDownloadTokenVO generateToken(UUID fileId, UUID currentUserId, UUID sessionId,
                                               String tokenType, String linkType) {
        FileEntity file = getFileOrThrow(fileId);
        // TODO: W3 OAuth — verify file.getOwnerId().equals(currentUserId) once UserPrincipal is real
        // Check expiration
        if (file.getExpiresAt() != null && file.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BizException(ErrorCode.FILE_EXPIRED);
        }

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = base64Encoder.encodeToString(tokenBytes);

        FileDownloadTokenEntity entity = new FileDownloadTokenEntity()
                .setId(UUID.randomUUID())
                .setFileId(fileId)
                .setUserId(currentUserId)
                .setSessionId(sessionId)
                .setToken(token)
                .setTokenType(tokenType)
                .setUsed(false)
                .setExpiresAt(OffsetDateTime.now().plusHours(tokenType.equals("preview") ? 24 : 1))
                .setCreatedAt(OffsetDateTime.now());
        tokenMapper.insert(entity);

        FileDownloadTokenVO vo = new FileDownloadTokenVO();
        vo.setDownloadUrl("/api/v1/files/" + linkType + "/" + token);
        vo.setExpiresAt(entity.getExpiresAt());
        return vo;
    }

    // ───────── 9.3 Download / Preview ─────────

    public record FileDownloadResult(InputStream content, String filename, String mimeType) {}

    public FileDownloadResult download(String token, String expectedType) {
        FileDownloadTokenEntity entity = getTokenOrThrow(token, expectedType);

        // Download tokens are one-time use
        if ("download".equals(expectedType)) {
            if (Boolean.TRUE.equals(entity.getUsed())) {
                throw new BizException(ErrorCode.FILE_LINK_EXPIRED, Map.of("reason", "Token already used"));
            }
            entity.setUsed(true);
            entity.setUsedAt(OffsetDateTime.now());
            tokenMapper.updateById(entity);
        }

        FileEntity file = fileMapper.selectById(entity.getFileId());
        if (file == null || "deleted".equals(file.getStatus())) {
            throw new BizException(ErrorCode.FILE_EXPIRED);
        }

        try {
            InputStream content = storageService.read(file.getStoragePath());
            return new FileDownloadResult(content, file.getFilename(), file.getMimeType());
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, Map.of("reason", "Failed to read file"));
        }
    }

    // ───────── 9.4 Scheduled Cleanup ─────────

    public int cleanupExpiredFiles() {
        List<FileEntity> expired = fileMapper.selectList(new LambdaQueryWrapper<FileEntity>()
                .eq(FileEntity::getStatus, "active")
                .lt(FileEntity::getExpiresAt, OffsetDateTime.now())
                .isNotNull(FileEntity::getExpiresAt)
                .last("LIMIT 500"));

        int cleaned = 0;
        for (FileEntity file : expired) {
            try {
                file.setStatus("expired");
                fileMapper.updateById(file);
                storageService.delete(file.getStoragePath());
                cleaned++;
            } catch (Exception e) {
                log.warn("Failed to clean up file {}: {}", file.getId(), e.getMessage());
            }
        }
        if (cleaned > 0) log.info("Cleaned up {} expired files", cleaned);
        return cleaned;
    }

    // ─── helpers ───

    private FileEntity getFileOrThrow(UUID fileId) {
        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        return file;
    }

    private FileDownloadTokenEntity getTokenOrThrow(String token, String expectedType) {
        FileDownloadTokenEntity entity = tokenMapper.selectOne(
                new LambdaQueryWrapper<FileDownloadTokenEntity>()
                        .eq(FileDownloadTokenEntity::getToken, token)
                        .eq(FileDownloadTokenEntity::getTokenType, expectedType));
        if (entity == null) throw new BizException(ErrorCode.FILE_LINK_EXPIRED);
        if (entity.getExpiresAt().isBefore(OffsetDateTime.now()))
            throw new BizException(ErrorCode.FILE_LINK_EXPIRED, Map.of("reason", "Token expired"));
        return entity;
    }

    private boolean isMimeConsistent(String declared, String detected, String ext) {
        if (declared == null || detected == null) return true;
        if (declared.equals(detected)) return true;
        // application/octet-stream means client didn't specify — trust detected
        if ("application/octet-stream".equals(declared) && !"application/octet-stream".equals(detected)) return true;
        // Text files often have overlapping MIME types
        if (TEXT_MIME_TYPES.contains(detected) && TEXT_MIME_TYPES.contains(declared)) return true;
        // Images
        if (declared.startsWith("image/") && detected.startsWith("image/")) return true;
        return false;
    }
}
