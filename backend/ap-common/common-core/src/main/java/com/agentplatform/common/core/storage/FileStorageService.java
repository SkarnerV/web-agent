package com.agentplatform.common.core.storage;

import java.io.InputStream;
import java.util.UUID;

/**
 * Cross-module interface for file storage operations.
 * MVP uses local filesystem; production uses MinIO.
 */
public interface FileStorageService {

    /**
     * Store a file and return the storage path.
     *
     * @param userId   owner user ID
     * @param kbId     knowledge base ID
     * @param docId    document ID
     * @param filename original filename
     * @param content  file content stream
     * @return storage path in the format kb/{user_id}/{kb_id}/{doc_id}/{filename}
     */
    String storeKbDocument(UUID userId, UUID kbId, UUID docId, String filename, InputStream content);

    /**
     * Read a file's content as an InputStream. Caller must close the stream.
     */
    InputStream read(String storagePath) throws java.io.IOException;

    /**
     * Delete a file from storage.
     */
    void delete(String storagePath);
}
