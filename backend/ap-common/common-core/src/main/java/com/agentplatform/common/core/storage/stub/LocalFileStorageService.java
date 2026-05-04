package com.agentplatform.common.core.storage.stub;

import com.agentplatform.common.core.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@Profile("!production")
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);
    private static final Path BASE_DIR = Path.of(System.getProperty("java.io.tmpdir", "/tmp"), "agent-platform-storage");

    @Override
    public String storeKbDocument(UUID userId, UUID kbId, UUID docId, String filename, InputStream content) {
        String relativePath = "kb/" + userId + "/" + kbId + "/" + docId + "/" + filename;
        try {
            Path target = BASE_DIR.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored KB document: {}", target);
            return relativePath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file: " + relativePath, e);
        }
    }

    @Override
    public InputStream read(String storagePath) throws IOException {
        Path target = BASE_DIR.resolve(storagePath);
        return Files.newInputStream(target);
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path target = BASE_DIR.resolve(storagePath);
            Files.deleteIfExists(target);
            log.info("Deleted file: {}", target);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storagePath, e);
        }
    }
}
