package com.agentplatform.common.core.knowledge;

import java.util.UUID;

/**
 * Cross-module interface for sending async knowledge base cleanup messages.
 * MVP stub logs the event; full implementation sends RabbitMQ message to kb.cleanup queue.
 */
public interface KbCleanupMessageSender {

    /**
     * Trigger asynchronous cleanup of all kb_documents and kb_chunks for the given KB.
     */
    void sendCleanup(UUID knowledgeBaseId);
}
