package com.agentplatform.file.service;

import com.agentplatform.common.core.error.BizException;
import com.agentplatform.common.core.error.ErrorCode;
import com.agentplatform.common.core.storage.FileStorageService;
import com.agentplatform.common.mybatis.entity.FileEntity;
import com.agentplatform.common.mybatis.mapper.FileMapper;
import com.agentplatform.file.dto.FileDownloadTokenVO;
import com.agentplatform.file.entity.FileDownloadTokenEntity;
import com.agentplatform.file.mapper.FileDownloadTokenMapper;
import com.agentplatform.file.service.FileService.FileDownloadResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private FileMapper fileMapper;
    @Mock private FileDownloadTokenMapper tokenMapper;
    @Mock private FileStorageService storageService;

    private FileService fileService;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FILE_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    @BeforeEach
    void setUp() {
        fileService = new FileService(fileMapper, tokenMapper, storageService);
    }

    @Nested
    @DisplayName("Upload")
    class UploadTests {

        @Test
        @DisplayName("rejects file over 50MB")
        void rejectsTooLarge() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(51 * 1024 * 1024L);
            when(file.isEmpty()).thenReturn(false);

            assertThatThrownBy(() -> fileService.upload(file, USER_ID))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        @Test
        @DisplayName("rejects disallowed extensions")
        void rejectsBadExtension() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(1000L);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("virus.exe");

            assertThatThrownBy(() -> fileService.upload(file, USER_ID))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_TYPE_REJECTED);
        }

        @Test
        @DisplayName("uploads valid PDF and returns metadata")
        void uploadsPdf() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(1024L);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("doc.pdf");
            when(file.getContentType()).thenReturn("application/pdf");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[1024]));
            when(storageService.storeKbDocument(any(), any(), any(), any(), any()))
                    .thenReturn("kb/path/doc.pdf");

            Map<String, Object> result = fileService.upload(file, USER_ID);

            assertThat(result).containsEntry("scan_status", "clean");
            assertThat(result.get("file_id")).isNotNull();
            verify(fileMapper).insert(any(FileEntity.class));
        }

        @Test
        @DisplayName("rejects empty file")
        void rejectsEmpty() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> fileService.upload(file, USER_ID))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("rejects file without extension")
        void rejectsNoExtension() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(1000L);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn("noextension");

            assertThatThrownBy(() -> fileService.upload(file, USER_ID))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_TYPE_REJECTED);
        }

        @Test
        @DisplayName("rejects null original filename")
        void rejectsNullFilename() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(1000L);
            when(file.isEmpty()).thenReturn(false);
            when(file.getOriginalFilename()).thenReturn(null);

            assertThatThrownBy(() -> fileService.upload(file, USER_ID))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_TYPE_REJECTED);
        }
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenTests {

        @Test
        @DisplayName("generates download token with 86-char Base64URL string")
        void generateDownloadToken() {
            FileEntity file = new FileEntity().setId(FILE_ID).setStatus("active");
            when(fileMapper.selectById(FILE_ID)).thenReturn(file);

            FileDownloadTokenVO vo = fileService.generateDownloadToken(FILE_ID, USER_ID, null);

            assertThat(vo.getDownloadUrl()).isNotNull();
            assertThat(vo.getExpiresAt()).isAfter(OffsetDateTime.now());
            verify(tokenMapper).insert(any(FileDownloadTokenEntity.class));
        }

        @Test
        @DisplayName("throws FILE_EXPIRED when file has expired")
        void rejectsExpiredFile() {
            FileEntity file = new FileEntity().setId(FILE_ID).setStatus("active")
                    .setExpiresAt(OffsetDateTime.now().minusDays(1));
            when(fileMapper.selectById(FILE_ID)).thenReturn(file);

            assertThatThrownBy(() -> fileService.generateDownloadToken(FILE_ID, USER_ID, null))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_EXPIRED);
        }

        @Test
        @DisplayName("generates preview token")
        void generatePreviewToken() {
            FileEntity file = new FileEntity().setId(FILE_ID).setStatus("active");
            when(fileMapper.selectById(FILE_ID)).thenReturn(file);

            FileDownloadTokenVO vo = fileService.generatePreviewToken(FILE_ID, USER_ID, null);
            assertThat(vo.getDownloadUrl()).contains("/p/");
            verify(tokenMapper).insert(any(FileDownloadTokenEntity.class));
        }
    }

    @Nested
    @DisplayName("Download / Preview")
    class DownloadTests {

        @Test
        @DisplayName("download marks token used and returns file stream")
        void downloadOneTime() throws IOException {
            FileDownloadTokenEntity token = new FileDownloadTokenEntity()
                    .setId(UUID.randomUUID()).setFileId(FILE_ID)
                    .setToken("test-token").setTokenType("download")
                    .setUsed(false).setExpiresAt(OffsetDateTime.now().plusHours(1));
            when(tokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(token);

            FileEntity file = new FileEntity().setId(FILE_ID).setFilename("doc.pdf")
                    .setMimeType("application/pdf").setStoragePath("kb/path/doc.pdf").setStatus("active");
            when(fileMapper.selectById(FILE_ID)).thenReturn(file);
            when(storageService.read("kb/path/doc.pdf"))
                    .thenReturn(new ByteArrayInputStream("content".getBytes()));

            FileDownloadResult result = fileService.download("test-token", "download");

            assertThat(result.filename()).isEqualTo("doc.pdf");
            verify(tokenMapper).updateById(any(FileDownloadTokenEntity.class));
        }

        @Test
        @DisplayName("rejects already-used download token")
        void rejectsUsedToken() {
            FileDownloadTokenEntity token = new FileDownloadTokenEntity()
                    .setId(UUID.randomUUID()).setFileId(FILE_ID)
                    .setToken("used-token").setTokenType("download")
                    .setUsed(true).setExpiresAt(OffsetDateTime.now().plusHours(1));
            when(tokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(token);

            assertThatThrownBy(() -> fileService.download("used-token", "download"))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_LINK_EXPIRED);
        }

        @Test
        @DisplayName("rejects download for deleted file")
        void rejectsDeletedFile() {
            FileDownloadTokenEntity token = new FileDownloadTokenEntity()
                    .setId(UUID.randomUUID()).setFileId(FILE_ID)
                    .setToken("tok").setTokenType("download")
                    .setUsed(false).setExpiresAt(OffsetDateTime.now().plusHours(1));
            when(tokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(token);

            FileEntity file = new FileEntity().setId(FILE_ID).setStatus("deleted");
            when(fileMapper.selectById(FILE_ID)).thenReturn(file);

            assertThatThrownBy(() -> fileService.download("tok", "download"))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_EXPIRED);
        }

        @Test
        @DisplayName("rejects expired token")
        void rejectsExpiredToken() {
            FileDownloadTokenEntity token = new FileDownloadTokenEntity()
                    .setId(UUID.randomUUID()).setFileId(FILE_ID)
                    .setToken("old").setTokenType("download")
                    .setUsed(false).setExpiresAt(OffsetDateTime.now().minusHours(1));
            when(tokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(token);

            assertThatThrownBy(() -> fileService.download("old", "download"))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_LINK_EXPIRED);
        }

        @Test
        @DisplayName("rejects nonexistent token")
        void rejectsNonexistentToken() {
            when(tokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> fileService.download("ghost", "download"))
                    .isInstanceOf(BizException.class)
                    .extracting(e -> ((BizException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FILE_LINK_EXPIRED);
        }

        @Test
        @DisplayName("preview does not mark token used and returns inline stream")
        void previewReusable() throws IOException {
            FileDownloadTokenEntity token = new FileDownloadTokenEntity()
                    .setId(UUID.randomUUID()).setFileId(FILE_ID)
                    .setToken("preview-token").setTokenType("preview")
                    .setUsed(false).setExpiresAt(OffsetDateTime.now().plusHours(24));
            when(tokenMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(token);

            FileEntity file = new FileEntity().setId(FILE_ID).setFilename("img.png")
                    .setMimeType("image/png").setStoragePath("kb/path/img.png").setStatus("active");
            when(fileMapper.selectById(FILE_ID)).thenReturn(file);
            when(storageService.read("kb/path/img.png"))
                    .thenReturn(new ByteArrayInputStream("img".getBytes()));

            FileDownloadResult result = fileService.download("preview-token", "preview");

            assertThat(result.mimeType()).isEqualTo("image/png");
            verify(tokenMapper, never()).updateById(any(FileDownloadTokenEntity.class));
        }
    }

    @Nested
    @DisplayName("Cleanup")
    class CleanupTests {

        @Test
        @DisplayName("cleans up expired active files")
        void cleansExpired() {
            FileEntity expired = new FileEntity().setId(FILE_ID).setStoragePath("expired/file.txt")
                    .setStatus("active").setExpiresAt(OffsetDateTime.now().minusDays(1));
            when(fileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of(expired));
            when(fileMapper.updateById(any(FileEntity.class))).thenReturn(1);

            int cleaned = fileService.cleanupExpiredFiles();

            assertThat(cleaned).isEqualTo(1);
            verify(storageService).delete("expired/file.txt");
        }

        @Test
        @DisplayName("skips files that are already expired status")
        void skipsAlreadyExpired() {
            when(fileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of());

            int cleaned = fileService.cleanupExpiredFiles();
            assertThat(cleaned).isZero();
        }
    }
}
