package com.agentplatform.common.core.knowledge;

import java.util.UUID;

/**
 * Cross-module interface for sending knowledge base indexing messages.
 * MVP stub logs; full implementation sends RabbitMQ message to kb.index queue.
 */
public interface KbIndexMessageSender {

    /**
     * Trigger asynchronous indexing for a document.
     */
    void sendIndex(UUID documentId);
}
