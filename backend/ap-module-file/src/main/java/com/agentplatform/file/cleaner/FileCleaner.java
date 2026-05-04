package com.agentplatform.file.cleaner;

import com.agentplatform.file.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FileCleaner {

    private static final Logger log = LoggerFactory.getLogger(FileCleaner.class);

    private final FileService fileService;

    public FileCleaner(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Hourly cleanup: scan files with expires_at < now() AND status='active',
     * mark status='expired', and delete from object storage.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanExpiredFiles() {
        log.debug("Starting scheduled file cleanup");
        int cleaned = fileService.cleanupExpiredFiles();
        if (cleaned > 0) {
            log.info("Scheduled cleanup completed: {} files expired", cleaned);
        }
    }
}
