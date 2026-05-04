package com.agentplatform.file.controller;

import com.agentplatform.common.core.response.ApiResponse;
import com.agentplatform.common.core.security.CurrentUser;
import com.agentplatform.common.core.security.UserPrincipal;
import com.agentplatform.common.core.trace.RequestIdContext;
import com.agentplatform.file.dto.FileDownloadTokenVO;
import com.agentplatform.file.service.FileService;
import com.agentplatform.file.service.FileService.FileDownloadResult;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                    @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(fileService.upload(file, user.id()), RequestIdContext.current());
    }

    @PostMapping("/{id}/download-token")
    public ApiResponse<FileDownloadTokenVO> downloadToken(@PathVariable UUID id,
                                                           @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(fileService.generateDownloadToken(id, user.id(), null), RequestIdContext.current());
    }

    @PostMapping("/{id}/preview-token")
    public ApiResponse<FileDownloadTokenVO> previewToken(@PathVariable UUID id,
                                                          @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(fileService.generatePreviewToken(id, user.id(), null), RequestIdContext.current());
    }

    @GetMapping("/d/{token}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String token) {
        FileDownloadResult result = fileService.download(token, "download");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(result.filename()).build().toString())
                .body(new InputStreamResource(result.content()));
    }

    @GetMapping("/p/{token}")
    public ResponseEntity<InputStreamResource> preview(@PathVariable String token) {
        FileDownloadResult result = fileService.download(token, "preview");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(result.filename()).build().toString())
                .body(new InputStreamResource(result.content()));
    }
}
